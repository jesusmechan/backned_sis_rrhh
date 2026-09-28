package pe.andina.rrhh.adapter.in.web.ratelimit;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.config.RateLimitProperties;

/**
 * Contadores por clave (IP + bucket) con ventana fija de 1 minuto.
 * Pensado para una sola instancia; no compartido entre nodos.
 */
@Component
public class RateLimitService {

    private static final long WINDOW_MS = 60_000L;
    private static final long STALE_MS = 5 * 60_000L;
    private static final long CLEANUP_EVERY_MS = 60_000L;

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMs = new AtomicLong(0);

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
        Window window = windows.compute(bucketKey, (k, existing) -> {
            if (existing == null || now - existing.windowStartMs >= WINDOW_MS) {
                return new Window(now, 1);
            }
            existing.count++;
            return existing;
        });
        maybeCleanup(now);
        int remaining = Math.max(0, limit - window.count);
        long retryAfterSec = Math.max(1L, (WINDOW_MS - (now - window.windowStartMs) + 999) / 1000);
        if (window.count > limit) {
            return Decision.denied(limit, 0, retryAfterSec);
        }
        return Decision.allowed(limit, remaining);
    }

    private void maybeCleanup(long now) {
        long prev = lastCleanupMs.get();
        if (now - prev < CLEANUP_EVERY_MS || !lastCleanupMs.compareAndSet(prev, now)) {
            return;
        }
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Window> e = it.next();
            if (now - e.getValue().windowStartMs > STALE_MS) {
                it.remove();
            }
        }
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
