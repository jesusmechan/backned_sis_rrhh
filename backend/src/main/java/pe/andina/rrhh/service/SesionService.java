package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.MenuItem;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.dto.AppDtos.MenuGrupoResponse;
import pe.andina.rrhh.dto.AppDtos.MenuItemResponse;
import pe.andina.rrhh.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.repo.MenuItemRepository;
import pe.andina.rrhh.repo.RolPermisoRepository;
import pe.andina.rrhh.repo.UsuarioRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SesionService {

    private final UsuarioRepository usuarioRepository;
    private final MenuItemRepository menuItemRepository;
    private final RolPermisoRepository rolPermisoRepository;

    public SesionService(UsuarioRepository usuarioRepository,
                         MenuItemRepository menuItemRepository,
                         RolPermisoRepository rolPermisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.menuItemRepository = menuItemRepository;
        this.rolPermisoRepository = rolPermisoRepository;
    }

    @Transactional(readOnly = true)
    public SesionResponse actual() {
        Usuario usuario = usuarioRepository.findById(SecurityUtils.current().getIdUsuario())
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
