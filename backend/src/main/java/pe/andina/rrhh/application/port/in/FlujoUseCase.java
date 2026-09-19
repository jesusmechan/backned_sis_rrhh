package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.FlujoPasoRequest;
import pe.andina.rrhh.application.dto.AppDtos.FlujoRequest;
import pe.andina.rrhh.application.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.model.ConfiguracionAprobacionDetalle;
import pe.andina.rrhh.domain.model.enums.TipoAprobador;

public interface FlujoUseCase {
    List<FlujoResponse> listar();
    FlujoResponse obtener(Integer id);
    FlujoResponse crear(FlujoRequest request);
    FlujoResponse actualizar(Integer id, FlujoRequest request);
}
