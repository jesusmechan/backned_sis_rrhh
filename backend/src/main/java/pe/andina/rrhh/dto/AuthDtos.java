package pe.andina.rrhh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import pe.andina.rrhh.dto.AppDtos.SesionResponse;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "Indique el usuario")
            @Pattern(regexp = "^[a-z][a-z0-9._]{2,59}$", message = "El usuario solo admite minúsculas, números, punto o guion bajo")
            @Schema(example = "jesus.mechan") String nombreUsuario,
            @NotBlank(message = "Indique la contraseña")
            @Schema(example = "Andina2026") String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            SesionResponse usuario
    ) {}

    public record UsuarioResumen(
            Integer idUsuario,
            String nombreUsuario,
            String correo,
            String rol,
            Integer idEmpleado,
            String nombreCompleto
    ) {}
}
