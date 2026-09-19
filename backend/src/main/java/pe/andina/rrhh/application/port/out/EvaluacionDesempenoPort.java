package pe.andina.rrhh.application.port.out;

import java.util.List;
import java.util.Optional;
import pe.andina.rrhh.domain.model.EvaluacionDesempeno;

public interface EvaluacionDesempenoPort {
    Optional<EvaluacionDesempeno> findById(Integer id);
    List<EvaluacionDesempeno> findAll();
    EvaluacionDesempeno save(EvaluacionDesempeno entity);
    void deleteById(Integer id);
    void delete(EvaluacionDesempeno entity);
    boolean existsById(Integer id);
    long count();

    List<EvaluacionDesempeno> findAllByOrderByFechaDescIdEvaluacionDesc();
    List<EvaluacionDesempeno> findByEmpleado_IdEmpleadoOrderByFechaDesc(Integer idEmpleado);
}
