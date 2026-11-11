package pe.andina.rrhh.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IT Sesión — recurso protegido con JWT real")
class SesionIntegrationIT extends AbstractIntegrationIT {

    @Test
    @DisplayName("CP-AUTH-IT-07: GET /api/sesion sin token responde 401")
    void sesionSinTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/sesion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CP-AUTH-IT-08: GET /api/sesion con Bearer válido devuelve perfil")
    void sesionConTokenOk() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String accessToken = login.get("accessToken").asText();

        mockMvc.perform(get("/api/sesion")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreUsuario").value(DEMO_USER))
                .andExpect(jsonPath("$.rol").value("EMPLEADO"))
                .andExpect(jsonPath("$.menu.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("CP-AUTH-IT-09: GET /api/configuracion/publica es accesible sin autenticación")
    void configuracionPublicaSinAuth() throws Exception {
        mockMvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app_nombre").exists())
                .andExpect(jsonPath("$.empresa_razon_social").exists());
    }
}
