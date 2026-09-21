package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.NotificacionUseCase;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.domain.model.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.application.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.application.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.port.out.HistorialSolicitudPort;
import pe.andina.rrhh.application.port.out.SolicitudHoraExtraPort;
import pe.andina.rrhh.application.port.out.SolicitudPasoAprobacionPort;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;
import pe.andina.rrhh.domain.service.BandejaAsignacion;
import pe.andina.rrhh.domain.service.HoraExtraReglas;
import pe.andina.rrhh.domain.service.PermisoReglas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SolicitudService implements SolicitudUseCase {

    private final SolicitudPermisoPort permisoRepository;
    private final SolicitudHoraExtraPort horaExtraRepository;
    private final SolicitudPasoAprobacionPort pasoRepository;
    private final HistorialSolicitudPort historialRepository;
    private final TipoPermisoPort tipoPermisoRepository;
    private final EmpleadoScope empleadoScope;
    private final PermisoReglas permisoReglas;
    private final HoraExtraReglas horaExtraReglas;
    private final EntityManager entityManager;
    private final AuditoriaUseCase auditoriaService;
    private final NotificacionUseCase notificacionService;

    private final CurrentUserPort currentUser;

    public SolicitudService(SolicitudPermisoPort permisoRepository,
                            SolicitudHoraExtraPort horaExtraRepository,
                            SolicitudPasoAprobacionPort pasoRepository,
                            HistorialSolicitudPort historialRepository,
                            TipoPermisoPort tipoPermisoRepository,
                            EmpleadoScope empleadoScope,
                            PermisoReglas permisoReglas,
                            HoraExtraReglas horaExtraReglas,
                            EntityManager entityManager,
                            AuditoriaUseCase auditoriaService,
                            NotificacionUseCase notificacionService,
                           CurrentUserPort currentUser) {
        this.permisoRepository = permisoRepository;
        this.horaExtraRepository = horaExtraRepository;
        this.pasoRepository = pasoRepository;
        this.historialRepository = historialRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.empleadoScope = empleadoScope;
        this.permisoReglas = permisoReglas;
        this.horaExtraReglas = horaExtraReglas;
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
        this.notificacionService = notificacionService;
        this.currentUser = currentUser;
    }

    @Transactional
    public PermisoResponse crearPermiso(PermisoRequest request) {
        Empleado empleado = empleadoScope.resolverSolicitante(request.idEmpleado());
        var tipo = tipoPermisoRepository.findById(request.idTipoPermiso())
                .orElseThrow(() -> DomainException.badRequest("Tipo de permiso no existe"));
        permisoReglas.validarAlCrear(empleado, tipo, request);
        SolicitudPermiso s = new SolicitudPermiso();
        s.setEmpleado(empleado);
        s.setTipoPermiso(tipo);
        s.setFechaInicio(request.fechaInicio());
        s.setFechaFin(request.fechaFin());
        s.setHoraInicio(request.horaInicio());
        s.setHoraFin(request.horaFin());
        s.setMotivo(request.motivo());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        permisoRepository.saveAndFlush(s);
        entityManager.refresh(s);
        auditoriaService.registrar(currentUser.usuario(), "REGISTRAR", "PERMISO", s.getIdSolicitudPermiso(), s.getMotivo());
        notificarPasoEnCurso(pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso()));
        return toPermiso(s);
    }

    @Transactional
    public HoraExtraResponse crearHoraExtra(HoraExtraRequest request) {
        Empleado empleado = empleadoScope.resolverSolicitante(request.idEmpleado());
        horaExtraReglas.validarAlCrear(empleado, request);
        SolicitudHoraExtra s = new SolicitudHoraExtra();
        s.setEmpleado(empleado);
        s.setFecha(request.fecha());
        s.setHoraInicio(request.horaInicio());
        s.setHoraFin(request.horaFin());
        s.setCantidadHoras(request.cantidadHoras());
        s.setMotivo(request.motivo());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        horaExtraRepository.saveAndFlush(s);
        entityManager.refresh(s);
        auditoriaService.registrar(currentUser.usuario(), "REGISTRAR", "HORA_EXTRA", s.getIdSolicitudHoraExtra(), s.getMotivo());
        notificarPasoEnCurso(pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra()));
        return toHoraExtra(s);
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        List<SolicitudPermiso> data = currentUser.puedeVerConjuntoOperativo()
                ? permisoRepository.findAll()
                : permisoRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(currentUser.idEmpleado());
        return data.stream().map(this::toPermiso).toList();
    }

    @Transactional(readOnly = true)
    public PermisoResponse obtenerPermiso(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        empleadoScope.assertPuedeConsultar(
                s.getEmpleado().getIdEmpleado(),
                participoEnCircuito(pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso())));
        return toPermiso(s);
    }

    @Transactional(readOnly = true)
    public List<HoraExtraResponse> listarHorasExtras() {
        List<SolicitudHoraExtra> data = currentUser.puedeVerConjuntoOperativo()
                ? horaExtraRepository.findAll()
                : horaExtraRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(currentUser.idEmpleado());
        return data.stream().map(this::toHoraExtra).toList();
    }

    @Transactional(readOnly = true)
    public HoraExtraResponse obtenerHoraExtra(Integer id) {
        SolicitudHoraExtra s = horaExtraRepository.findById(id).orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        empleadoScope.assertPuedeConsultar(
                s.getEmpleado().getIdEmpleado(),
                participoEnCircuito(pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra())));
        return toHoraExtra(s);
    }

    @Transactional
    public PermisoResponse cancelarPermiso(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id).orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        empleadoScope.assertDuenioORrhh(s.getEmpleado().getIdEmpleado());
        exigirPendiente(s.getEstado());
        s.setEstado(EstadoSolicitud.CANCELADO);
        return toPermiso(s);
    }

    @Transactional
    public HoraExtraResponse cancelarHoraExtra(Integer id) {
        SolicitudHoraExtra s = horaExtraRepository.findById(id).orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        empleadoScope.assertDuenioORrhh(s.getEmpleado().getIdEmpleado());
        exigirPendiente(s.getEstado());
        s.setEstado(EstadoSolicitud.CANCELADO);
        return toHoraExtra(s);
    }

    @Transactional(readOnly = true)
    public BandejaItem pasoPendiente(Integer idPaso) {
        SolicitudPasoAprobacion paso = exigirPasoEnCurso(idPaso);
        if (!BandejaAsignacion.corresponde(paso, currentUser.idUsuario(), currentUser.codigoRol())) {
            throw DomainException.forbidden("Este paso no le corresponde");
        }
        return toBandeja(paso, true);
    }

    @Transactional
    public PasoResponse decidir(Integer idPaso, boolean aprobar, DecisionRequest request) {
        SolicitudPasoAprobacion paso = exigirPasoEnCurso(idPaso);
        if (!BandejaAsignacion.corresponde(paso, currentUser.idUsuario(), currentUser.codigoRol())) {
            throw DomainException.forbidden("Este paso no le corresponde");
        }
        Usuario decisor = currentUser.usuario();
        paso.setUsuarioDecision(decisor);
        paso.setComentario(request.comentario());
        paso.setEstado(aprobar ? EstadoPasoAprobacion.APROBADO : EstadoPasoAprobacion.RECHAZADO);
        pasoRepository.saveAndFlush(paso);
        entityManager.flush();
        entityManager.refresh(paso);
        if (paso.getSolicitudPermiso() != null) {
            entityManager.refresh(paso.getSolicitudPermiso());
        }
        if (paso.getSolicitudHoraExtra() != null) {
            entityManager.refresh(paso.getSolicitudHoraExtra());
        }
        List<SolicitudPasoAprobacion> pasos = pasosDe(paso);
        pasos.forEach(entityManager::refresh);
        auditoriaService.registrar(decisor, aprobar ? "APROBAR" : "RECHAZAR", "PASO_APROBACION", idPaso, request.comentario());
        notificarTrasDecision(pasos, paso, aprobar);
        return DtoMapper.paso(paso);
    }

    @Transactional(readOnly = true)
    public List<BandejaItem> bandeja(String vista) {
        if (vista != null && "SEGUIMIENTO".equalsIgnoreCase(vista.trim())) {
            return bandejaSeguimiento();
        }
        return bandejaPendientes();
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historialPermiso(Integer id) {
        obtenerPermiso(id);
        return historialRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByIdHistorialAsc(id)
                .stream().map(DtoMapper::historial).toList();
    }

    @Transactional(readOnly = true)
    public List<HistorialResponse> historialHoraExtra(Integer id) {
        obtenerHoraExtra(id);
        return historialRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByIdHistorialAsc(id)
                .stream().map(DtoMapper::historial).toList();
    }

    private List<BandejaItem> bandejaPendientes() {
        return pasoRepository.findByEstado(EstadoPasoAprobacion.EN_CURSO).stream()
                .filter(p -> BandejaAsignacion.corresponde(p, currentUser.idUsuario(), currentUser.codigoRol()))
                .map(p -> toBandeja(p, true))
                .toList();
    }

    private List<BandejaItem> bandejaSeguimiento() {
        Map<String, BandejaItem> items = new LinkedHashMap<>();
        Integer idEmpleado = currentUser.idEmpleado();
        Integer idUsuario = currentUser.idUsuario();

        if (idEmpleado != null) {
            permisoRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(idEmpleado)
                    .forEach(s -> putSeguimiento(items, s));
            horaExtraRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(idEmpleado)
                    .forEach(s -> putSeguimiento(items, s));
        }

        Set<EstadoPasoAprobacion> resueltos = Set.of(
                EstadoPasoAprobacion.APROBADO,
                EstadoPasoAprobacion.RECHAZADO,
                EstadoPasoAprobacion.OMITIDO,
                EstadoPasoAprobacion.CANCELADO);

        List<SolicitudPasoAprobacion> participados = new ArrayList<>();
        participados.addAll(pasoRepository.findByUsuarioDecision_IdUsuario(idUsuario));
        participados.addAll(pasoRepository.findByUsuarioAsignado_IdUsuarioAndEstadoIn(idUsuario, resueltos));
        for (SolicitudPasoAprobacion paso : participados) {
            if (paso.getSolicitudPermiso() != null) {
                putSeguimiento(items, paso.getSolicitudPermiso());
            } else if (paso.getSolicitudHoraExtra() != null) {
                putSeguimiento(items, paso.getSolicitudHoraExtra());
            }
        }

        List<BandejaItem> data = new ArrayList<>(items.values());
        data.sort(Comparator.comparing(BandejaItem::fechaInicio, Comparator.nullsLast(Comparator.reverseOrder())));
        return data;
    }

    private void putSeguimiento(Map<String, BandejaItem> items, SolicitudPermiso s) {
        items.putIfAbsent("PERMISO-" + s.getIdSolicitudPermiso(), toBandejaSeguimiento(s));
    }

    private void putSeguimiento(Map<String, BandejaItem> items, SolicitudHoraExtra s) {
        items.putIfAbsent("HORA_EXTRA-" + s.getIdSolicitudHoraExtra(), toBandejaSeguimiento(s));
    }

    private BandejaItem toBandeja(SolicitudPasoAprobacion p, boolean puedeDecidir) {
        if (p.getSolicitudPermiso() != null) {
            SolicitudPermiso s = p.getSolicitudPermiso();
            return new BandejaItem(p.getIdPasoSolicitud(), "PERMISO", s.getIdSolicitudPermiso(),
                    s.getEmpleado().nombreCompleto(), s.getTipoPermiso().getNombre(),
                    p.getNumeroPaso(), p.getNombrePaso(), p.getTipoAprobador(), s.getMotivo(),
                    p.getFechaInicio() != null ? p.getFechaInicio() : s.getFechaCreacion(),
                    s.getEstado().name(), puedeDecidir);
        }
        SolicitudHoraExtra s = p.getSolicitudHoraExtra();
        return new BandejaItem(p.getIdPasoSolicitud(), "HORA_EXTRA", s.getIdSolicitudHoraExtra(),
                s.getEmpleado().nombreCompleto(), "Horas extras",
                p.getNumeroPaso(), p.getNombrePaso(), p.getTipoAprobador(), s.getMotivo(),
                p.getFechaInicio() != null ? p.getFechaInicio() : s.getFechaCreacion(),
                s.getEstado().name(), puedeDecidir);
    }

    private BandejaItem toBandejaSeguimiento(SolicitudPermiso s) {
        List<SolicitudPasoAprobacion> pasos = pasoRepository
                .findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso());
        return toBandeja(pasoVisible(pasos), false, s.getEmpleado().nombreCompleto(),
                s.getTipoPermiso().getNombre(), s.getMotivo(), s.getFechaCreacion(),
                s.getEstado().name(), "PERMISO", s.getIdSolicitudPermiso());
    }

    private BandejaItem toBandejaSeguimiento(SolicitudHoraExtra s) {
        List<SolicitudPasoAprobacion> pasos = pasoRepository
                .findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra());
        return toBandeja(pasoVisible(pasos), false, s.getEmpleado().nombreCompleto(),
                "Horas extras", s.getMotivo(), s.getFechaCreacion(),
                s.getEstado().name(), "HORA_EXTRA", s.getIdSolicitudHoraExtra());
    }

    private BandejaItem toBandeja(SolicitudPasoAprobacion p, boolean puedeDecidir, String solicitante,
                                 String tipoTramite, String motivo, java.time.OffsetDateTime fecha,
                                 String estadoSolicitud, String tipoSolicitud, Integer idSolicitud) {
        if (p == null) {
            return new BandejaItem(null, tipoSolicitud, idSolicitud, solicitante, tipoTramite,
                    null, null, null, motivo, fecha, estadoSolicitud, puedeDecidir);
        }
        return new BandejaItem(p.getIdPasoSolicitud(), tipoSolicitud, idSolicitud, solicitante, tipoTramite,
                p.getNumeroPaso(), p.getNombrePaso(), p.getTipoAprobador(), motivo,
                fecha != null ? fecha : p.getFechaInicio(), estadoSolicitud, puedeDecidir);
    }

    private SolicitudPasoAprobacion pasoVisible(List<SolicitudPasoAprobacion> pasos) {
        if (pasos == null || pasos.isEmpty()) {
            return null;
        }
        return pasos.stream()
                .filter(p -> p.getEstado() == EstadoPasoAprobacion.EN_CURSO)
                .findFirst()
                .orElse(pasos.get(pasos.size() - 1));
    }

    private SolicitudPasoAprobacion exigirPasoEnCurso(Integer idPaso) {
        SolicitudPasoAprobacion paso = pasoRepository.findById(idPaso)
                .orElseThrow(() -> DomainException.notFound("Paso no encontrado"));
        if (paso.getEstado() != EstadoPasoAprobacion.EN_CURSO) {
            throw DomainException.badRequest("Este paso ya no está pendiente de decisión");
        }
        return paso;
    }

    private void exigirPendiente(EstadoSolicitud estado) {
        if (estado != EstadoSolicitud.PENDIENTE) {
            throw DomainException.badRequest("Solo se puede cancelar una solicitud pendiente");
        }
    }

    private void notificarPasoEnCurso(List<SolicitudPasoAprobacion> pasos) {
        pasos.stream()
                .filter(p -> p.getEstado() == EstadoPasoAprobacion.EN_CURSO)
                .findFirst()
                .ifPresent(p -> notificacionService.avisarPasoEnCurso(p, currentUser.idUsuario()));
    }

    private void notificarTrasDecision(List<SolicitudPasoAprobacion> pasos, SolicitudPasoAprobacion decidido, boolean aprobar) {
        Integer actor = currentUser.idUsuario();
        var siguiente = pasos.stream()
                .filter(p -> p.getEstado() == EstadoPasoAprobacion.EN_CURSO)
                .findFirst();
        if (siguiente.isPresent()) {
            notificacionService.avisarPasoEnCurso(siguiente.get(), actor);
            return;
        }
        notificacionService.avisarResultado(decidido, aprobar, actor);
    }

    private List<SolicitudPasoAprobacion> pasosDe(SolicitudPasoAprobacion paso) {
        if (paso.getSolicitudPermiso() != null) {
            return pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(
                    paso.getSolicitudPermiso().getIdSolicitudPermiso());
        }
        return pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(
                paso.getSolicitudHoraExtra().getIdSolicitudHoraExtra());
    }

    private PermisoResponse toPermiso(SolicitudPermiso s) {
        return DtoMapper.permiso(s, pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso()));
    }

    private HoraExtraResponse toHoraExtra(SolicitudHoraExtra s) {
        return DtoMapper.horaExtra(s, pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra()));
    }

    private boolean participoEnCircuito(List<SolicitudPasoAprobacion> pasos) {
        Integer idUsuario = currentUser.idUsuario();
        if (pasos == null || idUsuario == null) {
            return false;
        }
        return pasos.stream().anyMatch(p ->
                (p.getUsuarioDecision() != null && idUsuario.equals(p.getUsuarioDecision().getIdUsuario()))
                        || (p.getUsuarioAsignado() != null && idUsuario.equals(p.getUsuarioAsignado().getIdUsuario())));
    }
}
