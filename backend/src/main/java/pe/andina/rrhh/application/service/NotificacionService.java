package pe.andina.rrhh.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pe.andina.rrhh.application.dto.AppDtos.ContadorNotificaciones;
import pe.andina.rrhh.application.dto.AppDtos.NotificacionEvento;
import pe.andina.rrhh.application.dto.AppDtos.NotificacionResponse;
import pe.andina.rrhh.application.port.in.NotificacionUseCase;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.out.NotificacionPort;
import pe.andina.rrhh.application.port.out.NotificacionPushPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Notificacion;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.domain.model.enums.TipoAprobador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificacionService implements NotificacionUseCase {

    private final NotificacionPort notificacionRepository;
    private final NotificacionPushPort pushPort;
    private final UsuarioPort usuarioRepository;
    private final CurrentUserPort currentUser;

    public NotificacionService(NotificacionPort notificacionRepository,
                               NotificacionPushPort pushPort,
                               UsuarioPort usuarioRepository,
                               CurrentUserPort currentUser) {
        this.notificacionRepository = notificacionRepository;
        this.pushPort = pushPort;
        this.usuarioRepository = usuarioRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<NotificacionResponse> listar() {
        return notificacionRepository.findByUsuario_IdUsuarioOrderByIdNotificacionDesc(currentUser.idUsuario())
                .stream()
                .limit(40)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContadorNotificaciones noLeidas() {
        return new ContadorNotificaciones(notificacionRepository.countByUsuario_IdUsuarioAndLeidaFalse(currentUser.idUsuario()));
    }

    @Transactional
    public NotificacionResponse marcarLeida(Integer id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> DomainException.notFound("Notificación no encontrada"));
        if (!currentUser.idUsuario().equals(n.getUsuario().getIdUsuario())) {
            throw DomainException.forbidden("No puede marcar notificaciones de otro usuario");
        }
        n.setLeida(true);
        return toResponse(notificacionRepository.save(n));
    }

    @Transactional
    public void marcarTodas() {
        Integer idUsuario = currentUser.idUsuario();
        for (Notificacion n : notificacionRepository.findByUsuario_IdUsuarioOrderByIdNotificacionDesc(idUsuario)) {
            if (!Boolean.TRUE.equals(n.getLeida())) {
                n.setLeida(true);
                notificacionRepository.save(n);
            }
        }
    }

    @Transactional
    public void avisarPasoEnCurso(SolicitudPasoAprobacion paso, Integer idUsuarioExcluido) {
        if (paso == null) {
            return;
        }
        Contexto ctx = contexto(paso);
        String titulo = "Tiene un paso por aprobar";
        String mensaje = ctx.solicitante + " registró " + ctx.tramite + ". Le corresponde el paso «" + paso.getNombrePaso() + "».";
        if (paso.getNumeroPaso() != null && paso.getNumeroPaso() > 1) {
            mensaje = "Hay un paso siguiente en " + ctx.tramite + " de " + ctx.solicitante
                    + ": «" + paso.getNombrePaso() + "».";
        }
        publicar(destinatarios(paso, idUsuarioExcluido), "BANDEJA", titulo, mensaje,
                "/bandeja/" + paso.getIdPasoSolicitud(), ctx.tipoSolicitud, ctx.idSolicitud, paso.getIdPasoSolicitud());
    }

    @Transactional
    public void avisarResultado(SolicitudPasoAprobacion pasoDecidido, boolean aprobada, Integer idUsuarioExcluido) {
        Contexto ctx = contexto(pasoDecidido);
        Usuario solicitante = usuarioRepository.findByEmpleado_IdEmpleado(ctx.idEmpleado).orElse(null);
        if (solicitante == null || (idUsuarioExcluido != null && idUsuarioExcluido.equals(solicitante.getIdUsuario()))) {
            return;
        }
        String tipo = aprobada ? "APROBADA" : "RECHAZADA";
        String titulo = aprobada ? "Solicitud aprobada" : "Solicitud rechazada";
        String mensaje = aprobada
                ? "Su solicitud de " + ctx.tramite + " fue aprobada."
                : "Su solicitud de " + ctx.tramite + " fue rechazada.";
        String ruta = "HORA_EXTRA".equals(ctx.tipoSolicitud)
                ? "/horas-extras/" + ctx.idSolicitud
                : "/permisos/" + ctx.idSolicitud;
        publicar(List.of(solicitante), tipo, titulo, mensaje, ruta, ctx.tipoSolicitud, ctx.idSolicitud, pasoDecidido.getIdPasoSolicitud());
    }

    private void publicar(List<Usuario> destinatarios, String tipo, String titulo, String mensaje,
                          String ruta, String tipoSolicitud, Integer idSolicitud, Integer idPaso) {
        for (Usuario usuario : destinatarios) {
            if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
                continue;
            }
            Notificacion n = new Notificacion();
            n.setUsuario(usuario);
            n.setTipo(tipo);
            n.setTitulo(titulo);
            n.setMensaje(mensaje);
            n.setRuta(ruta);
            n.setTipoSolicitud(tipoSolicitud);
            n.setIdSolicitud(idSolicitud);
            n.setIdPaso(idPaso);
            n.setLeida(false);
            n.setFechaCreacion(java.time.OffsetDateTime.now());
            Notificacion guardada = notificacionRepository.save(n);
            NotificacionEvento evento = new NotificacionEvento(
                    toResponse(guardada),
                    notificacionRepository.countByUsuario_IdUsuarioAndLeidaFalse(usuario.getIdUsuario()));
            String nombreUsuario = usuario.getNombreUsuario();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        pushPort.enviarAUsuario(nombreUsuario, evento);
                    }
                });
            } else {
                pushPort.enviarAUsuario(nombreUsuario, evento);
            }
        }
    }

    private List<Usuario> destinatarios(SolicitudPasoAprobacion paso, Integer idUsuarioExcluido) {
        Map<Integer, Usuario> unicos = new LinkedHashMap<>();
        if (paso.getTipoAprobador() == TipoAprobador.ROL && paso.getRol() != null) {
            for (Usuario u : usuarioRepository.findByRol_CodigoAndActivoTrue(paso.getRol().getCodigo())) {
                unicos.put(u.getIdUsuario(), u);
            }
            if ("GERENCIA".equals(paso.getRol().getCodigo())) {
                for (Usuario u : usuarioRepository.findByRol_CodigoAndActivoTrue("ADMIN")) {
                    unicos.put(u.getIdUsuario(), u);
                }
            }
        } else if (paso.getUsuarioAsignado() != null) {
            unicos.put(paso.getUsuarioAsignado().getIdUsuario(), paso.getUsuarioAsignado());
        }
        if (idUsuarioExcluido != null) {
            unicos.remove(idUsuarioExcluido);
        }
        return new ArrayList<>(unicos.values());
    }

    private Contexto contexto(SolicitudPasoAprobacion paso) {
        if (paso.getSolicitudPermiso() != null) {
            SolicitudPermiso s = paso.getSolicitudPermiso();
            Empleado e = s.getEmpleado();
            return new Contexto("PERMISO", s.getIdSolicitudPermiso(), e.getIdEmpleado(),
                    e.nombreCompleto(), s.getTipoPermiso().getNombre());
        }
        SolicitudHoraExtra s = paso.getSolicitudHoraExtra();
        Empleado e = s.getEmpleado();
        return new Contexto("HORA_EXTRA", s.getIdSolicitudHoraExtra(), e.getIdEmpleado(),
                e.nombreCompleto(), "horas extras");
    }

    private NotificacionResponse toResponse(Notificacion n) {
        return new NotificacionResponse(
                n.getIdNotificacion(), n.getTipo(), n.getTitulo(), n.getMensaje(), n.getRuta(),
                n.getTipoSolicitud(), n.getIdSolicitud(), n.getIdPaso(), n.getLeida(), n.getFechaCreacion());
    }

    private record Contexto(String tipoSolicitud, Integer idSolicitud, Integer idEmpleado,
                            String solicitante, String tramite) {}
}
