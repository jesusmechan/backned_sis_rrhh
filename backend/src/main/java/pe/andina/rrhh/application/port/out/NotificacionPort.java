package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.domain.model.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionPort {
    Optional<Notificacion> findById(Integer id);
    Notificacion save(Notificacion entity);
    List<Notificacion> findByUsuario_IdUsuarioOrderByIdNotificacionDesc(Integer idUsuario);
    long countByUsuario_IdUsuarioAndLeidaFalse(Integer idUsuario);
}
