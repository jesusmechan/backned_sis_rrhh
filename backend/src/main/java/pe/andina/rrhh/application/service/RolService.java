package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.RolUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.PermisoFuncional;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.domain.model.RolPermiso;
import pe.andina.rrhh.application.dto.AppDtos.RolRequest;
import pe.andina.rrhh.application.dto.AppDtos.RolResponse;
import pe.andina.rrhh.application.port.out.MenuItemPort;
import pe.andina.rrhh.application.port.out.PermisoFuncionalPort;
import pe.andina.rrhh.application.port.out.RolPermisoPort;
import pe.andina.rrhh.application.port.out.RolPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolService implements RolUseCase {

    private final RolPort rolRepository;
    private final UsuarioPort usuarioRepository;
    private final MenuItemPort menuItemRepository;
    private final PermisoFuncionalPort permisoRepository;
    private final RolPermisoPort rolPermisoRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public RolService(RolPort rolRepository,
                      UsuarioPort usuarioRepository,
                      MenuItemPort menuItemRepository,
                      PermisoFuncionalPort permisoRepository,
                      RolPermisoPort rolPermisoRepository,
                      AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.menuItemRepository = menuItemRepository;
        this.permisoRepository = permisoRepository;
        this.rolPermisoRepository = rolPermisoRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listar() {
        return rolRepository.findAllByOrderByNombreAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RolResponse obtener(Integer id) {
        return toDto(buscar(id));
    }

    @Transactional
    public RolResponse crear(RolRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (rolRepository.existsByCodigoIgnoreCase(codigo)) {
            throw DomainException.conflict("Ya existe un rol con ese código");
        }
        Rol rol = new Rol();
        rol.setCodigo(codigo);
        aplicar(rol, request, true);
        rolRepository.save(rol);
        sincronizar(rol, request);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "ROL", rol.getIdRol(), rol.getCodigo());
        return toDto(rol);
    }

    @Transactional
    public RolResponse actualizar(Integer id, RolRequest request) {
        Rol rol = buscar(id);
        String codigo = normalizarCodigo(request.codigo());
        if (esAdministrador(rol) && !codigo.equals(rol.getCodigo())) {
            throw DomainException.badRequest("El código del rol Administrador no se puede cambiar");
        }
        if (rolRepository.existsByCodigoIgnoreCaseAndIdRolNot(codigo, id)) {
            throw DomainException.conflict("Ya existe un rol con ese código");
        }
        if (esAdministrador(rol) && Boolean.FALSE.equals(request.activo())) {
            throw DomainException.badRequest("El rol Administrador no se puede desactivar");
        }
        if (Boolean.FALSE.equals(request.activo()) && usuarioRepository.countByRol_IdRol(id) > 0) {
            throw DomainException.badRequest("No se puede desactivar un rol con cuentas asignadas");
        }
        rol.setCodigo(codigo);
        aplicar(rol, request, false);
        sincronizar(rol, request);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "ROL", id, rol.getCodigo());
        return toDto(rol);
    }

    private void aplicar(Rol rol, RolRequest request, boolean nuevo) {
        rol.setNombre(request.nombre().trim());
        rol.setDescripcion(request.descripcion() != null && !request.descripcion().isBlank()
                ? request.descripcion().trim() : null);
        rol.setActivo(request.activo() == null || request.activo());
        if (nuevo && rol.getActivo() == null) {
            rol.setActivo(true);
        }
    }

    private void sincronizar(Rol rol, RolRequest request) {
        Set<Integer> idMenus = new HashSet<>(request.idMenus() == null ? List.of() : request.idMenus());
        if (esAdministrador(rol)) {
            for (MenuItem m : menuItemRepository.findAll()) {
                if ("MENU".equalsIgnoreCase(m.getCodigo()) || "ROLES".equalsIgnoreCase(m.getCodigo())
                        || "/menu".equals(m.getRuta()) || "/roles".equals(m.getRuta())) {
                    idMenus.add(m.getIdMenu());
                }
            }
        }
        for (MenuItem menu : menuItemRepository.findAllWithPerfiles()) {
            boolean asignado = idMenus.contains(menu.getIdMenu());
            boolean tiene = menu.getPerfiles().stream().anyMatch(r -> r.getIdRol().equals(rol.getIdRol()));
            if (asignado && !tiene) {
                menu.getPerfiles().add(rol);
            } else if (!asignado && tiene) {
                menu.getPerfiles().removeIf(r -> r.getIdRol().equals(rol.getIdRol()));
            }
        }

        rolPermisoRepository.deleteByRol_IdRol(rol.getIdRol());
        rolPermisoRepository.flush();
        Set<Integer> idPermisos = new HashSet<>(request.idPermisos() == null ? List.of() : request.idPermisos());
        for (Integer idPermiso : idPermisos) {
            PermisoFuncional permiso = permisoRepository.findById(idPermiso)
                    .orElseThrow(() -> DomainException.badRequest("Permiso funcional no existe"));
            RolPermiso rp = new RolPermiso();
            rp.setRol(rol);
            rp.setPermiso(permiso);
            rolPermisoRepository.save(rp);
        }
    }

    private Rol buscar(Integer id) {
        return rolRepository.findById(id).orElseThrow(() -> DomainException.notFound("Rol no encontrado"));
    }

    private RolResponse toDto(Rol rol) {
        List<MenuItem> menus = menuItemRepository.findAllWithPerfiles().stream()
                .filter(m -> m.getPerfiles().stream().anyMatch(r -> r.getIdRol().equals(rol.getIdRol())))
                .toList();
        List<RolPermiso> permisos = rolPermisoRepository.findByRolId(rol.getIdRol());
        return new RolResponse(
                rol.getIdRol(),
                rol.getCodigo(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.getActivo(),
                (int) usuarioRepository.countByRol_IdRol(rol.getIdRol()),
                menus.stream().map(MenuItem::getIdMenu).toList(),
                menus.stream().map(MenuItem::getEtiqueta).toList(),
                permisos.stream().map(rp -> rp.getPermiso().getIdPermiso()).toList(),
                permisos.stream().map(rp -> rp.getPermiso().getNombre()).toList()
        );
    }

    private boolean esAdministrador(Rol rol) {
        return "ADMIN".equalsIgnoreCase(rol.getCodigo());
    }

    private String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.trim().toUpperCase();
    }
}
