package pe.andina.rrhh.service;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.SolicitudHoraExtra;
import pe.andina.rrhh.domain.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.SolicitudPermiso;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.enums.EstadoSolicitud;
import pe.andina.rrhh.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.repo.HistorialSolicitudRepository;
import pe.andina.rrhh.repo.SolicitudHoraExtraRepository;
import pe.andina.rrhh.repo.SolicitudPasoAprobacionRepository;
import pe.andina.rrhh.repo.SolicitudPermisoRepository;
import pe.andina.rrhh.repo.TipoPermisoRepository;
import pe.andina.rrhh.security.SecurityUtils;
import pe.andina.rrhh.security.UsuarioPrincipal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SolicitudService {

    private final SolicitudPermisoRepository permisoRepository;
    private final SolicitudHoraExtraRepository horaExtraRepository;
    private final SolicitudPasoAprobacionRepository pasoRepository;
    private final HistorialSolicitudRepository historialRepository;
    private final TipoPermisoRepository tipoPermisoRepository;
    private final EmpleadoService empleadoService;
    private final EntityManager entityManager;
    private final AuditoriaService auditoriaService;

    public SolicitudService(SolicitudPermisoRepository permisoRepository,
                            SolicitudHoraExtraRepository horaExtraRepository,
                            SolicitudPasoAprobacionRepository pasoRepository,
                            HistorialSolicitudRepository historialRepository,
                            TipoPermisoRepository tipoPermisoRepository,
                            EmpleadoService empleadoService,
                            EntityManager entityManager,
                            AuditoriaService auditoriaService) {
        this.permisoRepository = permisoRepository;
        this.horaExtraRepository = horaExtraRepository;
        this.pasoRepository = pasoRepository;
        this.historialRepository = historialRepository;
        this.tipoPermisoRepository = tipoPermisoRepository;
        this.empleadoService = empleadoService;
        this.entityManager = entityManager;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PermisoResponse crearPermiso(PermisoRequest request) {
        Empleado empleado = resolverEmpleadoSolicitante(request.idEmpleado());
        SolicitudPermiso s = new SolicitudPermiso();
        s.setEmpleado(empleado);
        s.setTipoPermiso(tipoPermisoRepository.findById(request.idTipoPermiso())
                .orElseThrow(() -> ApiException.badRequest("Tipo de permiso no existe")));
        s.setFechaInicio(request.fechaInicio());
        s.setFechaFin(request.fechaFin());
        s.setHoraInicio(request.horaInicio());
        s.setHoraFin(request.horaFin());
        s.setMotivo(request.motivo());
        s.setEstado(EstadoSolicitud.PENDIENTE);
        permisoRepository.saveAndFlush(s);
        entityManager.refresh(s);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "REGISTRAR", "PERMISO", s.getIdSolicitudPermiso(), s.getMotivo());
        return toPermiso(s);
    }

    @Transactional
    public HoraExtraResponse crearHoraExtra(HoraExtraRequest request) {
        Empleado empleado = resolverEmpleadoSolicitante(request.idEmpleado());
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
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "REGISTRAR", "HORA_EXTRA", s.getIdSolicitudHoraExtra(), s.getMotivo());
        return toHoraExtra(s);
    }

    @Transactional(readOnly = true)
    public List<PermisoResponse> listarPermisos() {
        UsuarioPrincipal me = SecurityUtils.current();
        List<SolicitudPermiso> data = SecurityUtils.puedeVerConjuntoOperativo()
                ? permisoRepository.findAll()
                : permisoRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(me.getIdEmpleado());
        return data.stream().map(this::toPermiso).toList();
    }

    @Transactional(readOnly = true)
    public PermisoResponse obtenerPermiso(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        assertPuedeVer(
                s.getEmpleado().getIdEmpleado(),
                pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso()));
        return toPermiso(s);
    }

    @Transactional(readOnly = true)
    public List<HoraExtraResponse> listarHorasExtras() {
        UsuarioPrincipal me = SecurityUtils.current();
        List<SolicitudHoraExtra> data = SecurityUtils.puedeVerConjuntoOperativo()
                ? horaExtraRepository.findAll()
                : horaExtraRepository.findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(me.getIdEmpleado());
        return data.stream().map(this::toHoraExtra).toList();
    }

    @Transactional(readOnly = true)
    public HoraExtraResponse obtenerHoraExtra(Integer id) {
        SolicitudHoraExtra s = horaExtraRepository.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        assertPuedeVer(
                s.getEmpleado().getIdEmpleado(),
                pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra()));
        return toHoraExtra(s);
    }

    @Transactional
    public PermisoResponse cancelarPermiso(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        assertEsDuenioORrhh(s.getEmpleado().getIdEmpleado());
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw ApiException.badRequest("Solo se puede cancelar una solicitud pendiente");
        }
        s.setEstado(EstadoSolicitud.CANCELADO);
        return toPermiso(s);
    }

    @Transactional
    public HoraExtraResponse cancelarHoraExtra(Integer id) {
        SolicitudHoraExtra s = horaExtraRepository.findById(id).orElseThrow(() -> ApiException.notFound("Solicitud no encontrada"));
        assertEsDuenioORrhh(s.getEmpleado().getIdEmpleado());
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw ApiException.badRequest("Solo se puede cancelar una solicitud pendiente");
        }
        s.setEstado(EstadoSolicitud.CANCELADO);
        return toHoraExtra(s);
    }

    @Transactional
    public PasoResponse decidir(Integer idPaso, boolean aprobar, DecisionRequest request) {
        SolicitudPasoAprobacion paso = pasoRepository.findById(idPaso)
                .orElseThrow(() -> ApiException.notFound("Paso no encontrado"));
        if (paso.getEstado() != EstadoPasoAprobacion.EN_CURSO) {
            throw ApiException.badRequest("Este paso ya no está pendiente de decisión");
        }
        UsuarioPrincipal me = SecurityUtils.current();
        if (!correspondeBandeja(paso, me)) {
            throw ApiException.forbidden("Este paso no le corresponde");
        }
        Usuario decisor = me.getUsuario();
        paso.setUsuarioDecision(decisor);
        paso.setComentario(request.comentario());
        paso.setEstado(aprobar ? EstadoPasoAprobacion.APROBADO : EstadoPasoAprobacion.RECHAZADO);
        pasoRepository.saveAndFlush(paso);
        entityManager.refresh(paso);
        auditoriaService.registrar(decisor, aprobar ? "APROBAR" : "RECHAZAR", "PASO_APROBACION", idPaso, request.comentario());
        return DtoMapper.paso(paso);
    }

    @Transactional(readOnly = true)
    public List<BandejaItem> bandeja(String vista) {
        if (vista != null && "SEGUIMIENTO".equalsIgnoreCase(vista.trim())) {
            return bandejaSeguimiento();
        }
        return bandejaPendientes();
    }

    private List<BandejaItem> bandejaPendientes() {
        UsuarioPrincipal me = SecurityUtils.current();
        return pasoRepository.findByEstado(EstadoPasoAprobacion.EN_CURSO).stream()
                .filter(p -> correspondeBandeja(p, me))
                .map(p -> toBandeja(p, true))
                .toList();
    }

    private List<BandejaItem> bandejaSeguimiento() {
        UsuarioPrincipal me = SecurityUtils.current();
        Map<String, BandejaItem> items = new LinkedHashMap<>();
        Integer idEmpleado = me.getIdEmpleado();
        Integer idUsuario = me.getIdUsuario();

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

    private boolean correspondeBandeja(SolicitudPasoAprobacion p, UsuarioPrincipal me) {
        return switch (p.getTipoAprobador()) {
            case JEFE_INMEDIATO, USUARIO -> p.getUsuarioAsignado() != null
                    && p.getUsuarioAsignado().getIdUsuario().equals(me.getIdUsuario());
            case ROL -> p.getRol() != null && p.getRol().getCodigo().equals(me.getCodigoRol());
        };
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

    private PermisoResponse toPermiso(SolicitudPermiso s) {
        return DtoMapper.permiso(s, pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso()));
    }

    private HoraExtraResponse toHoraExtra(SolicitudHoraExtra s) {
        return DtoMapper.horaExtra(s, pasoRepository.findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(s.getIdSolicitudHoraExtra()));
    }

    private Empleado resolverEmpleadoSolicitante(Integer idEmpleadoRequest) {
        UsuarioPrincipal me = SecurityUtils.current();
        if (SecurityUtils.isAdminOrRrhh() && idEmpleadoRequest != null) {
            return empleadoService.buscar(idEmpleadoRequest);
        }
        if (me.getIdEmpleado() == null) {
            throw ApiException.badRequest("El usuario no está asociado a un trabajador");
        }
        return empleadoService.buscar(me.getIdEmpleado());
    }

    private void assertPuedeVer(Integer idEmpleado, List<SolicitudPasoAprobacion> pasos) {
        if (SecurityUtils.isAdminOrRrhh() || SecurityUtils.hasRole("APROBADOR")) {
            return;
        }
        UsuarioPrincipal me = SecurityUtils.current();
        if (idEmpleado.equals(me.getIdEmpleado())) {
            return;
        }
        if (participoEnCircuito(pasos, me.getIdUsuario())) {
            return;
        }
        throw ApiException.forbidden("No puede consultar solicitudes de otro trabajador");
    }

    private boolean participoEnCircuito(List<SolicitudPasoAprobacion> pasos, Integer idUsuario) {
        if (pasos == null || idUsuario == null) {
            return false;
        }
        return pasos.stream().anyMatch(p ->
                (p.getUsuarioDecision() != null && idUsuario.equals(p.getUsuarioDecision().getIdUsuario()))
                        || (p.getUsuarioAsignado() != null && idUsuario.equals(p.getUsuarioAsignado().getIdUsuario())));
    }

    private void assertEsDuenioORrhh(Integer idEmpleado) {
        if (SecurityUtils.isAdminOrRrhh()) {
            return;
        }
        if (!idEmpleado.equals(SecurityUtils.current().getIdEmpleado())) {
            throw ApiException.forbidden("Solo el solicitante o RR. HH. puede cancelar");
        }
    }
}
