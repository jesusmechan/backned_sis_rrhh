package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import java.util.List;
public interface SolicitudPasoAprobacionRepository extends JpaRepository<SolicitudPasoAprobacion, Integer> {
    List<SolicitudPasoAprobacion> findBySolicitudPermiso_IdSolicitudPermisoOrderByNumeroPasoAsc(Integer id);
    List<SolicitudPasoAprobacion> findBySolicitudHoraExtra_IdSolicitudHoraExtraOrderByNumeroPasoAsc(Integer id);
    List<SolicitudPasoAprobacion> findByEstado(EstadoPasoAprobacion estado);
}
