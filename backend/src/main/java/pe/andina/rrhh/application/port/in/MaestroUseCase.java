package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.AreaRequest;
import pe.andina.rrhh.application.dto.AppDtos.AreaResponse;
import pe.andina.rrhh.application.dto.AppDtos.CargoRequest;
import pe.andina.rrhh.application.dto.AppDtos.CargoResponse;
import pe.andina.rrhh.application.dto.AppDtos.CuentaRequest;
import pe.andina.rrhh.application.dto.AppDtos.CuentaResponse;
import pe.andina.rrhh.application.dto.AppDtos.HorarioRequest;
import pe.andina.rrhh.application.dto.AppDtos.HorarioResponse;
import pe.andina.rrhh.application.dto.AppDtos.ParametroRequest;
import pe.andina.rrhh.application.dto.AppDtos.ParametroResponse;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoMaestroResponse;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoRequest;

public interface MaestroUseCase {
    List<AreaResponse> areas();
    AreaResponse crearArea(AreaRequest r);
    AreaResponse actualizarArea(Integer id, AreaRequest r);
    List<CargoResponse> cargos();
    CargoResponse crearCargo(CargoRequest r);
    CargoResponse actualizarCargo(Integer id, CargoRequest r);
    List<HorarioResponse> horarios();
    HorarioResponse crearHorario(HorarioRequest r);
    HorarioResponse actualizarHorario(Integer id, HorarioRequest r);
    List<TipoPermisoMaestroResponse> tiposPermiso();
    TipoPermisoMaestroResponse crearTipoPermiso(TipoPermisoRequest r);
    TipoPermisoMaestroResponse actualizarTipoPermiso(Integer id, TipoPermisoRequest r);
    List<ParametroResponse> parametros();
    ParametroResponse crearParametro(ParametroRequest r);
    ParametroResponse actualizarParametro(String clave, ParametroRequest r);
    List<CuentaResponse> cuentas();
    CuentaResponse crearCuenta(CuentaRequest r);
    CuentaResponse actualizarCuenta(Integer id, CuentaRequest r);
}
