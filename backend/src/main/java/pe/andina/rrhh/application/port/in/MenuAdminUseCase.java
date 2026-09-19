package pe.andina.rrhh.application.port.in;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pe.andina.rrhh.application.dto.AppDtos.MenuAdminRequest;
import pe.andina.rrhh.application.dto.AppDtos.MenuAdminResponse;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.Rol;

public interface MenuAdminUseCase {
    List<MenuAdminResponse> listar();
    MenuAdminResponse obtener(Integer id);
    MenuAdminResponse crear(MenuAdminRequest request);
    MenuAdminResponse actualizar(Integer id, MenuAdminRequest request);
    void eliminar(Integer id);
}
