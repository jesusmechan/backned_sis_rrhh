package pe.andina.rrhh.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.Empleado;
import java.util.Optional;
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer> {
    Optional<Empleado> findByCodigoEmpleado(String codigo);
    Optional<Empleado> findByNumeroDocumento(String numero);
}
