package pe.andina.rrhh.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.andina.rrhh.application.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;
import pe.andina.rrhh.application.dto.AuthDtos.RefreshRequest;
import pe.andina.rrhh.application.port.in.AuthUseCase;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Login, refresh token y cierre de sesión")
public class AuthController {

    private final AuthUseCase authService;

    public AuthController(AuthUseCase authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Iniciar sesión", description = "Usuarios de prueba: jesus.mechan, carla.reyes, jesus.pantoja, juan.espinoza. Contraseña: Andina2026")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Renovar access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar refresh token")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.logout(request.refreshToken());
        } else {
            authService.logoutAll();
        }
        return ResponseEntity.noContent().build();
    }
}
