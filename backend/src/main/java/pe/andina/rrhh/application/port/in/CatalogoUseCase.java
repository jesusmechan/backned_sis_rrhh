package pe.andina.rrhh.application.port.in;

import java.util.List;
import java.util.Map;
import pe.andina.rrhh.application.dto.AppDtos.AfpItem;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoItem;
import pe.andina.rrhh.application.dto.AppDtos.CatalogoValorItem;
import pe.andina.rrhh.application.dto.AppDtos.IdNombre;
import pe.andina.rrhh.application.dto.AppDtos.PermisoFuncionalItem;
import pe.andina.rrhh.application.dto.AppDtos.RegimenLaboralItem;
import pe.andina.rrhh.application.dto.AppDtos.TipoPermisoItem;

public interface CatalogoUseCase {
    List<IdNombre> areas();
    List<IdNombre> cargos();
    List<IdNombre> horarios();
    List<TipoPermisoItem> tiposPermiso();
    List<CatalogoItem> roles();
    List<PermisoFuncionalItem> permisosFuncionales();
    List<Map<String, String>> parametros();
    Map<String, String> configuracionPublica();
    Map<String, List<CatalogoValorItem>> valores();
    List<AfpItem> afps();
    List<RegimenLaboralItem> regimenesLaborales();
}
