package pe.andina.rrhh.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            secret = java.util.Arrays.copyOf(secret, 32);
        }
        this.key = Keys.hmacShaKeyFor(secret);
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
