package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.SolicitudHoraExtra;
import java.util.List;
public interface SolicitudHoraExtraRepository extends JpaRepository<SolicitudHoraExtra, Integer> {
    List<SolicitudHoraExtra> findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(Integer idEmpleado);
}
