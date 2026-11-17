package pe.andina.rrhh.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.andina.rrhh.support.AbstractIntegrationIT;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de seguridad — A02:2025 Configuración incorrecta (cabeceras HTTP).
 */
@DisplayName("SEC — Cabeceras de seguridad HTTP")
class SecurityHeadersIT extends AbstractIntegrationIT {

    @Test
    @DisplayName("SEC-A02-01: respuestas incluyen X-Content-Type-Options nosniff")
    void contentTypeOptions() throws Exception {
        mockMvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    @DisplayName("SEC-A02-02: X-Frame-Options DENY (anti-clickjacking)")
    void frameOptionsDeny() throws Exception {
        mockMvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    @DisplayName("SEC-A02-03: Content-Security-Policy restringida en API")
    void contentSecurityPolicy() throws Exception {
        mockMvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")));
    }

    @Test
    @DisplayName("SEC-A02-04: Referrer-Policy definida")
    void referrerPolicy() throws Exception {
        mockMvc.perform(get("/api/configuracion/publica"))
                .andExpect(status().isOk())
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }
}
