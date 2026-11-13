package pe.andina.rrhh.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT Auth — login / refresh / logout contra PostgreSQL")
class AuthIntegrationIT extends AbstractIntegrationIT {

    @Test
    @DisplayName("CP-AUTH-IT-01: login válido emite access y refresh token")
    void loginValidoEmiteTokens() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"%s"}
                                """.formatted(DEMO_USER, DEMO_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.usuario.nombreUsuario").value(DEMO_USER))
                .andExpect(jsonPath("$.usuario.rol").value("EMPLEADO"));
    }

    @Test
    @DisplayName("CP-AUTH-IT-02: password incorrecta responde 401")
    void loginPasswordIncorrecta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"clave-incorrecta"}
                                """.formatted(DEMO_USER)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("CP-AUTH-IT-03: usuario corto inválido responde 400")
    void loginValidacionUsuarioCorto() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"ab","password":"Andina2026"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("CP-AUTH-IT-04: refresh rota el refresh token y emite nuevo access")
    void refreshEmiteNuevosTokens() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(refreshToken)));
    }

    @Test
    @DisplayName("CP-AUTH-IT-05: refresh token ya usado (rotado) responde 401")
    void refreshTokenReusadoFalla() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CP-AUTH-IT-06: logout revoca el refresh token")
    void logoutRevocaRefresh() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String accessToken = login.get("accessToken").asText();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }
}
