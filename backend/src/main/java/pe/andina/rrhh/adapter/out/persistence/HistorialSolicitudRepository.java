package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.HistorialSolicitudPort;
import pe.andina.rrhh.domain.model.HistorialSolicitud;
import java.util.List;
public interface HistorialSolicitudRepository extends JpaRepository<HistorialSolicitud, Integer>, HistorialSolicitudPort {
    List<HistorialSolicitud> findBySolicitudPermiso_IdSolicitudPermisoOrderByIdHistorialAsc(Integer id);
    List<HistorialSolicitud> findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByIdHistorialAsc(Integer id);
}
