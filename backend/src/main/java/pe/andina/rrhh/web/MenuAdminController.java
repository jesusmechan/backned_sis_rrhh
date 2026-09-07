package pe.andina.rrhh.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.MenuAdminRequest;
import pe.andina.rrhh.dto.AppDtos.MenuAdminResponse;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.service.MenuAdminService;

@RestController
@RequestMapping("/api/menus")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Menú", description = "Mantenedor de opciones de menú por perfil")
public class MenuAdminController {

    private final MenuAdminService menuAdminService;

    public MenuAdminController(MenuAdminService menuAdminService) {
        this.menuAdminService = menuAdminService;
    }

    @GetMapping
    @Operation(summary = "Listar opciones de menú paginado")
    public PageResponse<MenuAdminResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) Boolean activo) {
        var data = PageResponses.search(menuAdminService.listar(), q, m ->
                PageResponses.text(m.codigo(), m.etiqueta(), m.ruta(), m.grupo(), String.join(" ", m.perfiles())));
        if (grupo != null && !grupo.isBlank()) {
            data = data.stream().filter(m -> grupo.equalsIgnoreCase(m.grupo())).toList();
        }
        if (activo != null) {
            data = data.stream().filter(m -> activo.equals(m.activo())).toList();
        }
        return PageResponses.of(data, page, size);
    }

    @GetMapping("/{id}")
    public MenuAdminResponse obtener(@PathVariable Integer id) {
        return menuAdminService.obtener(id);
    }

    @PostMapping
    public MenuAdminResponse crear(@Valid @RequestBody MenuAdminRequest request) {
        return menuAdminService.crear(request);
    }

    @PutMapping("/{id}")
    public MenuAdminResponse actualizar(@PathVariable Integer id, @Valid @RequestBody MenuAdminRequest request) {
        return menuAdminService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Integer id) {
        menuAdminService.eliminar(id);
    }
}
