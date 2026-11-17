package pe.andina.rrhh.adapter.in.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import pe.andina.rrhh.config.RateLimitProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SEC-A07 — rate limit de login (unidad del filtro).
 */
@DisplayName("SEC — RateLimitFilter (fuerza bruta login)")
class RateLimitFilterTest {

    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setAuthPerMinute(5);
        props.setApiPerMinute(100);
        filter = new RateLimitFilter(props, new RateLimitService(props), new ObjectMapper().findAndRegisterModules());
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("SEC-A07-03: el 6.º intento de login desde la misma IP responde 429")
    void sextoLoginResponde429() throws Exception {
        String ip = "198.51.100.10";
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse allowed = dispatchLogin(ip);
            assertEquals(200, allowed.getStatus());
            assertEquals("5", allowed.getHeader("X-RateLimit-Limit"));
        }
        verify(chain, times(5)).doFilter(any(), any());

        MockHttpServletResponse blocked = dispatchLogin(ip);
        assertEquals(429, blocked.getStatus());
        assertEquals("0", blocked.getHeader("X-RateLimit-Remaining"));
        assertTrue(blocked.getHeader("Retry-After") != null && !blocked.getHeader("Retry-After").isBlank());
        assertTrue(blocked.getContentAsString().contains("Demasiados intentos"));
        verify(chain, times(5)).doFilter(any(), any());
    }

    private MockHttpServletResponse dispatchLogin(String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.addHeader("X-Forwarded-For", ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
