package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.SolicitudHoraExtraPort;
import pe.andina.rrhh.domain.model.SolicitudHoraExtra;
import java.util.List;
public interface SolicitudHoraExtraRepository extends JpaRepository<SolicitudHoraExtra, Integer>, SolicitudHoraExtraPort {
    List<SolicitudHoraExtra> findByEmpleado_IdEmpleadoOrderByIdSolicitudHoraExtraDesc(Integer idEmpleado);
}
