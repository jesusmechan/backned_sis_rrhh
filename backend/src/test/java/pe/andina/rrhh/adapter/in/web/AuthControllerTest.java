package pe.andina.rrhh.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.andina.rrhh.application.dto.AppDtos.SesionResponse;
import pe.andina.rrhh.application.dto.AuthDtos.AuthResponse;
import pe.andina.rrhh.application.dto.AuthDtos.LoginRequest;
import pe.andina.rrhh.application.port.in.AuthUseCase;
import pe.andina.rrhh.domain.exception.DomainException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthUseCase authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login devuelve 200 y JSON con tokens")
    void loginOk() throws Exception {
        var sesion = new SesionResponse(10, "juan.espinoza", "j@andina.pe", "EMPLEADO", "Empleado",
                1, null, "Juan", List.of(), List.of());
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new AuthResponse("access", "refresh-1", "Bearer", 3600, sesion));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("juan.espinoza", "Andina2026"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/login con usuario inválido devuelve 400")
    void loginValidacion400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"ab","password":"Andina2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales incorrectas devuelve 401")
    void loginUnauthorized() throws Exception {
        when(authService.login(any())).thenThrow(DomainException.unauthorized("Usuario o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("juan.espinoza", "mala"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
