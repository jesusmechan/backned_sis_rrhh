package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.NotificacionPort;
import pe.andina.rrhh.domain.model.Notificacion;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Integer>, NotificacionPort {
    List<Notificacion> findByUsuario_IdUsuarioOrderByIdNotificacionDesc(Integer idUsuario);
    long countByUsuario_IdUsuarioAndLeidaFalse(Integer idUsuario);
}
