package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.MarcacionPort;
import pe.andina.rrhh.domain.model.Marcacion;
import pe.andina.rrhh.domain.model.enums.TipoMarcacion;
import java.time.LocalDate;
import java.util.List;
public interface MarcacionRepository extends JpaRepository<Marcacion, Integer>, MarcacionPort {
    List<Marcacion> findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(Integer idEmpleado);
    List<Marcacion> findByFechaBetweenOrderByFechaHoraDesc(LocalDate desde, LocalDate hasta);
    List<Marcacion> findByEmpleado_IdEmpleadoAndTipoAndFecha(Integer idEmpleado, TipoMarcacion tipo, LocalDate fecha);
}
