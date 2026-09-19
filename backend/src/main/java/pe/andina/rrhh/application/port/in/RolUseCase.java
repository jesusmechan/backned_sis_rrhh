package pe.andina.rrhh.application.port.in;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pe.andina.rrhh.application.dto.AppDtos.RolRequest;
import pe.andina.rrhh.application.dto.AppDtos.RolResponse;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.PermisoFuncional;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.domain.model.RolPermiso;

public interface RolUseCase {
    List<RolResponse> listar();
    RolResponse obtener(Integer id);
    RolResponse crear(RolRequest request);
    RolResponse actualizar(Integer id, RolRequest request);
}
