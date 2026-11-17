package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoArchivo;
import pe.andina.rrhh.application.dto.AppDtos.AdjuntoResponse;
import pe.andina.rrhh.application.port.in.AdjuntoUseCase;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.out.SolicitudAdjuntoPort;
import pe.andina.rrhh.application.port.out.SolicitudPasoAprobacionPort;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.SolicitudAdjunto;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.enums.EstadoSolicitud;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AdjuntoService implements AdjuntoUseCase {

    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final Set<String> CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final SolicitudAdjuntoPort adjuntoRepository;
    private final SolicitudPermisoPort permisoRepository;
    private final SolicitudPasoAprobacionPort pasoRepository;
    private final EmpleadoScope empleadoScope;
    private final AuditoriaUseCase auditoriaService;
    private final CurrentUserPort currentUser;

    public AdjuntoService(SolicitudAdjuntoPort adjuntoRepository,
                          SolicitudPermisoPort permisoRepository,
                          SolicitudPasoAprobacionPort pasoRepository,
                          EmpleadoScope empleadoScope,
                          AuditoriaUseCase auditoriaService,
                          CurrentUserPort currentUser) {
        this.adjuntoRepository = adjuntoRepository;
        this.permisoRepository = permisoRepository;
        this.pasoRepository = pasoRepository;
        this.empleadoScope = empleadoScope;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdjuntoResponse> listarDePermiso(Integer idSolicitudPermiso) {
        SolicitudPermiso s = exigirPermisoVisible(idSolicitudPermiso);
        return adjuntoRepository.findBySolicitudPermiso_IdSolicitudPermisoOrderByIdAdjuntoAsc(s.getIdSolicitudPermiso())
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public AdjuntoResponse subirAPermiso(Integer idSolicitudPermiso, MultipartFile archivo, Integer idPaso) {
        SolicitudPermiso s = exigirPermisoEditable(idSolicitudPermiso);
        if (archivo == null || archivo.isEmpty()) {
            throw DomainException.badRequest("Seleccione un archivo de sustento");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw DomainException.badRequest("El archivo no puede superar 5 MB");
        }
        String contentType = normalizarContentType(archivo.getContentType());
        if (!CONTENT_TYPES.contains(contentType)) {
            throw DomainException.badRequest("Solo se admiten PDF, JPG, PNG, WEBP o Word");
        }
        String nombre = sanitizarNombre(archivo.getOriginalFilename());

        SolicitudPasoAprobacion paso = null;
        if (idPaso != null) {
            paso = pasoRepository.findById(idPaso)
                    .orElseThrow(() -> DomainException.badRequest("Paso no encontrado"));
            if (paso.getSolicitudPermiso() == null
                    || !s.getIdSolicitudPermiso().equals(paso.getSolicitudPermiso().getIdSolicitudPermiso())) {
                throw DomainException.badRequest("El paso no pertenece a esta solicitud");
            }
        }

        byte[] bytes;
        try {
            bytes = archivo.getBytes();
        } catch (IOException e) {
            throw DomainException.badRequest("No se pudo leer el archivo");
        }

        SolicitudAdjunto adj = new SolicitudAdjunto();
        adj.setSolicitudPermiso(s);
        adj.setPaso(paso);
        adj.setNombreOriginal(nombre);
        adj.setContentType(contentType);
        adj.setTamanioBytes(bytes.length);
        adj.setContenido(bytes);
        adj.setUsuarioCarga(currentUser.usuario());
        adjuntoRepository.save(adj);

        auditoriaService.registrar(currentUser.usuario(), "ADJUNTAR", "PERMISO",
                s.getIdSolicitudPermiso(), nombre);
        return toDto(adj);
    }

    @Transactional(readOnly = true)
    @Override
    public AdjuntoArchivo descargarDePermiso(Integer idSolicitudPermiso, Integer idAdjunto) {
        exigirPermisoVisible(idSolicitudPermiso);
        SolicitudAdjunto adj = exigirAdjuntoDePermiso(idSolicitudPermiso, idAdjunto);
        return new AdjuntoArchivo(adj.getNombreOriginal(), adj.getContentType(), adj.getContenido());
    }

    @Transactional
    @Override
    public void eliminarDePermiso(Integer idSolicitudPermiso, Integer idAdjunto) {
        exigirPermisoEditable(idSolicitudPermiso);
        SolicitudAdjunto adj = exigirAdjuntoDePermiso(idSolicitudPermiso, idAdjunto);
        adjuntoRepository.delete(adj);
        auditoriaService.registrar(currentUser.usuario(), "ELIMINAR_ADJUNTO", "PERMISO",
                idSolicitudPermiso, adj.getNombreOriginal());
    }

    private SolicitudPermiso exigirPermisoVisible(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        Integer idUsuario = currentUser.idUsuario();
        boolean participo = pasoRepository
                .findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(id).stream()
                .anyMatch(p ->
                        (p.getUsuarioDecision() != null && idUsuario.equals(p.getUsuarioDecision().getIdUsuario()))
                                || (p.getUsuarioAsignado() != null && idUsuario.equals(p.getUsuarioAsignado().getIdUsuario()))
                                || (p.getRol() != null && currentUser.codigoRol() != null
                                && p.getRol().getCodigo().equals(currentUser.codigoRol())));
        empleadoScope.assertPuedeConsultar(s.getEmpleado().getIdEmpleado(), participo);
        return s;
    }

    private SolicitudPermiso exigirPermisoEditable(Integer id) {
        SolicitudPermiso s = permisoRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("Solicitud no encontrada"));
        empleadoScope.assertDuenioORrhh(s.getEmpleado().getIdEmpleado());
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw DomainException.badRequest("Solo se pueden adjuntar archivos a solicitudes pendientes");
        }
        return s;
    }

    private SolicitudAdjunto exigirAdjuntoDePermiso(Integer idSolicitud, Integer idAdjunto) {
        SolicitudAdjunto adj = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> DomainException.notFound("Adjunto no encontrado"));
        if (adj.getSolicitudPermiso() == null
                || !idSolicitud.equals(adj.getSolicitudPermiso().getIdSolicitudPermiso())) {
            throw DomainException.notFound("Adjunto no encontrado");
        }
        return adj;
    }

    private AdjuntoResponse toDto(SolicitudAdjunto a) {
        return new AdjuntoResponse(
                a.getIdAdjunto(),
                a.getPaso() != null ? a.getPaso().getIdPasoSolicitud() : null,
                a.getNombreOriginal(),
                a.getContentType(),
                a.getTamanioBytes(),
                a.getUsuarioCarga() != null ? a.getUsuarioCarga().getNombreUsuario() : null,
                a.getFechaCarga()
        );
    }

    private static String normalizarContentType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "application/octet-stream";
        }
        return raw.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizarNombre(String original) {
        if (original == null || original.isBlank()) {
            return "sustento.bin";
        }
        String nombre = original.replace('\\', '/');
        int slash = nombre.lastIndexOf('/');
        if (slash >= 0) {
            nombre = nombre.substring(slash + 1);
        }
        nombre = nombre.trim();
        if (nombre.isEmpty() || nombre.length() > 255) {
            return "sustento.bin";
        }
        return nombre;
    }
}
