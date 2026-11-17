package pe.andina.rrhh.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad — A07:2025 Fallas de autenticación (JWT).
 * Rate limit 429: ver {@code RateLimitFilterTest} (unidad del filtro).
 */
@DisplayName("SEC — Autenticación endurecida (JWT)")
class SecurityAuthHardeningIT extends AbstractIntegrationIT {

    @Test
    @DisplayName("SEC-A07-01: JWT access expira en ≤ 15 minutos (900 s)")
    void accessTokenExpiraEnQuinceMinutos() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"%s"}
                                """.formatted(DEMO_USER, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn", lessThanOrEqualTo(900)))
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("SEC-A07-02: refresh token no sirve como Bearer en API protegida → 401")
    void refreshTokenNoAutenticaApi() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String refresh = login.get("refreshToken").asText();

        mockMvc.perform(get("/api/sesion").header("Authorization", "Bearer " + refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-A07-04: mensaje de password incorrecta no revela si el usuario existe")
    void mensajeLoginNoEnumeraUsuarios() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"usuario.fantasma99","password":"Andina2026"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }
}
