package pe.andina.rrhh.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.domain.EvaluacionDesempeno;

import java.util.List;

public interface EvaluacionDesempenoRepository extends JpaRepository<EvaluacionDesempeno, Integer> {
    List<EvaluacionDesempeno> findAllByOrderByFechaDescIdEvaluacionDesc();
    List<EvaluacionDesempeno> findByEmpleado_IdEmpleadoOrderByFechaDesc(Integer idEmpleado);
}
