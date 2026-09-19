package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.SolicitudPermisoPort;
import pe.andina.rrhh.domain.model.SolicitudPermiso;
import java.util.List;
public interface SolicitudPermisoRepository extends JpaRepository<SolicitudPermiso, Integer>, SolicitudPermisoPort {
    List<SolicitudPermiso> findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(Integer idEmpleado);
}
