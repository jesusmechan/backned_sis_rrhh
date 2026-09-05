package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.SolicitudPermiso;
import java.util.List;
public interface SolicitudPermisoRepository extends JpaRepository<SolicitudPermiso, Integer> {
    List<SolicitudPermiso> findByEmpleado_IdEmpleadoOrderByIdSolicitudPermisoDesc(Integer idEmpleado);
}
