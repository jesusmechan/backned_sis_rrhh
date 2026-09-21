package pe.andina.rrhh.adapter.out.ws;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.application.dto.AppDtos.NotificacionEvento;
import pe.andina.rrhh.application.port.out.NotificacionPushPort;

@Component
public class StompNotificacionAdapter implements NotificacionPushPort {

    private final SimpMessagingTemplate messagingTemplate;

    public StompNotificacionAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void enviarAUsuario(String nombreUsuario, NotificacionEvento evento) {
        if (nombreUsuario == null || nombreUsuario.isBlank()) {
            return;
        }
        messagingTemplate.convertAndSendToUser(nombreUsuario, "/queue/notificaciones", evento);
    }
}
