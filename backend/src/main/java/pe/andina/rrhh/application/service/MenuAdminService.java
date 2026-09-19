package pe.andina.rrhh.application.service;
import pe.andina.rrhh.application.port.out.CurrentUserPort;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;

import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.in.MenuAdminUseCase;
import org.springframework.transaction.annotation.Transactional;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.MenuItem;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.application.dto.AppDtos.MenuAdminRequest;
import pe.andina.rrhh.application.dto.AppDtos.MenuAdminResponse;
import pe.andina.rrhh.application.port.out.MenuItemPort;
import pe.andina.rrhh.application.port.out.RolPort;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MenuAdminService implements MenuAdminUseCase {

    private final MenuItemPort menuItemRepository;
    private final RolPort rolRepository;
    private final AuditoriaUseCase auditoriaService;

    private final CurrentUserPort currentUser;

    public MenuAdminService(MenuItemPort menuItemRepository,
                            RolPort rolRepository,
                            AuditoriaUseCase auditoriaService,
                           CurrentUserPort currentUser) {
        this.menuItemRepository = menuItemRepository;
        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
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
            throw DomainException.conflict("Ya existe una opción de menú con ese código");
        }
        MenuItem item = new MenuItem();
        aplicar(item, request);
        menuItemRepository.save(item);
        auditoriaService.registrar(currentUser.usuario(), "CREAR", "MENU", item.getIdMenu(), item.getCodigo());
        return toDto(item);
    }

    @Transactional
    public MenuAdminResponse actualizar(Integer id, MenuAdminRequest request) {
        if (menuItemRepository.existsByCodigoIgnoreCaseAndIdMenuNot(request.codigo().trim(), id)) {
            throw DomainException.conflict("Ya existe una opción de menú con ese código");
        }
        MenuItem item = buscar(id);
        aplicar(item, request);
        auditoriaService.registrar(currentUser.usuario(), "ACTUALIZAR", "MENU", id, item.getCodigo());
        return toDto(item);
    }

    @Transactional
    public void eliminar(Integer id) {
        MenuItem item = buscar(id);
        if (esMantenedor(item)) {
            throw DomainException.badRequest("No se puede eliminar este mantenedor");
        }
        auditoriaService.registrar(currentUser.usuario(), "ELIMINAR", "MENU", id, item.getCodigo());
        menuItemRepository.delete(item);
    }

    private void aplicar(MenuItem item, MenuAdminRequest request) {
        Set<Rol> perfiles = resolverPerfiles(request.idPerfiles());
        if (esMantenedor(request) && perfiles.stream().noneMatch(r -> "ADMIN".equalsIgnoreCase(r.getCodigo()))) {
            throw DomainException.badRequest("El mantenedor debe quedar asignado al perfil Administrador");
        }
        if (Boolean.FALSE.equals(request.activo()) && esMantenedor(item)) {
            throw DomainException.badRequest("El mantenedor no puede desactivarse");
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
            throw DomainException.badRequest("Debe asociar la opción al menos a un perfil");
        }
        Set<Rol> perfiles = new HashSet<>();
        for (Integer idRol : ids) {
            perfiles.add(rolRepository.findById(idRol).orElseThrow(() -> DomainException.badRequest("Perfil no existe")));
        }
        return perfiles;
    }

    private MenuItem buscar(Integer id) {
        MenuItem item = menuItemRepository.findById(id).orElseThrow(() -> DomainException.notFound("Opción de menú no encontrada"));
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
        return esMantenedor(item.getCodigo(), item.getRuta());
    }

    private boolean esMantenedor(MenuAdminRequest request) {
        return esMantenedor(request.codigo(), normalizarRuta(request.ruta()));
    }

    private boolean esMantenedor(String codigo, String ruta) {
        return "MENU".equalsIgnoreCase(codigo) || "/menu".equals(ruta)
                || "ROLES".equalsIgnoreCase(codigo) || "/roles".equals(ruta);
    }

    private String normalizarRuta(String ruta) {
        String value = ruta == null ? "" : ruta.trim();
        if (value.isBlank()) {
            throw DomainException.badRequest("La ruta es obligatoria");
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        return value;
    }
}
