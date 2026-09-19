package pe.andina.rrhh.application.port.in;

import pe.andina.rrhh.application.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;

public interface AuthUseCase {
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshTokenValue);
    void logout(String refreshTokenValue);
    void logoutAll();
}
