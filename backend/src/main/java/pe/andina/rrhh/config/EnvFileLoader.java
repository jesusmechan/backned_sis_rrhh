package pe.andina.rrhh.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class EnvFileLoader {

    private EnvFileLoader() {
    }

    public static void load() {
        Path envFile = resolveEnvFile();
        if (envFile == null) {
            System.err.println("No se encontró archivo .env en el directorio de trabajo: " + System.getProperty("user.dir"));
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
            System.out.println("Variables cargadas desde " + envFile.toAbsolutePath());
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
