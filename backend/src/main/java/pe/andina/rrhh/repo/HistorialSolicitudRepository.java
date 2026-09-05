package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.HistorialSolicitud;
import java.util.List;
public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, Integer> {
    List<HistorialSolicitud> findBySolicitudPermiso_IdSolicitudPermisoOrderByIdHistorialAsc(Integer id);
    List<HistorialSolicitud> findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByIdHistorialAsc(Integer id);
}
