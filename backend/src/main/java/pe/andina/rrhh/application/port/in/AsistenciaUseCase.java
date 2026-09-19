package pe.andina.rrhh.application.port.in;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionRequest;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.domain.model.Empleado;
import pe.andina.rrhh.domain.model.Marcacion;

public interface AsistenciaUseCase {
    MarcacionResponse marcar(MarcacionRequest request);
    List<MarcacionResponse> listar(Integer idEmpleado, LocalDate desde, LocalDate hasta);
    List<MarcacionResponse> reporte(Integer idEmpleado, LocalDate desde, LocalDate hasta);
    MarcacionResponse obtener(Integer id);
    MarcacionResponse actualizar(Integer id, MarcacionRequest request);
}
