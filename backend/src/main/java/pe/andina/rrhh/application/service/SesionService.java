package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.SesionUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.Usuario;
import pe.andina.rrhh.application.dto.AppDtos.MenuGrupoResponse;
import pe.andina.rrhh.application.dto.AppDtos.MenuItemResponse;
import pe.andina.rrhh.application.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.application.port.out.MenuItemPort;
import pe.andina.rrhh.application.port.out.RolPermisoPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SesionService implements SesionUseCase {

    private final UsuarioPort usuarioRepository;
    private final MenuItemPort menuItemRepository;
    private final RolPermisoPort rolPermisoRepository;

    private final CurrentUserPort currentUser;

    public SesionService(UsuarioPort usuarioRepository,
                         MenuItemPort menuItemRepository,
                         RolPermisoPort rolPermisoRepository,
                           CurrentUserPort currentUser) {
        this.usuarioRepository = usuarioRepository;
        this.menuItemRepository = menuItemRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public SesionResponse actual() {
        Usuario usuario = usuarioRepository.findById(currentUser.idUsuario())
                .orElseThrow();
        usuario.getRol().getNombre();
        if (usuario.getEmpleado() != null) {
            usuario.getEmpleado().nombreCompleto();
        }
        return desde(usuario);
    }

    @Transactional(readOnly = true)
    public SesionResponse desde(Usuario usuario) {
        Integer idRol = usuario.getRol().getIdRol();
        List<String> permisos = rolPermisoRepository.findByRolId(idRol).stream()
                .map(rp -> rp.getPermiso().getCodigo())
                .sorted()
                .toList();
        return new SesionResponse(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getCorreo(),
                usuario.getRol().getCodigo(),
                usuario.getRol().getNombre(),
                idRol,
                usuario.getEmpleado() != null ? usuario.getEmpleado().getIdEmpleado() : null,
                usuario.getEmpleado() != null ? usuario.getEmpleado().nombreCompleto() : usuario.getNombreUsuario(),
                permisos,
                agrupar(menuItemRepository.findActivosByPerfil(idRol))
        );
    }

    private List<MenuGrupoResponse> agrupar(List<MenuItem> items) {
        Map<String, List<MenuItemResponse>> grupos = new LinkedHashMap<>();
        for (MenuItem item : items) {
            grupos.computeIfAbsent(item.getGrupo(), key -> new ArrayList<>())
                    .add(new MenuItemResponse(
                            item.getCodigo(),
                            item.getEtiqueta(),
                            item.getRuta(),
                            item.getIcono(),
                            item.getDescripcion()
                    ));
        }
        return grupos.entrySet().stream()
                .map(entry -> new MenuGrupoResponse(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
