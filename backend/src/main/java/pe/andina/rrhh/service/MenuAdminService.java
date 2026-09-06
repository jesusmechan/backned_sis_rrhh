package pe.andina.rrhh.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.common.ApiException;
import pe.andina.rrhh.domain.MenuItem;
import pe.andina.rrhh.domain.Rol;
import pe.andina.rrhh.dto.AppDtos.MenuAdminRequest;
import pe.andina.rrhh.dto.AppDtos.MenuAdminResponse;
import pe.andina.rrhh.repo.MenuItemRepository;
import pe.andina.rrhh.repo.RolRepository;
import pe.andina.rrhh.security.SecurityUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MenuAdminService {

    private final MenuItemRepository menuItemRepository;
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    public MenuAdminService(MenuItemRepository menuItemRepository,
                            RolRepository rolRepository,
                            AuditoriaService auditoriaService) {
        this.menuItemRepository = menuItemRepository;
        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<MenuAdminResponse> listar() {
        return menuItemRepository.findAllWithPerfiles().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MenuAdminResponse obtener(Integer id) {
        return toDto(buscar(id));
    }

    @Transactional
    public MenuAdminResponse crear(MenuAdminRequest request) {
        if (menuItemRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw ApiException.conflict("Ya existe una opción de menú con ese código");
        }
        MenuItem item = new MenuItem();
        aplicar(item, request);
        menuItemRepository.save(item);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "CREAR", "MENU", item.getIdMenu(), item.getCodigo());
        return toDto(item);
    }

    @Transactional
    public MenuAdminResponse actualizar(Integer id, MenuAdminRequest request) {
        if (menuItemRepository.existsByCodigoIgnoreCaseAndIdMenuNot(request.codigo().trim(), id)) {
            throw ApiException.conflict("Ya existe una opción de menú con ese código");
        }
        MenuItem item = buscar(id);
        aplicar(item, request);
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ACTUALIZAR", "MENU", id, item.getCodigo());
        return toDto(item);
    }

    @Transactional
    public void eliminar(Integer id) {
        MenuItem item = buscar(id);
        if (esMantenedor(item)) {
            throw ApiException.badRequest("No se puede eliminar el mantenedor de menú");
        }
        auditoriaService.registrar(SecurityUtils.current().getUsuario(), "ELIMINAR", "MENU", id, item.getCodigo());
        menuItemRepository.delete(item);
    }

    private void aplicar(MenuItem item, MenuAdminRequest request) {
        Set<Rol> perfiles = resolverPerfiles(request.idPerfiles());
        if (esMantenedor(request) && perfiles.stream().noneMatch(r -> "ADMIN".equals(r.getCodigo()))) {
            throw ApiException.badRequest("El mantenedor de menú debe quedar asignado al perfil Administrador");
        }
        if (Boolean.FALSE.equals(request.activo()) && esMantenedor(item)) {
            throw ApiException.badRequest("El mantenedor de menú no puede desactivarse");
        }
        item.setCodigo(request.codigo().trim().toUpperCase());
        item.setEtiqueta(request.etiqueta().trim());
        item.setRuta(normalizarRuta(request.ruta()));
        item.setIcono(request.icono().trim());
        item.setGrupo(request.grupo().trim());
        item.setDescripcion(request.descripcion() != null && !request.descripcion().isBlank()
                ? request.descripcion().trim() : null);
        item.setOrden(request.orden() != null ? request.orden() : 0);
        item.setActivo(request.activo() == null || request.activo());
        item.getPerfiles().clear();
        item.getPerfiles().addAll(perfiles);
    }

    private Set<Rol> resolverPerfiles(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw ApiException.badRequest("Debe asociar la opción al menos a un perfil");
        }
        Set<Rol> perfiles = new HashSet<>();
        for (Integer idRol : ids) {
            perfiles.add(rolRepository.findById(idRol).orElseThrow(() -> ApiException.badRequest("Perfil no existe")));
        }
        return perfiles;
    }

    private MenuItem buscar(Integer id) {
        MenuItem item = menuItemRepository.findById(id).orElseThrow(() -> ApiException.notFound("Opción de menú no encontrada"));
        item.getPerfiles().size();
        return item;
    }

    private MenuAdminResponse toDto(MenuItem item) {
        List<Rol> perfiles = item.getPerfiles().stream()
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .toList();
        return new MenuAdminResponse(
                item.getIdMenu(),
                item.getCodigo(),
                item.getEtiqueta(),
                item.getRuta(),
                item.getIcono(),
                item.getGrupo(),
                item.getDescripcion(),
                item.getOrden(),
                item.getActivo(),
                perfiles.stream().map(Rol::getIdRol).toList(),
                perfiles.stream().map(Rol::getNombre).toList()
        );
    }

    private boolean esMantenedor(MenuItem item) {
        return "MENU".equalsIgnoreCase(item.getCodigo()) || "/menu".equals(item.getRuta());
    }

    private boolean esMantenedor(MenuAdminRequest request) {
        return "MENU".equalsIgnoreCase(request.codigo()) || "/menu".equals(normalizarRuta(request.ruta()));
    }

    private String normalizarRuta(String ruta) {
        String value = ruta == null ? "" : ruta.trim();
        if (value.isBlank()) {
            throw ApiException.badRequest("La ruta es obligatoria");
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return value;
    }
}
