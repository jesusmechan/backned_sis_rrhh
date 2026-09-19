package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.EmpleadoUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.AsistenciaUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Marcacion;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.application.port.out.MarcacionPort;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AsistenciaService implements AsistenciaUseCase {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private final MarcacionPort marcacionRepository;
    private final EmpleadoUseCase empleadoService;
    private final EmpleadoScope empleadoScope;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public AsistenciaService(MarcacionPort marcacionRepository,
                             EmpleadoUseCase empleadoService,
                             EmpleadoScope empleadoScope,
                             AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.marcacionRepository = marcacionRepository;
        this.empleadoService = empleadoService;
        this.empleadoScope = empleadoScope;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional
    public MarcacionResponse marcar(MarcacionRequest request) {
        String origen = request.origen() != null ? request.origen() : "WEB";
        Empleado empleado = empleadoScope.resolverSolicitante(request.idEmpleado());
        if (currentUser.isAdminOrRrhh() && request.idEmpleado() != null && request.origen() == null) {
            origen = "MANUAL";
        }
        OffsetDateTime fechaHora = request.fechaHora() != null ? request.fechaHora() : OffsetDateTime.now();
        LocalDate fecha = fechaHora.atZoneSameInstant(LIMA).toLocalDate();
        DayOfWeek dia = fecha.getDayOfWeek();
        if ("WEB".equals(origen) && (dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY)) {
            throw DomainException.badRequest("No se puede marcar asistencia los sábados ni los domingos");
        }
        if (!marcacionRepository.findByEmpleado_IdEmpleadoAndTipoAndFecha(empleado.getIdEmpleado(), request.tipo(), fecha).isEmpty()
                && "WEB".equals(origen)) {
            throw DomainException.conflict("Ya existe una marcación de " + request.tipo() + " para hoy");
        }
        Marcacion m = new Marcacion();
        m.setEmpleado(empleado);
        m.setTipo(request.tipo());
        m.setFechaHora(fechaHora);
        m.setOrigen(origen);
        m.setObservacion(request.observacion());
        m.setUsuarioRegistro(currentUser.usuario());
        marcacionRepository.saveAndFlush(m);
        auditoriaService.registrar(currentUser.usuario(), "MARCAR", "ASISTENCIA", m.getIdMarcacion(), request.tipo().name());
        return DtoMapper.marcacion(marcacionRepository.findById(m.getIdMarcacion()).orElse(m));
    }

    @Transactional(readOnly = true)
    public List<MarcacionResponse> listar(Integer idEmpleado, LocalDate desde, LocalDate hasta) {
        LocalDate ini = desde != null ? desde : LocalDate.now().minusDays(15);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        if (!currentUser.isAdminOrRrhh()) {
            Integer propio = currentUser.idEmpleado();
            if (propio == null) {
                throw DomainException.forbidden("No tiene trabajador asociado");
            }
            return marcacionRepository.findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(propio).stream()
                    .filter(m -> !m.getFechaHora().toLocalDate().isBefore(ini) && !m.getFechaHora().toLocalDate().isAfter(fin))
                    .map(DtoMapper::marcacion)
                    .toList();
        }
        if (idEmpleado != null) {
            return marcacionRepository.findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(idEmpleado).stream()
                    .filter(m -> !m.getFechaHora().toLocalDate().isBefore(ini) && !m.getFechaHora().toLocalDate().isAfter(fin))
                    .map(DtoMapper::marcacion)
                    .toList();
        }
        return marcacionRepository.findByFechaBetweenOrderByFechaHoraDesc(ini, fin).stream()
                .map(DtoMapper::marcacion).toList();
    }

    @Transactional(readOnly = true)
    public List<MarcacionResponse> reporte(Integer idEmpleado, LocalDate desde, LocalDate hasta) {
        LocalDate ini = desde != null ? desde : LocalDate.of(2019, 1, 1);
        LocalDate fin = hasta != null ? hasta : LocalDate.now();
        if (idEmpleado != null) {
            return marcacionRepository.findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(idEmpleado).stream()
                    .filter(m -> {
                        LocalDate f = m.getFecha() != null ? m.getFecha() : m.getFechaHora().toLocalDate();
                        return !f.isBefore(ini) && !f.isAfter(fin);
                    })
                    .map(DtoMapper::marcacion)
                    .toList();
        }
        return marcacionRepository.findByFechaBetweenOrderByFechaHoraDesc(ini, fin).stream()
                .map(DtoMapper::marcacion)
                .toList();
    }

    @Transactional(readOnly = true)
    public MarcacionResponse obtener(Integer id) {
        Marcacion m = marcacionRepository.findById(id).orElseThrow(() -> DomainException.notFound("Marcación no encontrada"));
        empleadoScope.assertMismaPersona(m.getEmpleado().getIdEmpleado());
        return DtoMapper.marcacion(m);
    }

    @Transactional
    public MarcacionResponse actualizar(Integer id, MarcacionRequest request) {
        if (!currentUser.isAdminOrRrhh()) {
            throw DomainException.forbidden("Solo RR. HH. o Administrador puede corregir marcaciones");
        }
        Marcacion m = marcacionRepository.findById(id).orElseThrow(() -> DomainException.notFound("Marcación no encontrada"));
        if (request.tipo() != null) {
            m.setTipo(request.tipo());
        }
        if (request.fechaHora() != null) {
            m.setFechaHora(request.fechaHora());
        }
        if (request.observacion() != null) {
            m.setObservacion(request.observacion());
        }
        m.setOrigen("CORRECCION");
        auditoriaService.registrar(currentUser.usuario(), "CORREGIR", "ASISTENCIA", id, null);
        return DtoMapper.marcacion(m);
    }
}
