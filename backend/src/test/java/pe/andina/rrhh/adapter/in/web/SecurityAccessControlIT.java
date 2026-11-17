package pe.andina.rrhh.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad — A01:2025 Pérdida de control de acceso (IDOR / roles).
 * Criterio: un EMPLEADO no debe leer ficha ajena ni módulos de planilla.
 */
@DisplayName("SEC — Control de acceso (IDOR / roles)")
class SecurityAccessControlIT extends AbstractIntegrationIT {

    private static final String JEFE_USER = "jesus.pantoja";

    @Test
    @DisplayName("SEC-A01-01: GET /api/empleados sin token → 401")
    void listarEmpleadosSinTokenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/empleados"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("SEC-A01-02: EMPLEADO no puede listar personal (falta PERSONAL_CONSULTAR) → 403")
    void empleadoNoListaPersonal() throws Exception {
        String token = bearer(DEMO_USER);
        mockMvc.perform(get("/api/empleados").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-A01-03: EMPLEADO puede ver su propia ficha → 200")
    void empleadoVeSuFicha() throws Exception {
        JsonNode login = loginAs(DEMO_USER, DEMO_PASSWORD);
        String token = "Bearer " + login.get("accessToken").asText();
        int propio = login.get("usuario").get("idEmpleado").asInt();

        mockMvc.perform(get("/api/empleados/" + propio).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idEmpleado").value(propio));
    }

    @Test
    @DisplayName("SEC-A01-04: EMPLEADO no puede ver ficha de otro (IDOR) → 403")
    void empleadoNoVeFichaAjena() throws Exception {
        JsonNode juan = loginAs(DEMO_USER, DEMO_PASSWORD);
        JsonNode jefe = loginAs(JEFE_USER, DEMO_PASSWORD);
        String tokenJuan = "Bearer " + juan.get("accessToken").asText();
        int idJefe = jefe.get("usuario").get("idEmpleado").asInt();

        mockMvc.perform(get("/api/empleados/" + idJefe).header("Authorization", tokenJuan))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("SEC-A01-05: EMPLEADO no accede a planillas → 403")
    void empleadoNoAccedePlanillas() throws Exception {
        String token = bearer(DEMO_USER);
        mockMvc.perform(get("/api/planillas").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SEC-A01-06: EMPLEADO no accede a auditoría → 403")
    void empleadoNoAccedeAuditoria() throws Exception {
        String token = bearer(DEMO_USER);
        mockMvc.perform(get("/api/auditoria").header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    private String bearer(String usuario) throws Exception {
        return "Bearer " + loginAs(usuario, DEMO_PASSWORD).get("accessToken").asText();
    }
}
