package pe.andina.rrhh.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import pe.andina.rrhh.config.EnvFileLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prepara {@code rrhh_andina_it} con {@code database/01_install.sql} antes de levantar el contexto.
 * Aísla las IT de la base de desarrollo {@code rrhh_andina}.
 */
public class ItDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final String IT_DB = "rrhh_andina_it";
    private static final Object LOCK = new Object();
    private static volatile boolean schemaReady;

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        EnvFileLoader.load();

        String hostUrl = firstNonBlank(
                System.getProperty("IT_DB_ADMIN_URL"),
                System.getenv("IT_DB_ADMIN_URL"),
                "jdbc:postgresql://127.0.0.1:5432/postgres");
        String username = firstNonBlank(
                System.getProperty("IT_DB_USERNAME"),
                System.getProperty("DB_USERNAME"),
                System.getenv("IT_DB_USERNAME"),
                System.getenv("DB_USERNAME"),
                "postgres");
        String password = firstNonBlank(
                System.getProperty("IT_DB_PASSWORD"),
                System.getProperty("DB_PASSWORD"),
                System.getenv("IT_DB_PASSWORD"),
                System.getenv("DB_PASSWORD"),
                "root");

        String itJdbcUrl = "jdbc:postgresql://127.0.0.1:5432/" + IT_DB;

        synchronized (LOCK) {
            if (!schemaReady) {
                createDatabaseIfMissing(hostUrl, username, password);
                applyInstallScript(itJdbcUrl, username, password);
                schemaReady = true;
            }
        }

        // Fuerza la BD de IT por encima de DB_URL del .env de desarrollo.
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.datasource.url", itJdbcUrl);
        overrides.put("spring.datasource.username", username);
        overrides.put("spring.datasource.password", password);
        overrides.put("DB_URL", itJdbcUrl);
        overrides.put("DB_USERNAME", username);
        overrides.put("DB_PASSWORD", password);
        overrides.put("app.rate-limit.enabled", "false");
        overrides.put("app.idempotency.enabled", "false");
        if (isBlank(System.getProperty("JWT_SECRET")) && isBlank(System.getenv("JWT_SECRET"))) {
            overrides.put("app.jwt.secret", "AndinaRRHH2026JwtSecretClaveSuperSeguraDeAlMenos64Caracteres!!");
        }
        context.getEnvironment().getPropertySources()
                .addFirst(new MapPropertySource("it-database-overrides", overrides));
    }

    private static void createDatabaseIfMissing(String adminUrl, String username, String password) {
        try (Connection connection = DriverManager.getConnection(adminUrl, username, password);
             Statement statement = connection.createStatement()) {
            boolean exists;
            try (ResultSet rs = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + IT_DB + "'")) {
                exists = rs.next();
            }
            if (!exists) {
                statement.executeUpdate("CREATE DATABASE " + IT_DB);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "No se pudo crear/verificar la BD de integración '" + IT_DB
                            + "'. ¿PostgreSQL en marcha? Credenciales IT_DB_* / DB_*.", ex);
        }
    }

    private static void applyInstallScript(String jdbcUrl, String username, String password) {
        Path script = resolveInstallScript();
        String sql;
        try {
            sql = Files.readString(script, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer " + script.toAbsolutePath(), ex);
        }
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Falló la carga de " + script.toAbsolutePath() + " sobre " + IT_DB, ex);
        }
    }

    private static Path resolveInstallScript() {
        Path workingDir = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                workingDir.resolve("database").resolve("01_install.sql"),
                workingDir.resolve("..").resolve("database").resolve("01_install.sql")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.normalize().toAbsolutePath();
            }
        }
        throw new IllegalStateException(
                "No se encontró database/01_install.sql (cwd=" + workingDir.toAbsolutePath() + ")");
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
