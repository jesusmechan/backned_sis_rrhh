package pe.andina.rrhh.application.port.in;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.EvaluacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.EvaluacionResponse;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.EvaluacionDesempeno;
import pe.andina.rrhh.domain.model.enums.EstadoEvaluacion;

public interface DesempenoUseCase {
    List<EvaluacionResponse> listar();
    EvaluacionResponse obtener(Integer id);
    EvaluacionResponse crear(EvaluacionRequest request);
}
