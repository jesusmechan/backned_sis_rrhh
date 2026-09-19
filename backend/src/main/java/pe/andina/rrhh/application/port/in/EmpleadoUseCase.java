package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoRequest;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.model.enums.TipoContrato;
import pe.andina.rrhh.domain.model.enums.TipoDocumento;

public interface EmpleadoUseCase {
    List<EmpleadoResponse> listar();
    EmpleadoResponse obtener(Integer id);
    EmpleadoResponse crear(EmpleadoRequest request);
    EmpleadoResponse actualizar(Integer id, EmpleadoRequest request);
    Empleado buscar(Integer id);
}
