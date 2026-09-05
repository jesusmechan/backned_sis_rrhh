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
import pe.andina.rrhh.dto.AppDtos.UsuarioRequest;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;
import pe.andina.rrhh.service.UsuarioService;

import java.util.List;

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
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
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
