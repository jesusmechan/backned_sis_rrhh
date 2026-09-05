package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Marcacion;
import pe.andina.rrhh.domain.enums.TipoMarcacion;
import java.time.LocalDate;
import java.util.List;
public interface MarcacionRepository extends JpaRepository<Marcacion, Integer> {
    List<Marcacion> findByEmpleado_IdEmpleadoOrderByFechaHoraDesc(Integer idEmpleado);
    List<Marcacion> findByFechaBetweenOrderByFechaHoraDesc(LocalDate desde, LocalDate hasta);
    List<Marcacion> findByEmpleado_IdEmpleadoAndTipoAndFecha(Integer idEmpleado, TipoMarcacion tipo, LocalDate fecha);
}
