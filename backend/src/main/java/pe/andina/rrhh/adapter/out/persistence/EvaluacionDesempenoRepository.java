package pe.andina.rrhh.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.andina.rrhh.application.port.out.EvaluacionDesempenoPort;
import pe.andina.rrhh.domain.model.EvaluacionDesempeno;

import java.util.List;

public interface EvaluacionDesempenoRepository extends JpaRepository<EvaluacionDesempeno, Integer>, EvaluacionDesempenoPort {
    List<EvaluacionDesempeno> findAllByOrderByFechaDescIdEvaluacionDesc();
    List<EvaluacionDesempeno> findByEmpleado_IdEmpleadoOrderByFechaDesc(Integer idEmpleado);
}
