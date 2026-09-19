package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.application.dto.AppDtos.HistorialResponse;

public interface ConsultaUseCase {
    List<AuditoriaResponse> auditoria();
    List<HistorialResponse> trazabilidad();
}
