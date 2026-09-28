package pe.andina.rrhh.adapter.in.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import pe.andina.rrhh.application.port.out.TokenPort;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService implements TokenPort {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(requireSecret(properties.getSecret()));
    }

    private static byte[] requireSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido. Configúralo en backend/.env (mínimo 32 caracteres).");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET es demasiado corto (" + bytes.length
                            + " bytes). Use al menos " + MIN_SECRET_BYTES + " caracteres.");
        }
        return bytes;
    }

    public String generateAccessToken(Integer idUsuario, String nombreUsuario, String rol) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(properties.getAccessExpirationMs());
        return Jwts.builder()
                .subject(nombreUsuario)
                .claims(Map.of(
                        "uid", idUsuario,
                        "rol", rol,
                        "typ", "access"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public boolean isAccessToken(String token) {
        try {
            return "access".equals(parse(token).get("typ", String.class));
        } catch (Exception ex) {
            return false;
        }
    }

    public long getAccessExpirationMs() {
        return properties.getAccessExpirationMs();
    }

    public long getRefreshExpirationMs() {
        return properties.getRefreshExpirationMs();
    }
}
