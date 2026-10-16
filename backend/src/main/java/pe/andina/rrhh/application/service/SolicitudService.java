package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.NotificacionUseCase;
import pe.andina.rrhh.application.port.in.SolicitudUseCase;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.AdjuntoSolicitud;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacionDetalle;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.domain.model.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.model.enums.TipoOrigenFlujo;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoEntrada;
import pe.andina.rrhh.application.dto.AppDtos.ArchivoDescarga;
import pe.andina.rrhh.application.dto.AppDtos.BandejaItem;
import pe.andina.rrhh.application.dto.AppDtos.DecisionRequest;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraRequest;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoRequest;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.dto.AppDtos.RequisitoAdjuntoPaso;
import pe.andina.rrhh.application.dto.AppDtos.RequisitosAdjuntoResponse;
import pe.andina.rrhh.application.port.out.AdjuntoSolicitudPort;
import pe.andina.rrhh.application.port.out.AlmacenArchivosPort;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionDetallePort;
import pe.andina.rrhh.application.port.out.ConfiguracionAprobacionPort;
import pe.andina.rrhh.application.port.out.HistorialSolicitudPort;
import pe.andina.rrhh.application.port.out.ParametroPort;
import pe.andina.rrhh.application.port.out.SolicitudHoraExtraPort;
import pe.andina.rrhh.application.port.out.SolicitudPasoAprobacionPort;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;
import pe.andina.rrhh.application.port.out.TipoPermisoPort;
import pe.andina.rrhh.domain.service.BandejaAsignacion;
import pe.andina.rrhh.domain.service.HoraExtraReglas;
import pe.andina.rrhh.domain.service.PermisoReglas;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SolicitudService implements SolicitudUseCase {

    private static final int MAX_ARCHIVOS_POR_ACTIVIDAD = 3;
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "pdf", "application/pdf",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp"
    );

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
    private final ParametroPort parametros;
    private final ConfiguracionAprobacionPort flujoRepository;
    private final ConfiguracionAprobacionDetallePort detalleRepository;
    private final AdjuntoSolicitudPort adjuntoRepository;
    private final AlmacenArchivosPort almacen;

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
                            CurrentUserPort currentUser,
                            ParametroPort parametros,
                            ConfiguracionAprobacionPort flujoRepository,
                            ConfiguracionAprobacionDetallePort detalleRepository,
                            AdjuntoSolicitudPort adjuntoRepository,
                            AlmacenArchivosPort almacen) {
        this.parametros = parametros;
        this.flujoRepository = flujoRepository;
        this.detalleRepository = detalleRepository;
        this.adjuntoRepository = adjuntoRepository;
        this.almacen = almacen;
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

    @Transactional(readOnly = true)
    public RequisitosAdjuntoResponse requisitosAdjunto(Integer idTipoPermiso) {
        if (idTipoPermiso == null) {
            throw DomainException.badRequest("Seleccione el tipo de permiso");
        }
        tipoPermisoRepository.findById(idTipoPermiso)
                .orElseThrow(() -> DomainException.badRequest("Tipo de permiso no existe"));
        ConfiguracionAprobacion flujo = resolverFlujoPermiso(idTipoPermiso);
        List<RequisitoAdjuntoPaso> pasos = actividades(flujo).stream()
                .map(d -> new RequisitoAdjuntoPaso(
                        d.getNumeroPaso(),
                        d.getNombrePaso(),
                        Boolean.TRUE.equals(d.getRequiereAdjunto())))
                .toList();
        return new RequisitosAdjuntoResponse(flujo.getNombre(), pasos);
    }

    @Transactional
    public PermisoResponse crearPermiso(PermisoRequest request, List<AdjuntoEntrada> adjuntos) {
        List<AdjuntoEntrada> archivos = adjuntos == null ? List.of() : adjuntos;
        Empleado empleado = empleadoScope.resolverSolicitante(request.idEmpleado());
        var tipo = tipoPermisoRepository.findById(request.idTipoPermiso())
                .orElseThrow(() -> DomainException.badRequest("Tipo de permiso no existe"));
        permisoReglas.validarAlCrear(empleado, tipo, request);
        validarAdjuntos(actividades(resolverFlujoPermiso(request.idTipoPermiso())), archivos);
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
        List<SolicitudPasoAprobacion> pasos = pasoRepository
                .findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso());
        guardarAdjuntos(s, pasos, archivos);
        auditoriaService.registrar(currentUser.usuario(), "REGISTRAR", "PERMISO", s.getIdSolicitudPermiso(), s.getMotivo());
        notificarPasoEnCurso(pasos);
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
        List<SolicitudPermiso> data = currentUser.veConjuntoOperativo()
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
        List<SolicitudHoraExtra> data = currentUser.veConjuntoOperativo()
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
        int minRechazo = parametros.entero("comentario_rechazo_min_caracteres");
        if (!aprobar && request.comentario().trim().length() < minRechazo) {
            throw DomainException.badRequest("Al rechazar, el comentario debe tener al menos " + minRechazo + " caracteres");
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
            throw DomainException.badRequest("Este paso ya no estÃ¡ pendiente de decisiÃ³n");
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

    @Transactional(readOnly = true)
    public ArchivoDescarga descargarAdjunto(Integer idSolicitud, Integer idAdjunto) {
        obtenerPermiso(idSolicitud);
        AdjuntoSolicitud adjunto = adjuntoRepository
                .findByIdAdjuntoAndSolicitudPermiso_IdSolicitudPermiso(idAdjunto, idSolicitud)
                .orElseThrow(() -> DomainException.notFound("Archivo no encontrado"));
        return new ArchivoDescarga(adjunto.getNombreOriginal(), adjunto.getTipoContenido(), almacen.leer(adjunto.getClaveAlmacen()));
    }

    private PermisoResponse toPermiso(SolicitudPermiso s) {
        return DtoMapper.permiso(
                s,
                pasoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(s.getIdSolicitudPermiso()),
                adjuntoRepository.findBySolicitudPermisoId(s.getIdSolicitudPermiso()));
    }

    private ConfiguracionAprobacion resolverFlujoPermiso(Integer idTipoPermiso) {
        return flujoRepository.findActivaPorTipoPermiso(TipoOrigenFlujo.PERMISO, idTipoPermiso)
                .or(() -> flujoRepository.findActivaGenerica(TipoOrigenFlujo.PERMISO))
                .orElseThrow(() -> DomainException.badRequest("No existe un flujo de aprobación activo para este permiso"));
    }

    private List<ConfiguracionAprobacionDetalle> actividades(ConfiguracionAprobacion flujo) {
        return detalleRepository.findByConfiguracion_IdConfiguracionOrderByNumeroPasoAsc(flujo.getIdConfiguracion())
                .stream()
                .filter(d -> Boolean.TRUE.equals(d.getActivo()))
                .toList();
    }

    private void validarAdjuntos(List<ConfiguracionAprobacionDetalle> actividades, List<AdjuntoEntrada> archivos) {
        Map<Integer, ConfiguracionAprobacionDetalle> porNumero = new HashMap<>();
        for (ConfiguracionAprobacionDetalle actividad : actividades) {
            porNumero.put(actividad.getNumeroPaso(), actividad);
        }
        Map<Integer, Integer> cantidades = new HashMap<>();
        for (AdjuntoEntrada archivo : archivos) {
            ConfiguracionAprobacionDetalle actividad = porNumero.get(archivo.numeroPaso());
            if (actividad == null || !Boolean.TRUE.equals(actividad.getRequiereAdjunto())) {
                throw DomainException.badRequest("El archivo no corresponde a una actividad que exija sustento");
            }
            validarArchivo(archivo, actividad.getNombrePaso());
            cantidades.merge(archivo.numeroPaso(), 1, Integer::sum);
        }
        for (ConfiguracionAprobacionDetalle actividad : actividades) {
            if (!Boolean.TRUE.equals(actividad.getRequiereAdjunto())) {
                continue;
            }
            int cantidad = cantidades.getOrDefault(actividad.getNumeroPaso(), 0);
            if (cantidad == 0) {
                throw DomainException.badRequest("La actividad \"" + actividad.getNombrePaso() + "\" requiere un archivo de sustentación");
            }
            if (cantidad > MAX_ARCHIVOS_POR_ACTIVIDAD) {
                throw DomainException.badRequest("La actividad \"" + actividad.getNombrePaso() + "\" admite como máximo "
                        + MAX_ARCHIVOS_POR_ACTIVIDAD + " archivos");
            }
        }
    }

    private void validarArchivo(AdjuntoEntrada archivo, String actividad) {
        byte[] contenido = archivo.contenido();
        if (contenido == null || contenido.length == 0) {
            throw DomainException.badRequest("El archivo de \"" + actividad + "\" está vacío");
        }
        if (contenido.length > MAX_BYTES) {
            throw DomainException.badRequest("El archivo de \"" + actividad + "\" supera los 5 MB");
        }
        String extension = extension(archivo.nombreOriginal());
        String esperado = TIPOS_PERMITIDOS.get(extension);
        if (esperado == null) {
            throw DomainException.badRequest("La actividad \"" + actividad + "\" solo admite PDF, PNG, JPG o WEBP");
        }
        String declarado = archivo.tipoContenido() == null ? "" : archivo.tipoContenido().toLowerCase(Locale.ROOT).trim();
        int separador = declarado.indexOf(';');
        if (separador >= 0) {
            declarado = declarado.substring(0, separador).trim();
        }
        if (!declarado.isBlank()
                && !declarado.equals("application/octet-stream")
                && !declarado.equals(esperado)
                && !(extension.equals("jpg") && declarado.equals("image/jpg"))) {
            throw DomainException.badRequest("El tipo del archivo de \"" + actividad + "\" no coincide con su extensión");
        }
    }

    private void guardarAdjuntos(SolicitudPermiso solicitud, List<SolicitudPasoAprobacion> pasos, List<AdjuntoEntrada> archivos) {
        if (archivos.isEmpty()) {
            return;
        }
        Map<Integer, SolicitudPasoAprobacion> porNumero = new HashMap<>();
        for (SolicitudPasoAprobacion paso : pasos) {
            porNumero.put(paso.getNumeroPaso(), paso);
        }
        List<String> claves = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        claves.forEach(almacen::eliminar);
                    }
                }
            });
        }
        for (AdjuntoEntrada archivo : archivos) {
            SolicitudPasoAprobacion paso = porNumero.get(archivo.numeroPaso());
            if (paso == null) {
                throw DomainException.badRequest("No se encontró la actividad del sustento");
            }
            String extension = extension(archivo.nombreOriginal());
            String clave = almacen.guardar("permisos/" + solicitud.getIdSolicitudPermiso(), archivo.nombreOriginal(), archivo.contenido());
            claves.add(clave);
            AdjuntoSolicitud adjunto = new AdjuntoSolicitud();
            adjunto.setSolicitudPermiso(solicitud);
            adjunto.setPaso(paso);
            adjunto.setNombreOriginal(nombreVisible(archivo.nombreOriginal()));
            adjunto.setTipoContenido(TIPOS_PERMITIDOS.get(extension));
            adjunto.setTamanoBytes((long) archivo.contenido().length);
            adjunto.setClaveAlmacen(clave);
            adjuntoRepository.save(adjunto);
        }
    }

    private static String extension(String nombre) {
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        if (punto < 0 || punto == nombre.length() - 1) {
            return "";
        }
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private static String nombreVisible(String nombre) {
        String base = nombre == null ? "archivo" : nombre.trim();
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        if (base.isBlank()) {
            return "archivo";
        }
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
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
