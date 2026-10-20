package pe.andina.rrhh.adapter.in.web.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.config.RateLimitProperties;

/**
 * Contadores por clave (IP + bucket) con ventana fija de 1 minuto.
 * Pensado para una sola instancia; no compartido entre nodos.
 * Guava Cache expira claves inactivas y acota el tamaño en memoria.
 */
@Component
public class RateLimitService {

    private static final long WINDOW_MS = 60_000L;

    private final RateLimitProperties properties;
    private final Cache<String, Window> windows = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    public RateLimitService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public Decision tryConsume(String clientKey, boolean authEndpoint) {
        int limit = authEndpoint ? properties.getAuthPerMinute() : properties.getApiPerMinute();
        if (limit <= 0) {
            return Decision.allowed(limit, limit);
        }
        String bucketKey = (authEndpoint ? "auth:" : "api:") + clientKey;
        long now = System.currentTimeMillis();
        Window window = windows.asMap().compute(bucketKey, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= WINDOW_MS) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });
        int remaining = Math.max(0, limit - window.count);
        long retryAfterSec = Math.max(1L, (WINDOW_MS - (now - window.windowStartMs) + 999) / 1000);
        if (window.count > limit) {
            return Decision.denied(limit, 0, retryAfterSec);
        }
        return Decision.allowed(limit, remaining);
    }

    public record Decision(boolean allowed, int limit, int remaining, long retryAfterSeconds) {
        static Decision allowed(int limit, int remaining) {
            return new Decision(true, limit, remaining, 0);
        }

        static Decision denied(int limit, int remaining, long retryAfterSeconds) {
            return new Decision(false, limit, remaining, retryAfterSeconds);
        }
    }

    private static final class Window {
        final long windowStartMs;
        int count;

        Window(long windowStartMs, int count) {
            this.windowStartMs = windowStartMs;
            this.count = count;
        }
    }
}
