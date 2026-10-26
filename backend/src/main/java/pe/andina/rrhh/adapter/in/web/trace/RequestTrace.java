package pe.andina.rrhh.adapter.in.web.trace;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pe.andina.rrhh.adapter.in.security.UsuarioPrincipal;

/**
 * Identidad de la petición HTTP (request id, usuario, IP) para correlacionar logs.
 */
public final class RequestTrace {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER = "user";
    public static final String MDC_HANDLER = "handler";
    public static final String ATTR_STARTED = RequestTrace.class.getName() + ".started";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "token", "refreshtoken", "refresh_token", "access_token",
            "authorization", "secret", "apikey", "api_key");

    private RequestTrace() {}

    public static String incomingId(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String correlation = request.getHeader("X-Correlation-Id");
        if (correlation != null && !correlation.isBlank()) {
            return correlation.trim();
        }
        return UUID.randomUUID().toString();
    }

    public static void putRequestId(String requestId) {
        MDC.put(MDC_REQUEST_ID, requestId);
    }

    public static void putHandler(String handler) {
        if (handler != null && !handler.isBlank()) {
            MDC.put(MDC_HANDLER, handler);
        }
    }

    public static void putUserFromSecurity() {
        String user = currentUser();
        if (user != null) {
            MDC.put(MDC_USER, user);
        }
    }

    public static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UsuarioPrincipal usuario) {
            return usuario.getUsername();
        }
        String name = auth.getName();
        return name == null || name.isBlank() || "anonymousUser".equals(name) ? null : name;
    }

    public static String handler() {
        return MDC.get(MDC_HANDLER);
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "-";
    }

    public static String sanitizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int eq = pair.indexOf('=');
            String key = eq >= 0 ? pair.substring(0, eq) : pair;
            String value = eq >= 0 ? pair.substring(eq + 1) : null;
            if (isSensitive(key)) {
                value = "***";
            }
            if (!out.isEmpty()) {
                out.append('&');
            }
            out.append(key);
            if (value != null) {
                out.append('=').append(value);
            }
        }
        return out.toString();
    }

    public static String pathWithQuery(HttpServletRequest request) {
        String path = request.getRequestURI();
        String query = sanitizeQuery(request.getQueryString());
        return query.isEmpty() ? path : path + "?" + query;
    }

    public static void clear() {
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_USER);
        MDC.remove(MDC_HANDLER);
    }

    private static boolean isSensitive(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "");
        return SENSITIVE_KEYS.contains(normalized)
                || normalized.contains("password")
                || normalized.contains("secret")
                || normalized.contains("token");
    }
}
