package pe.andina.rrhh.application.port.in;

import java.time.LocalDate;
import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.application.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.application.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.application.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.domain.model.enums.FormatoReporte;
import pe.andina.rrhh.domain.model.enums.TipoReporte;

public interface ReporteUseCase {
    List<EmpleadoResponse> trabajadores();
    List<PermisoResponse> permisos();
    List<HoraExtraResponse> horasExtras();
    List<MarcacionResponse> asistencia();
    List<MarcacionResponse> asistencia(Integer idEmpleado, LocalDate desde, LocalDate hasta);
    List<UsuarioResponse> usuarios();
    byte[] exportar(TipoReporte tipo, FormatoReporte formato);
}
