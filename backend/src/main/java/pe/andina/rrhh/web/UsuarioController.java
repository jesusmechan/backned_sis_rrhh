package pe.andina.rrhh.web;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.common.PageResponses;
import pe.andina.rrhh.dto.AppDtos.PageResponse;
import pe.andina.rrhh.dto.AppDtos.UsuarioRequest;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Perfil, administración de cuentas y estado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario autenticado")
    public UsuarioResponse me() {
        return usuarioService.yo();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    @Operation(summary = "Listar usuarios paginado")
    public PageResponse<UsuarioResponse> listar(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String rol) {
        var data = PageResponses.search(usuarioService.listar(), q, u ->
                PageResponses.text(u.nombreUsuario(), u.nombreCompleto(), u.rol(), u.perfil(), u.correo()));
        if (activo != null) {
            data = data.stream().filter(u -> activo.equals(u.activo())).toList();
        }
        if (rol != null && !rol.isBlank()) {
            data = data.stream()
                    .filter(u -> rol.equalsIgnoreCase(u.rol()) || rol.equalsIgnoreCase(u.perfil()))
                    .toList();
        }
        return PageResponses.of(data, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RRHH')")
    public UsuarioResponse obtener(@PathVariable Integer id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activar o desactivar usuario")
    public UsuarioResponse estado(@PathVariable Integer id, @RequestParam boolean activo) {
        return usuarioService.cambiarEstado(id, activo);
    }
}
