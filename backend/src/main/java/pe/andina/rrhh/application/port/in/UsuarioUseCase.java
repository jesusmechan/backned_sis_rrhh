package pe.andina.rrhh.application.port.in;

import java.util.List;
import pe.andina.rrhh.application.dto.AppDtos.CambioPasswordRequest;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioRequest;
import pe.andina.rrhh.application.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.domain.model.Usuario;

public interface UsuarioUseCase {
    List<UsuarioResponse> listar();
    UsuarioResponse obtener(Integer id);
    UsuarioResponse yo();
    UsuarioResponse crear(UsuarioRequest request);
    UsuarioResponse actualizar(Integer id, UsuarioRequest request);
    UsuarioResponse cambiarPassword(CambioPasswordRequest request);
    UsuarioResponse cambiarEstado(Integer id, boolean activo);
    Usuario buscar(Integer id);
}
