package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.BoletaPdfPort;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.PlanillaUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.AsientoContable;
import pe.andina.rrhh.domain.model.AsientoLinea;
import pe.andina.rrhh.domain.model.Contrato;
import pe.andina.rrhh.domain.model.CuentaContable;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Planilla;
import pe.andina.rrhh.domain.model.PlanillaDetalle;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoAsiento;
import pe.andina.rrhh.domain.model.enums.EstadoContrato;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.EstadoPlanilla;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.model.enums.ModalidadContrato;
import pe.andina.rrhh.application.dto.AppDtos.AsientoLineaResponse;
import pe.andina.rrhh.application.dto.AppDtos.AsientoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaDetalleResponse;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaRequest;
import pe.andina.rrhh.application.dto.AppDtos.PlanillaResponse;
import pe.andina.rrhh.application.dto.BoletaPdfFile;
import pe.andina.rrhh.application.port.out.AsientoContablePort;
import pe.andina.rrhh.application.port.out.ContratoPort;
import pe.andina.rrhh.application.port.out.CuentaContablePort;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.application.port.out.PlanillaDetallePort;
import pe.andina.rrhh.application.port.out.PlanillaPort;
import pe.andina.rrhh.application.port.out.SolicitudHoraExtraPort;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
public class PlanillaService implements PlanillaUseCase {

    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final PlanillaPort planillaRepository;
    private final PlanillaDetallePort detalleRepository;
    private final AsientoContablePort asientoRepository;
    private final EmpleadoPort empleadoRepository;
    private final ContratoPort contratoRepository;
    private final SolicitudHoraExtraPort horaExtraRepository;
    private final SolicitudPermisoPort permisoRepository;
    private final ParametroPort parametros;
    private final CuentaContablePort cuentaRepository;
    private final AuditoriaUseCase auditoriaService;
    private final BoletaPdfPort boletaPdfService;

    private final CurrentUserPort currentUser;

    public PlanillaService(PlanillaPort planillaRepository,
                           PlanillaDetallePort detalleRepository,
                           AsientoContablePort asientoRepository,
                           EmpleadoPort empleadoRepository,
                           ContratoPort contratoRepository,
                           SolicitudHoraExtraPort horaExtraRepository,
                           SolicitudPermisoPort permisoRepository,
                           ParametroPort parametros,
                           CuentaContablePort cuentaRepository,
                           AuditoriaUseCase auditoriaService,
                           BoletaPdfPort boletaPdfService,
                           CurrentUserPort currentUser) {
        this.planillaRepository = planillaRepository;
        this.detalleRepository = detalleRepository;
        this.asientoRepository = asientoRepository;
        this.empleadoRepository = empleadoRepository;
        this.contratoRepository = contratoRepository;
        this.horaExtraRepository = horaExtraRepository;
        this.permisoRepository = permisoRepository;
        this.parametros = parametros;
        this.cuentaRepository = cuentaRepository;
        this.auditoriaService = auditoriaService;
        this.boletaPdfService = boletaPdfService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<PlanillaResponse> listar() {
        return planillaRepository.findAllByOrderByAnioDescMesDesc().stream()
                .map(p -> toResponse(p, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanillaResponse obtener(Integer id) {
        Planilla p = buscar(id);
        return toResponse(p, detalleRepository.findByPlanilla_IdPlanillaOrderByIdDetalleAsc(id));
    }

    @Transactional
    public PlanillaResponse crear(PlanillaRequest request) {
        planillaRepository.findByAnioAndMes(request.anio(), request.mes()).ifPresent(p -> {
            throw DomainException.conflict("Ya existe una planilla para " + periodo(p.getAnio(), p.getMes()));
        });
        Planilla p = new Planilla();
        p.setAnio(request.anio());
        p.setMes(request.mes());
        p.setObservaciones(request.observaciones());
        p.setEstado(EstadoPlanilla.BORRADOR);
        planillaRepository.save(p);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "PLANILLA", p.getIdPlanilla(), periodo(p.getAnio(), p.getMes()));
        return toResponse(p, List.of());
    }

    @Transactional
    public PlanillaResponse calcular(Integer id) {
        Planilla p = buscar(id);
        if (p.getEstado() == EstadoPlanilla.CERRADA) {
            throw DomainException.badRequest("La planilla cerrada no se vuelve a calcular");
        }
        if (p.getEstado() == EstadoPlanilla.ANULADA) {
            throw DomainException.badRequest("La planilla anulada no se puede calcular");
        }
        detalleRepository.deleteByPlanilla_IdPlanilla(id);
        detalleRepository.flush();
        YearMonth ym = YearMonth.of(p.getAnio(), p.getMes());
        LocalDate desde = ym.atDay(1);
        LocalDate hasta = ym.atEndOfMonth();
        BigDecimal tasaHe = parametros.decimal("tasa_hora_extra", "1.25");
        BigDecimal tasaOnp = parametros.decimal("tasa_onp", "0.13");
        BigDecimal tasaEssalud = parametros.decimal("tasa_essalud", "0.09");
        BigDecimal horasMes = parametros.decimal("horas_mensuales_base", "240");

        BigDecimal tBruto = CERO;
        BigDecimal tDesc = CERO;
        BigDecimal tAporte = CERO;
        BigDecimal tNeto = CERO;

        for (Empleado empleado : empleadoRepository.findAll()) {
            if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
                continue;
            }
            Contrato contrato = contratoRepository
                    .findFirstByEmpleado_IdEmpleadoAndEstado(empleado.getIdEmpleado(), EstadoContrato.VIGENTE)
                    .orElse(null);
            if (contrato == null || contrato.getFechaInicio().isAfter(hasta)) {
                continue;
            }
            if (contrato.getFechaFin() != null && contrato.getFechaFin().isBefore(desde)) {
                continue;
            }
            PlanillaDetalle d = calcularBoleta(p, empleado, contrato, desde, hasta, tasaHe, tasaOnp, tasaEssalud, horasMes);
            detalleRepository.save(d);
            tBruto = tBruto.add(d.getBruto());
            tDesc = tDesc.add(d.getOnp()).add(d.getDescuentoAusencias());
            tAporte = tAporte.add(d.getEssalud());
            tNeto = tNeto.add(d.getNeto());
        }
        p.setTotalBruto(tBruto);
        p.setTotalDescuentos(tDesc);
        p.setTotalAportes(tAporte);
        p.setTotalNeto(tNeto);
        p.setEstado(EstadoPlanilla.CALCULADA);
        p.setFechaCalculo(OffsetDateTime.now());
        auditoriaService.registrar(currentUser.usuario(), "CALCULAR", "PLANILLA", id, periodo(p.getAnio(), p.getMes()));
        return toResponse(p, detalleRepository.findByPlanilla_IdPlanillaOrderByIdDetalleAsc(id));
    }

    @Transactional
    public PlanillaResponse cerrar(Integer id) {
        Planilla p = buscar(id);
        if (p.getEstado() != EstadoPlanilla.CALCULADA) {
            throw DomainException.badRequest("Solo se cierra una planilla calculada");
        }
        if (asientoRepository.findByPlanilla_IdPlanilla(id).isPresent()) {
            throw DomainException.conflict("Esta planilla ya tiene asiento contable");
        }
        p.setEstado(EstadoPlanilla.CERRADA);
        p.setFechaCierre(OffsetDateTime.now());
        AsientoContable asiento = generarAsiento(p);
        asientoRepository.save(asiento);
        auditoriaService.registrar(currentUser.usuario(), "CERRAR", "PLANILLA", id, asiento.getCodigo());
        return toResponse(p, detalleRepository.findByPlanilla_IdPlanillaOrderByIdDetalleAsc(id));
    }

    @Transactional(readOnly = true)
    public BoletaPdfFile pdfBoletas(Integer id) {
        Planilla p = exigirExportable(id);
        List<PlanillaDetalle> boletas = detalleRepository.findCompletosByPlanilla(id);
        return new BoletaPdfFile(
                boletaPdfService.exportarPlanilla(p, boletas),
                String.format("boletas-%d-%02d.pdf", p.getAnio(), p.getMes()));
    }

    @Transactional(readOnly = true)
    public BoletaPdfFile pdfBoleta(Integer id, Integer idDetalle) {
        Planilla p = exigirExportable(id);
        PlanillaDetalle d = detalleRepository.findCompleto(id, idDetalle)
                .orElseThrow(() -> DomainException.notFound("Boleta no encontrada"));
        String codigo = d.getEmpleado() != null && d.getEmpleado().getCodigoEmpleado() != null
                ? d.getEmpleado().getCodigoEmpleado().replaceAll("[^A-Za-z0-9-]", "")
                : "boleta";
        return new BoletaPdfFile(
                boletaPdfService.exportarBoleta(p, d),
                String.format("boleta-%s-%d-%02d.pdf", codigo, p.getAnio(), p.getMes()));
    }

    private Planilla exigirExportable(Integer id) {
        Planilla p = buscar(id);
        if (p.getEstado() != EstadoPlanilla.CALCULADA && p.getEstado() != EstadoPlanilla.CERRADA) {
            throw DomainException.badRequest("Calcule la planilla antes de exportar las boletas");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public List<AsientoResponse> listarAsientos() {
        return asientoRepository.findAllConLineas().stream().map(this::toAsiento).toList();
    }

    @Transactional(readOnly = true)
    public AsientoResponse obtenerAsiento(Integer id) {
        return toAsiento(asientoRepository.findConLineas(id).orElseThrow(() -> DomainException.notFound("Asiento no encontrado")));
    }

    private PlanillaDetalle calcularBoleta(Planilla p, Empleado empleado, Contrato contrato,
                                          LocalDate desde, LocalDate hasta,
                                          BigDecimal tasaHe, BigDecimal tasaOnp, BigDecimal tasaEssalud, BigDecimal horasMes) {
        BigDecimal base = contrato.getRemuneracionBasica() == null || contrato.getRemuneracionBasica().signum() <= 0
                ? (contrato.getModalidad() == ModalidadContrato.PRACTICANTE ? new BigDecimal("1500") : new BigDecimal("2500"))
                : contrato.getRemuneracionBasica();
        base = base.setScale(2, RoundingMode.HALF_UP);
        BigDecimal horas = horasExtras(empleado.getIdEmpleado(), desde, hasta);
        BigDecimal valorHora = horasMes.signum() == 0 ? CERO : base.divide(horasMes, 6, RoundingMode.HALF_UP);
        BigDecimal montoHe = horas.multiply(valorHora).multiply(tasaHe).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dias = diasNoLaborados(empleado.getIdEmpleado(), desde, hasta);
        BigDecimal descAus = dias.multiply(base).divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        boolean colaborador = contrato.getModalidad() != ModalidadContrato.PRACTICANTE;
        BigDecimal bruto = base.add(montoHe).setScale(2, RoundingMode.HALF_UP);
        BigDecimal onp = colaborador ? bruto.multiply(tasaOnp).setScale(2, RoundingMode.HALF_UP) : CERO;
        BigDecimal essalud = colaborador ? bruto.multiply(tasaEssalud).setScale(2, RoundingMode.HALF_UP) : CERO;
        BigDecimal neto = bruto.subtract(onp).subtract(descAus).max(CERO).setScale(2, RoundingMode.HALF_UP);

        PlanillaDetalle d = new PlanillaDetalle();
        d.setPlanilla(p);
        d.setEmpleado(empleado);
        d.setContrato(contrato);
        d.setModalidad(contrato.getModalidad().name());
        d.setRemuneracionBasica(base);
        d.setHorasExtras(horas);
        d.setMontoHorasExtras(montoHe);
        d.setDiasNoLaborados(dias);
        d.setDescuentoAusencias(descAus);
        d.setOnp(onp);
        d.setEssalud(essalud);
        d.setBruto(bruto);
        d.setNeto(neto);
        return d;
    }

    private BigDecimal horasExtras(Integer idEmpleado, LocalDate desde, LocalDate hasta) {
        return horaExtraRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(idEmpleado).stream()
                .filter(h -> h.getEstado() == EstadoSolicitud.APROBADO)
                .filter(h -> h.getFecha() != null && !h.getFecha().isBefore(desde) && !h.getFecha().isAfter(hasta))
                .map(SolicitudHoraExtra::getCantidadHoras)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal diasNoLaborados(Integer idEmpleado, LocalDate desde, LocalDate hasta) {
        long dias = 0;
        for (SolicitudPermiso s : permisoRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(idEmpleado)) {
            if (s.getEstado() != EstadoSolicitud.APROBADO) {
                continue;
            }
            if (s.getTipoPermiso() != null && "VACACIONES".equalsIgnoreCase(s.getTipoPermiso().getCodigo())) {
                continue;
            }
            LocalDate ini = s.getFechaInicio().isBefore(desde) ? desde : s.getFechaInicio();
            LocalDate fin = s.getFechaFin().isAfter(hasta) ? hasta : s.getFechaFin();
            if (fin.isBefore(ini)) {
                continue;
            }
            dias += fin.toEpochDay() - ini.toEpochDay() + 1;
        }
        return BigDecimal.valueOf(dias).setScale(2, RoundingMode.HALF_UP);
    }

    private AsientoContable generarAsiento(Planilla p) {
        AsientoContable a = new AsientoContable();
        a.setCodigo(String.format("ASI-%d-%02d", p.getAnio(), p.getMes()));
        a.setPlanilla(p);
        a.setFecha(YearMonth.of(p.getAnio(), p.getMes()).atEndOfMonth());
        a.setGlosa("Planilla de remuneraciones " + periodo(p.getAnio(), p.getMes()));
        a.setEstado(EstadoAsiento.CONTABILIZADO);
        addLineaUso(a, "SUELDOS", "6211", "Sueldos y salarios", p.getTotalBruto(), CERO);
        addLineaUso(a, "ESSALUD_GASTO", "6271", "EsSalud", p.getTotalAportes(), CERO);
        BigDecimal onp = CERO;
        BigDecimal ausencias = CERO;
        for (PlanillaDetalle d : detalleRepository.findByPlanilla_IdPlanillaOrderByIdDetalleAsc(p.getIdPlanilla())) {
            onp = onp.add(d.getOnp());
            ausencias = ausencias.add(d.getDescuentoAusencias());
        }
        addLineaUso(a, "ONP_POR_PAGAR", "4031", "ONP por pagar", CERO, onp);
        addLineaUso(a, "ESSALUD_POR_PAGAR", "4032", "EsSalud por pagar", CERO, p.getTotalAportes());
        addLineaUso(a, "REMU_POR_PAGAR", "4111", "Remuneraciones por pagar", CERO, p.getTotalNeto());
        addLineaUso(a, "DESC_AUSENCIAS", "4699", "Descuentos por ausencias", CERO, ausencias);
        a.setTotalDebe(p.getTotalBruto().add(p.getTotalAportes()));
        a.setTotalHaber(onp.add(p.getTotalAportes()).add(p.getTotalNeto()).add(ausencias));
        return a;
    }

    private void addLineaUso(AsientoContable a, String uso, String fallbackCodigo, String fallbackNombre,
                             BigDecimal debe, BigDecimal haber) {
        CuentaContable c = cuentaRepository.findByUsoIgnoreCase(uso)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElse(null);
        addLinea(a,
                c != null ? c.getCodigo() : fallbackCodigo,
                c != null ? c.getNombre() : fallbackNombre,
                debe, haber);
    }

    private void addLinea(AsientoContable a, String cuenta, String nombre, BigDecimal debe, BigDecimal haber) {
        if ((debe == null || debe.signum() == 0) && (haber == null || haber.signum() == 0)) {
            return;
        }
        AsientoLinea l = new AsientoLinea();
        l.setCuenta(cuenta);
        l.setNombreCuenta(nombre);
        l.setDebe(nz(debe));
        l.setHaber(nz(haber));
        a.addLinea(l);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? CERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private Planilla buscar(Integer id) {
        return planillaRepository.findById(id).orElseThrow(() -> DomainException.notFound("Planilla no encontrada"));
    }

    private String periodo(Integer anio, Integer mes) {
        String nombre = java.time.Month.of(mes).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-PE"));
        return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + anio;
    }

    private PlanillaResponse toResponse(Planilla p, List<PlanillaDetalle> detalles) {
        return new PlanillaResponse(
                p.getIdPlanilla(), p.getAnio(), p.getMes(), periodo(p.getAnio(), p.getMes()),
                p.getEstado(), p.getTotalBruto(), p.getTotalDescuentos(), p.getTotalAportes(), p.getTotalNeto(),
                p.getObservaciones(), p.getFechaCalculo(), p.getFechaCierre(),
                detalles.stream().map(this::toDetalle).toList()
        );
    }

    private PlanillaDetalleResponse toDetalle(PlanillaDetalle d) {
        return new PlanillaDetalleResponse(
                d.getIdDetalle(), d.getEmpleado().getIdEmpleado(), d.getEmpleado().nombreCompleto(),
                d.getModalidad(), d.getRemuneracionBasica(), d.getHorasExtras(), d.getMontoHorasExtras(),
                d.getDiasNoLaborados(), d.getDescuentoAusencias(), d.getOnp(), d.getEssalud(), d.getBruto(), d.getNeto()
        );
    }

    private AsientoResponse toAsiento(AsientoContable a) {
        Planilla p = a.getPlanilla();
        return new AsientoResponse(
                a.getIdAsiento(), a.getCodigo(),
                p != null ? p.getIdPlanilla() : null,
                p != null ? periodo(p.getAnio(), p.getMes()) : null,
                a.getFecha(), a.getGlosa(), a.getEstado(), a.getTotalDebe(), a.getTotalHaber(),
                a.getLineas().stream().map(l -> new AsientoLineaResponse(
                        l.getIdLinea(), l.getCuenta(), l.getNombreCuenta(), l.getDebe(), l.getHaber()
                )).toList()
        );
    }
}
