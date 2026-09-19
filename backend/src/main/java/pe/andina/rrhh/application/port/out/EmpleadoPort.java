package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.Empleado;

public interface EmpleadoPort {
    Optional<Empleado> findById(Integer id);
    List<Empleado> findAll();
    Empleado save(Empleado entity);
    void deleteById(Integer id);
    void delete(Empleado entity);
    boolean existsById(Integer id);
    long count();

    Optional<Empleado> findByCodigoEmpleado(String codigo);
    Optional<Empleado> findByNumeroDocumento(String numero);
}
