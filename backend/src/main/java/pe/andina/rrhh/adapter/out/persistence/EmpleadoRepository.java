package pe.andina.rrhh.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.EmpleadoPort;
import pe.andina.rrhh.domain.model.Empleado;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer>, EmpleadoPort {
    Optional<Empleado> findByCodigoEmpleado(String codigo);
    Optional<Empleado> findByNumeroDocumento(String numero);
    List<Empleado> findByJefeInmediato_IdEmpleado(Integer idJefe);
    List<Empleado> findByArea_IdArea(Integer idArea);
}
