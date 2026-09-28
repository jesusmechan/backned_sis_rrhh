package pe.andina.rrhh.adapter.in.web.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import pe.andina.rrhh.adapter.in.web.ApiError;
import pe.andina.rrhh.config.IdempotencyProperties;

/**
 * Si el cliente envía {@code Idempotency-Key} en operaciones críticas,
 * reutiliza la respuesta del primer intento (evita doble cierre/aprobación/marcación).
 */
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";

    private static final Pattern CRITICAL = Pattern.compile(
            "^/api/(planillas/\\d+/(calcular|cerrar)|pasos/\\d+/(aprobar|rechazar)|asistencias/marcar)$");

    private static final Set<String> METHODS = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyProperties properties;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public IdempotencyFilter(IdempotencyProperties properties,
                             IdempotencyService idempotencyService,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        if (!METHODS.contains(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null || !CRITICAL.matcher(path).matches()) {
            return true;
        }
        String key = request.getHeader(HEADER);
        return key == null || key.isBlank();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CachedBodyRequest req = new CachedBodyRequest(request);
        byte[] body = req.body();

        String key = req.getHeader(HEADER).trim();
        if (key.length() > 128) {
            writeError(response, 400, "Bad Request", "Idempotency-Key demasiado largo (máx. 128)");
            return;
        }

        String fingerprint = idempotencyService.fingerprint(req.getMethod(), req.getRequestURI(), body);
        String storageKey = idempotencyService.storageKey(actorKey(req), key);

        IdempotencyService.ClaimResult claim = idempotencyService.claim(storageKey, fingerprint);
        switch (claim.kind()) {
            case REPLAY -> {
                response.setStatus(claim.status());
                if (claim.contentType() != null) {
                    response.setContentType(claim.contentType());
                }
                response.setHeader(REPLAYED_HEADER, "true");
                byte[] cached = claim.body() != null ? claim.body() : new byte[0];
                response.setContentLength(cached.length);
                response.getOutputStream().write(cached);
                return;
            }
            case BUSY -> {
                writeError(response, 409, "Conflict",
                        "Ya hay una solicitud en curso con la misma Idempotency-Key. Reintente en unos segundos.");
                return;
            }
            case MISMATCH -> {
                writeError(response, 422, "Unprocessable Entity",
                        "Idempotency-Key reutilizada con un cuerpo o ruta distinta.");
                return;
            }
            case CLAIMED -> {
                // continúa
            }
        }

        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(req, res);
            byte[] responseBody = res.getContentAsByteArray();
            if (res.getStatus() < 500) {
                idempotencyService.complete(storageKey, res.getStatus(), res.getContentType(), responseBody);
            } else {
                idempotencyService.abandon(storageKey);
            }
            res.copyBodyToResponse();
        } catch (Exception ex) {
            idempotencyService.abandon(storageKey);
            throw ex;
        }
    }

    private static String actorKey(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && !auth.isBlank()) {
            return auth.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiError.of(status, error, message));
    }
}
