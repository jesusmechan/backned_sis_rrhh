package pe.andina.rrhh.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import pe.andina.rrhh.dto.AppDtos.SesionResponse;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Schema(example = "jesus.mechan") String nombreUsuario,
            @NotBlank @Schema(example = "Andina2026") String password
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
