package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.PageResponses;
import pe.andina.rrhh.application.dto.AppDtos.PageResponse;
import pe.andina.rrhh.application.dto.AppDtos.RolRequest;
import pe.andina.rrhh.application.dto.AppDtos.RolResponse;
import pe.andina.rrhh.application.port.in.RolUseCase;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Roles", description = "Mantenedor de perfiles, menús y permisos funcionales")
public class RolController {

    private final RolUseCase rolService;

    public RolController(RolUseCase rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @Operation(summary = "Listar roles paginado")
    public PageResponse<RolResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo) {
        return PageResponses.query(rolService.listar())
                .search(q, r -> PageResponses.text(r.codigo(), r.nombre(), r.descripcion(), String.join(" ", r.menus())))
                .donde(activo == null ? null : r -> activo.equals(r.activo()))
                .pagina(page, size);
    }

    @GetMapping("/{id}")
    public RolResponse obtener(@PathVariable Integer id) {
        return rolService.obtener(id);
    }

    @PostMapping
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        return rolService.crear(request);
    }

    @PutMapping("/{id}")
    public RolResponse actualizar(@PathVariable Integer id, @Valid @RequestBody RolRequest request) {
        return rolService.actualizar(id, request);
    }
}
