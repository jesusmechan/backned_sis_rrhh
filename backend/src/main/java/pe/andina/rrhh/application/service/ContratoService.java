package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;
import pe.andina.rrhh.application.port.out.ParametroPort;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.ContratoUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.HorarioLaboral;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.model.enums.ModalidadContrato;
import pe.andina.rrhh.domain.model.enums.TipoContrato;
import pe.andina.rrhh.application.dto.AppDtos.ContratoRequest;
import pe.andina.rrhh.application.dto.AppDtos.ContratoResponse;
import pe.andina.rrhh.application.dto.AppDtos.VacacionSaldoResponse;
import pe.andina.rrhh.application.port.out.ContratoPort;
import pe.andina.rrhh.application.port.out.HorarioLaboralPort;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class ContratoService implements ContratoUseCase {

    private static final String PARAM_TASA = "dias_vacaciones_mensuales";
    private static final BigDecimal TASA_DEFAULT = new BigDecimal("1.5");
    private static final Set<EstadoSolicitud> ESTADOS_QUE_CONSUMEN = EnumSet.of(
            EstadoSolicitud.PENDIENTE, EstadoSolicitud.APROBADO);

    private final ContratoPort contratoRepository;
    private final HorarioLaboralPort horarioRepository;
    private final SolicitudPermisoPort permisoRepository;
    private final ParametroPort parametros;
    private final EmpleadoUseCase empleadoService;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public ContratoService(ContratoPort contratoRepository,
                           HorarioLaboralPort horarioRepository,
                           SolicitudPermisoPort permisoRepository,
                           ParametroPort parametros,
                           EmpleadoUseCase empleadoService,
                           AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.contratoRepository = contratoRepository;
        this.horarioRepository = horarioRepository;
        this.permisoRepository = permisoRepository;
        this.parametros = parametros;
        this.empleadoService = empleadoService;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<ContratoResponse> listar() {
        return contratoRepository.findAllByOrderByIdContratoDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ContratoResponse obtener(Integer id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public ContratoResponse crear(ContratoRequest request) {
        validar(request, true);
        Contrato c = new Contrato();
        c.setCodigo(siguienteCodigo());
        aplicar(c, request);
        if (c.getEstado() == EstadoContrato.VIGENTE) {
            cerrarVigenteAnterior(c.getEmpleado().getIdEmpleado(), null, c.getFechaInicio());
            sincronizarFicha(c);
        }
        contratoRepository.save(c);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "CONTRATO", c.getIdContrato(), c.getCodigo());
        return toResponse(c);
    }

    @Transactional
    public ContratoResponse actualizar(Integer id, ContratoRequest request) {
        validar(request, false);
        Contrato c = buscar(id);
        aplicar(c, request);
        if (c.getEstado() == EstadoContrato.VIGENTE) {
            cerrarVigenteAnterior(c.getEmpleado().getIdEmpleado(), c.getIdContrato(), c.getFechaInicio());
            sincronizarFicha(c);
        }
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "CONTRATO", id, c.getCodigo());
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public VacacionSaldoResponse saldoVacaciones(Integer idEmpleado) {
        Integer destino = idEmpleado;
        if (!currentUser.isAdminOrRrhh()) {
            Integer propio = currentUser.idEmpleado();
            if (propio == null) {
                throw DomainException.forbidden("El usuario no está asociado a un trabajador");
            }
            if (destino != null && !destino.equals(propio)) {
                throw DomainException.forbidden("Solo puede consultar su saldo de vacaciones");
            }
            destino = propio;
        }
        if (destino == null) {
            throw DomainException.badRequest("Indique el trabajador");
        }
        return calcularSaldo(empleadoService.buscar(destino));
    }

    public void validarVacaciones(Empleado empleado, LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw DomainException.badRequest("El rango de vacaciones no es válido");
        }
        VacacionSaldoResponse saldo = calcularSaldo(empleado);
        BigDecimal pedidos = BigDecimal.valueOf(ChronoUnit.DAYS.between(desde, hasta) + 1);
        if (saldo.diasDisponibles().compareTo(BigDecimal.ZERO) <= 0) {
            throw DomainException.badRequest(
                    "Aún no ha ganado días de vacaciones. Se acumulan " + saldo.tasaMensual()
                            + " días por cada mes completo de vínculo (colaborador o practicante).");
        }
        if (pedidos.compareTo(saldo.diasDisponibles()) > 0) {
            throw DomainException.badRequest(
                    "No tiene días de vacaciones suficientes. Disponibles: "
                            + saldo.diasDisponibles() + ". Solicitados: " + pedidos + ".");
        }
    }

    private void validar(ContratoRequest r, boolean crear) {
        if (crear && r.idEmpleado() == null) {
            throw DomainException.badRequest("Seleccione el trabajador");
        }
        if (r.modalidad() == ModalidadContrato.PRACTICANTE && r.fechaFin() == null) {
            throw DomainException.badRequest("El contrato de practicante debe tener fecha de fin");
        }
        if (r.fechaFin() != null && r.fechaInicio() != null && r.fechaFin().isBefore(r.fechaInicio())) {
            throw DomainException.badRequest("La fecha de fin no puede ser anterior al inicio");
        }
    }

    private void aplicar(Contrato c, ContratoRequest r) {
        if (r.idEmpleado() != null) {
            c.setEmpleado(empleadoService.buscar(r.idEmpleado()));
        }
        HorarioLaboral horario = horarioRepository.findById(r.idHorario())
                .orElseThrow(() -> DomainException.badRequest("Horario no existe"));
        c.setModalidad(r.modalidad());
        c.setHorario(horario);
        c.setFechaInicio(r.fechaInicio());
        c.setFechaFin(r.fechaFin());
        c.setRemuneracionBasica(r.remuneracionBasica() != null ? r.remuneracionBasica() : BigDecimal.ZERO);
        c.setEstado(r.estado() != null ? r.estado() : EstadoContrato.VIGENTE);
        c.setObservaciones(r.observaciones());
    }

    private void cerrarVigenteAnterior(Integer idEmpleado, Integer exceptId, LocalDate inicioNuevo) {
        contratoRepository.findFirstByEmpleado_IdEmpleadoAndEstado(idEmpleado, EstadoContrato.VIGENTE)
                .ifPresent(previo -> {
                    if (exceptId != null && exceptId.equals(previo.getIdContrato())) {
                        return;
                    }
                    previo.setEstado(EstadoContrato.FINALIZADO);
                    if (previo.getFechaFin() == null) {
                        LocalDate cierre = inicioNuevo.minusDays(1);
                        if (cierre.isBefore(previo.getFechaInicio())) {
                            cierre = previo.getFechaInicio();
                        }
                        previo.setFechaFin(cierre);
                    }
                });
    }

    private void sincronizarFicha(Contrato c) {
        Empleado e = c.getEmpleado();
        e.setHorario(c.getHorario());
        if (c.getModalidad() == ModalidadContrato.PRACTICANTE) {
            e.setTipoContrato(TipoContrato.PRACTICAS);
        } else if (e.getTipoContrato() == TipoContrato.PRACTICAS) {
            e.setTipoContrato(TipoContrato.PLANILLA);
        }
    }

    private VacacionSaldoResponse calcularSaldo(Empleado empleado) {
        Contrato vigente = contratoRepository
                .findFirstByEmpleado_IdEmpleadoAndEstado(empleado.getIdEmpleado(), EstadoContrato.VIGENTE)
                .orElse(null);
        LocalDate inicio = vigente != null ? vigente.getFechaInicio() : empleado.getFechaIngreso();
        if (inicio == null) {
            inicio = LocalDate.now();
        }
        LocalDate hoy = LocalDate.now();
        int meses = (int) Math.max(0, ChronoUnit.MONTHS.between(inicio, hoy));
        BigDecimal tasa = tasaMensual();
        BigDecimal ganados = tasa.multiply(BigDecimal.valueOf(meses)).setScale(1, RoundingMode.HALF_UP);
        BigDecimal usados = permisoRepository
                .findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(empleado.getIdEmpleado())
                .stream()
                .filter(this::consumeVacaciones)
                .map(this::diasSolicitud)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal disponibles = ganados.subtract(usados).max(BigDecimal.ZERO).setScale(1, RoundingMode.HALF_UP);
        ModalidadContrato modalidad = vigente != null ? vigente.getModalidad() : mapearModalidad(empleado);
        return new VacacionSaldoResponse(
                empleado.getIdEmpleado(),
                empleado.nombreCompleto(),
                modalidad,
                inicio,
                meses,
                tasa,
                ganados,
                usados,
                disponibles
        );
    }

    private boolean consumeVacaciones(SolicitudPermiso s) {
        return s.getTipoPermiso() != null
                && "VACACIONES".equalsIgnoreCase(s.getTipoPermiso().getCodigo())
                && ESTADOS_QUE_CONSUMEN.contains(s.getEstado());
    }

    private BigDecimal diasSolicitud(SolicitudPermiso s) {
        return BigDecimal.valueOf(ChronoUnit.DAYS.between(s.getFechaInicio(), s.getFechaFin()) + 1);
    }

    private BigDecimal tasaMensual() {
        return parametros.decimal(PARAM_TASA, TASA_DEFAULT);
    }

    private ModalidadContrato mapearModalidad(Empleado e) {
        return e.getTipoContrato() == TipoContrato.PRACTICAS
                ? ModalidadContrato.PRACTICANTE
                : ModalidadContrato.COLABORADOR;
    }

    private String siguienteCodigo() {
        int max = contratoRepository.findAll().stream()
                .map(Contrato::getCodigo)
                .map(codigo -> codigo.replaceAll("\\D", ""))
                .filter(s -> !s.isBlank())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return "CTR-" + String.format("%03d", max + 1);
    }

    private Contrato buscar(Integer id) {
        return contratoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Contrato no encontrado"));
    }

    private ContratoResponse toResponse(Contrato c) {
        HorarioLaboral h = c.getHorario();
        Empleado e = c.getEmpleado();
        return new ContratoResponse(
                c.getIdContrato(),
                c.getCodigo(),
                e.getIdEmpleado(),
                e.nombreCompleto(),
                e.getCodigoEmpleado(),
                c.getModalidad(),
                h.getIdHorario(),
                h.getNombre(),
                h.getHoraIngreso() != null ? h.getHoraIngreso().toString() : null,
                h.getHoraSalida() != null ? h.getHoraSalida().toString() : null,
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getRemuneracionBasica(),
                c.getEstado(),
                c.getObservaciones(),
                calcularSaldo(e)
        );
    }
}
