package pe.andina.rrhh.application.port.in;

import java.util.List;
import java.util.Map;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.application.dto.AppDtos.IdNombre;
import pe.andina.rrhh.application.dto.AppDtos.PermisoFuncionalItem;

public interface CatalogoUseCase {
    List<IdNombre> areas();
    List<IdNombre> cargos();
    List<IdNombre> horarios();
    List<CatalogoItem> tiposPermiso();
    List<CatalogoItem> roles();
    List<PermisoFuncionalItem> permisosFuncionales();
    List<Map<String, String>> parametros();
}
