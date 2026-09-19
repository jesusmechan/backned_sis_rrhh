package pe.andina.rrhh.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.domain.model.Empleado;
import java.util.Optional;
public interface EmpleadoRepository extends JpaRepository<Empleado, Integer>, EmpleadoPort {
    Optional<Empleado> findByCodigoEmpleado(String codigo);
    Optional<Empleado> findByNumeroDocumento(String numero);
}
