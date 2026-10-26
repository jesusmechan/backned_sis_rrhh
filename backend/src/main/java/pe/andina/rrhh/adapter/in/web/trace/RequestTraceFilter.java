package pe.andina.rrhh.adapter.in.web.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asigna un request id a cada llamada /api/** y registra entrada/salida
 * (método, ruta, usuario, status y duración) para trazabilidad.
 */
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("pe.andina.rrhh.trace.http");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = RequestTrace.incomingId(request);
        RequestTrace.putRequestId(requestId);
        response.setHeader(RequestTrace.HEADER, requestId);
        request.setAttribute(RequestTrace.ATTR_STARTED, System.nanoTime());

        String method = request.getMethod();
        String path = RequestTrace.pathWithQuery(request);
        String ip = RequestTrace.clientIp(request);
        log.info("--> {} {} ip={}", method, path, ip);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestTrace.putUserFromSecurity();
            long started = startedAt(request);
            long durationMs = started > 0 ? (System.nanoTime() - started) / 1_000_000L : -1;
            String user = RequestTrace.currentUser();
            String handler = RequestTrace.handler();
            int status = response.getStatus();
            if (status >= 500) {
                log.error("<-- {} {} status={} {}ms user={} handler={}",
                        method, path, status, durationMs, dash(user), dash(handler));
            } else if (status >= 400) {
                log.warn("<-- {} {} status={} {}ms user={} handler={}",
                        method, path, status, durationMs, dash(user), dash(handler));
            } else {
                log.info("<-- {} {} status={} {}ms user={} handler={}",
                        method, path, status, durationMs, dash(user), dash(handler));
            }
            RequestTrace.clear();
        }
    }

    private static long startedAt(HttpServletRequest request) {
        Object started = request.getAttribute(RequestTrace.ATTR_STARTED);
        return started instanceof Long value ? value : 0L;
    }

    private static String dash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
