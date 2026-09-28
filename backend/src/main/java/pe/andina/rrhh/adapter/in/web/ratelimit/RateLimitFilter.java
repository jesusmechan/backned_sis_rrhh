package pe.andina.rrhh.adapter.in.web.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.andina.rrhh.adapter.in.web.ApiError;
import pe.andina.rrhh.config.RateLimitProperties;

/**
 * Aplica rate limiting a /api/** antes de Security.
 * Login/refresh usan un cupo más estricto (mitiga fuerza bruta).
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitProperties properties,
                           RateLimitService rateLimitService,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean auth = isAuthEndpoint(path);
        String clientKey = clientKey(request);
        RateLimitService.Decision decision = rateLimitService.tryConsume(clientKey, auth);

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            String message = auth
                    ? "Demasiados intentos de autenticación. Espere un momento e intente de nuevo."
                    : "Ha superado el límite de solicitudes. Espere un momento e intente de nuevo.";
            objectMapper.writeValue(response.getWriter(),
                    ApiError.of(429, "Too Many Requests", message));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isAuthEndpoint(String path) {
        return "/api/auth/login".equals(path) || "/api/auth/refresh".equals(path);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
