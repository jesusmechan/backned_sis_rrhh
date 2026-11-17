package pe.andina.rrhh.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad — A05:2025 Inyección.
 * Login usa JPA + Bean Validation: payloads SQLi/XSS no deben autenticar ni filtrar SQL crudo.
 */
@DisplayName("SEC — Inyección (SQLi / payloads maliciosos)")
class SecurityInjectionIT extends AbstractIntegrationIT {

    @Test
    @DisplayName("SEC-A05-01: payload clásico SQLi en usuario → 400 (validación), no login")
    void sqlInjectionEnUsuarioRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"admin' OR '1'='1","password":"cualquiera"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(content().string(not(org.hamcrest.Matchers.containsStringIgnoringCase("SQLException"))));
    }

    @Test
    @DisplayName("SEC-A05-02: comentario SQL admin'-- → 400")
    void sqlCommentBypassRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"admin'--","password":"x"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    @DisplayName("SEC-A05-03: UNION SELECT en usuario → 400")
    void unionSelectRechazado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"' UNION SELECT NULL--","password":"x"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SEC-A05-04: XSS en password no autentica ni expone stack → 401")
    void xssEnPasswordNoAutentica() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"%s","password":"<script>alert(1)</script>"}
                                """.formatted(DEMO_USER)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsStringIgnoringCase("Exception"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsStringIgnoringCase("at pe.andina"))));
    }

    @Test
    @DisplayName("SEC-A05-05: usuario inexistente con metacaracteres → 400 o 401, sin detalle SQL")
    void metacaracteresNoFiltranSql() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreUsuario":"or_1_1","password":"1' OR '1'='1"}
                                """))
                .andExpect(status().is(anyOf(is(400), is(401))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsStringIgnoringCase("syntax error"))))
                .andExpect(content().string(not(org.hamcrest.Matchers.containsStringIgnoringCase("postgresql"))));
    }
}
