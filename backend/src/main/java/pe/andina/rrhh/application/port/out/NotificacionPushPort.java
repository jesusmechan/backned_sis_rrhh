package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.application.dto.AppDtos.NotificacionEvento;

public interface NotificacionPushPort {
    void enviarAUsuario(String nombreUsuario, NotificacionEvento evento);
}
