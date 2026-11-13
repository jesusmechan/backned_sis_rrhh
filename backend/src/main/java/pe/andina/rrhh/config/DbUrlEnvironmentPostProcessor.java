package pe.andina.rrhh.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Normaliza {@code DB_URL}: prefijo JDBC y, si viene la URI de Neon con credenciales embebidas,
 * las extrae a {@code DB_USERNAME} / {@code DB_PASSWORD}.
 */
public class DbUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String KEY_URL = "DB_URL";
    private static final String KEY_USER = "DB_USERNAME";
    private static final String KEY_PASSWORD = "DB_PASSWORD";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty(KEY_URL);
        if (raw == null || raw.isBlank()) {
            return;
        }
        NormalizedDb normalized = normalize(raw.trim());
        if (!normalized.changed()) {
            return;
        }
        Map<String, Object> overrides = new HashMap<>();
        overrides.put(KEY_URL, normalized.jdbcUrl());
        if (normalized.username() != null && isBlank(environment.getProperty(KEY_USER))) {
            overrides.put(KEY_USER, normalized.username());
        }
        if (normalized.password() != null && isBlank(environment.getProperty(KEY_PASSWORD))) {
            overrides.put(KEY_PASSWORD, normalized.password());
        }
        environment.getPropertySources().addFirst(new MapPropertySource("normalizedDbUrl", overrides));
    }

    static NormalizedDb normalize(String url) {
        String jdbc = toJdbcUrl(url);
        CredentialsInUrl creds = extractEmbeddedCredentials(jdbc);
        if (creds == null) {
            boolean prefixed = !jdbc.equals(url);
            return new NormalizedDb(jdbc, null, null, prefixed);
        }
        return new NormalizedDb(creds.jdbcUrlWithoutUserInfo(), creds.username(), creds.password(), true);
    }

    static String toJdbcUrl(String url) {
        if (url.startsWith("jdbc:")) {
            return url;
        }
        if (url.startsWith("postgresql://")) {
            return "jdbc:" + url;
        }
        if (url.startsWith("postgres://")) {
            return "jdbc:postgresql://" + url.substring("postgres://".length());
        }
        return url;
    }

    /**
     * {@code jdbc:postgresql://user:pass@host/db?...} → URL sin userinfo + credenciales.
     */
    private static CredentialsInUrl extractEmbeddedCredentials(String jdbcUrl) {
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int authorityStart = schemeEnd + 3;
        int slash = jdbcUrl.indexOf('/', authorityStart);
        int at = jdbcUrl.indexOf('@', authorityStart);
        if (at < 0) {
            return null;
        }
        if (slash >= 0 && at > slash) {
            return null;
        }
        String userInfo = jdbcUrl.substring(authorityStart, at);
        String remainder = jdbcUrl.substring(at + 1);
        int colon = userInfo.indexOf(':');
        String user = decode(colon >= 0 ? userInfo.substring(0, colon) : userInfo);
        String password = decode(colon >= 0 ? userInfo.substring(colon + 1) : "");
        String withoutCreds = jdbcUrl.substring(0, authorityStart) + remainder;
        return new CredentialsInUrl(withoutCreds, user, password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    record NormalizedDb(String jdbcUrl, String username, String password, boolean changed) {}

    private record CredentialsInUrl(String jdbcUrlWithoutUserInfo, String username, String password) {}
}
