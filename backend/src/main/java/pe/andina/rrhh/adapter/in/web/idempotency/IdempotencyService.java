package pe.andina.rrhh.adapter.in.web.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.config.IdempotencyProperties;

/**
 * Caché en memoria de respuestas por Idempotency-Key (una instancia).
 * Evita efectos duplicados ante reintentos de red en operaciones críticas.
 */
@Component
public class IdempotencyService {

    private static final long CLEANUP_EVERY_MS = 60_000L;

    private final IdempotencyProperties properties;
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupMs = new AtomicLong(0);

    public IdempotencyService(IdempotencyProperties properties) {
        this.properties = properties;
    }

    public String fingerprint(String method, String path, byte[] body) {
        String raw = method + "\n" + path + "\n" + sha256(body == null ? new byte[0] : body);
        return sha256(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Clave estable por actor + Idempotency-Key (el fingerprint se valida aparte). */
    public String storageKey(String actor, String idempotencyKey) {
        return sha256((actor + "\n" + idempotencyKey).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return CLAIMED si la clave es nueva y se marcó en progreso;
     *         BUSY si otra petición ya la tiene en curso;
     *         REPLAY con la respuesta cacheada;
     *         MISMATCH si la misma key se reusa con otro cuerpo/ruta.
     */
    public ClaimResult claim(String storageKey, String fingerprint) {
        maybeCleanup(System.currentTimeMillis());
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1, properties.getTtlHours()) * 3_600_000L;
        AtomicReference<ClaimResult> outcome = new AtomicReference<>();

        store.compute(storageKey, (k, existing) -> {
            if (existing != null && now - existing.createdAtMs > ttlMs) {
                existing = null;
            }
            if (existing == null) {
                outcome.set(ClaimResult.claimed());
                return Entry.inProgress(fingerprint, now);
            }
            if (!existing.fingerprint.equals(fingerprint)) {
                outcome.set(ClaimResult.mismatch());
                return existing;
            }
            if (existing.state == State.IN_PROGRESS) {
                outcome.set(ClaimResult.busy());
                return existing;
            }
            outcome.set(ClaimResult.replay(existing.status, existing.contentType, existing.body));
            return existing;
        });

        return outcome.get();
    }

    public void complete(String storageKey, int status, String contentType, byte[] body) {
        store.computeIfPresent(storageKey, (k, existing) ->
                Entry.completed(existing.fingerprint, existing.createdAtMs, status, contentType, body));
    }

    public void abandon(String storageKey) {
        store.computeIfPresent(storageKey, (k, existing) ->
                existing.state == State.IN_PROGRESS ? null : existing);
    }

    private void maybeCleanup(long now) {
        long prev = lastCleanupMs.get();
        if (now - prev < CLEANUP_EVERY_MS || !lastCleanupMs.compareAndSet(prev, now)) {
            return;
        }
        long ttlMs = Math.max(1, properties.getTtlHours()) * 3_600_000L;
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (now - e.getValue().createdAtMs > ttlMs) {
                it.remove();
            }
        }
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    public enum State { IN_PROGRESS, COMPLETED }

    public record ClaimResult(Kind kind, int status, String contentType, byte[] body) {
        public enum Kind { CLAIMED, BUSY, REPLAY, MISMATCH }

        static ClaimResult claimed() {
            return new ClaimResult(Kind.CLAIMED, 0, null, null);
        }

        static ClaimResult busy() {
            return new ClaimResult(Kind.BUSY, 0, null, null);
        }

        static ClaimResult mismatch() {
            return new ClaimResult(Kind.MISMATCH, 0, null, null);
        }

        static ClaimResult replay(int status, String contentType, byte[] body) {
            return new ClaimResult(Kind.REPLAY, status, contentType, body);
        }
    }

    private static final class Entry {
        final State state;
        final String fingerprint;
        final long createdAtMs;
        final int status;
        final String contentType;
        final byte[] body;

        private Entry(State state, String fingerprint, long createdAtMs, int status, String contentType, byte[] body) {
            this.state = state;
            this.fingerprint = fingerprint;
            this.createdAtMs = createdAtMs;
            this.status = status;
            this.contentType = contentType;
            this.body = body;
        }

        static Entry inProgress(String fingerprint, long now) {
            return new Entry(State.IN_PROGRESS, fingerprint, now, 0, null, null);
        }

        static Entry completed(String fingerprint, long createdAtMs, int status, String contentType, byte[] body) {
            return new Entry(State.COMPLETED, fingerprint, createdAtMs, status, contentType, body);
        }
    }
}
