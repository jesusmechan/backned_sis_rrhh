package pe.andina.rrhh.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.andina.rrhh.application.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.application.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;
import pe.andina.rrhh.application.port.in.AuditoriaUseCase;
import pe.andina.rrhh.application.port.in.SesionUseCase;
import pe.andina.rrhh.application.port.out.RefreshTokenPort;
import pe.andina.rrhh.application.port.out.TokenPort;
import pe.andina.rrhh.application.port.out.UsuarioPort;
import pe.andina.rrhh.domain.exception.DomainException;
import pe.andina.rrhh.domain.model.RefreshToken;
import pe.andina.rrhh.domain.model.Rol;
import pe.andina.rrhh.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioPort usuarioRepository;
    @Mock
    private RefreshTokenPort refreshTokenRepository;
    @Mock
    private TokenPort tokenPort;
    @Mock
    private AuditoriaUseCase auditoriaService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SesionUseCase sesionService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioActivo;

    @BeforeEach
    void setUp() {
        Rol rol = new Rol();
        rol.setIdRol(1);
        rol.setCodigo("EMPLEADO");
        rol.setNombre("Empleado");

        usuarioActivo = new Usuario();
        usuarioActivo.setIdUsuario(10);
        usuarioActivo.setNombreUsuario("juan.espinoza");
        usuarioActivo.setCorreo("juan@andina.pe");
        usuarioActivo.setPasswordHash("$2a$10$hash");
        usuarioActivo.setActivo(true);
        usuarioActivo.setRol(rol);
    }

    @Test
    @DisplayName("CP-AUTH-01: login con credenciales válidas emite tokens")
    void loginExitoso() {
        var request = new LoginRequest("juan.espinoza", "Andina2026");
        when(usuarioRepository.findByNombreUsuario("juan.espinoza")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Andina2026", usuarioActivo.getPasswordHash())).thenReturn(true);
        when(tokenPort.generateAccessToken(10, "juan.espinoza", "EMPLEADO")).thenReturn("jwt-access");
        when(tokenPort.getAccessExpirationMs()).thenReturn(3_600_000L);
        when(tokenPort.getRefreshExpirationMs()).thenReturn(86_400_000L);
        when(sesionService.desde(usuarioActivo)).thenReturn(
                new SesionResponse(10, "juan.espinoza", "juan@andina.pe", "EMPLEADO", "Empleado",
                        1, null, "juan.espinoza", List.of(), List.of()));

        AuthResponse response = authService.login(request);

        assertEquals("jwt-access", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertNotNull(response.refreshToken());
        assertFalse(response.refreshToken().isBlank());
        verify(auditoriaService).registrar(eq(usuarioActivo), eq("LOGIN"), eq("USUARIO"), eq(10), isNull());
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(usuarioActivo, captor.getValue().getUsuario());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getRevocado()));
    }

    @Test
    @DisplayName("CP-AUTH-02: contraseña incorrecta rechaza el acceso")
    void loginConPasswordIncorrecta() {
        when(usuarioRepository.findByNombreUsuario("juan.espinoza")).thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("incorrecta", usuarioActivo.getPasswordHash())).thenReturn(false);

        DomainException ex = assertThrows(DomainException.class,
                () -> authService.login(new LoginRequest("juan.espinoza", "incorrecta")));

        assertEquals(DomainException.Code.UNAUTHORIZED, ex.getCode());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-AUTH-02b: usuario inexistente rechaza el acceso")
    void loginUsuarioDesconocido() {
        when(usuarioRepository.findByNombreUsuario("nadie")).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class,
                () -> authService.login(new LoginRequest("nadie", "Andina2026")));

        assertEquals(DomainException.Code.UNAUTHORIZED, ex.getCode());
    }

    @Test
    @DisplayName("Usuario inactivo no puede iniciar sesión")
    void loginUsuarioInactivo() {
        usuarioActivo.setActivo(false);
        when(usuarioRepository.findByNombreUsuario("juan.espinoza")).thenReturn(Optional.of(usuarioActivo));

        DomainException ex = assertThrows(DomainException.class,
                () -> authService.login(new LoginRequest("juan.espinoza", "Andina2026")));

        assertEquals(DomainException.Code.UNAUTHORIZED, ex.getCode());
        assertTrue(ex.getMessage().toLowerCase().contains("inactivo"));
    }
}
