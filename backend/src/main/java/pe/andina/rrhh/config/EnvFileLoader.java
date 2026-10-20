package pe.andina.rrhh.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EnvFileLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvFileLoader.class);

    private EnvFileLoader() {
    }

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            log.warn("No se encontró archivo .env en el directorio de trabajo: {}", System.getProperty("user.dir"));
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String raw : lines) {
                String line = raw.replace("\uFEFF", "").trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                String key = line.substring(0, separator).trim();
                String value = unquote(line.substring(separator + 1).trim());
                System.setProperty(key, value);
            }
            log.info("Variables cargadas desde {}", envFile.toAbsolutePath());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo .env: " + envFile, ex);
        }
    }

    private static Path resolveEnvFile() {
        Path workingDir = Path.of(System.getProperty("user.dir"));
        Path[] candidates = {
                workingDir.resolve(".env"),
                workingDir.resolve("backend").resolve(".env")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
