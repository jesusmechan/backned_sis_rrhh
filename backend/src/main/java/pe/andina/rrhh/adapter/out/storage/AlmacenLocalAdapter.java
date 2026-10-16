package pe.andina.rrhh.adapter.out.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.application.port.out.AlmacenArchivosPort;
import pe.andina.rrhh.config.AlmacenProperties;
import pe.andina.rrhh.domain.exception.DomainException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.almacen", name = "tipo", havingValue = "local", matchIfMissing = true)
public class AlmacenLocalAdapter implements AlmacenArchivosPort {

    private final Path raiz;

    public AlmacenLocalAdapter(AlmacenProperties properties) {
        this.raiz = Path.of(properties.getDirectorio()).toAbsolutePath().normalize();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, byte[] contenido) {
        String clave = carpeta + "/" + UUID.randomUUID() + "-" + sanitizar(nombreArchivo);
        Path destino = resolver(clave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, contenido);
            return clave;
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(destino);
            } catch (IOException ignored) {
                // El rollback de la transacción vuelve a intentar borrar la clave.
            }
            throw new UncheckedIOException("No se pudo guardar el archivo", ex);
        }
    }

    @Override
    public byte[] leer(String clave) {
        Path destino = resolver(clave);
        if (!Files.isRegularFile(destino)) {
            throw DomainException.notFound("Archivo no disponible");
        }
        try {
            return Files.readAllBytes(destino);
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el archivo", ex);
        }
    }

    @Override
    public void eliminar(String clave) {
        try {
            Files.deleteIfExists(resolver(clave));
        } catch (IOException ignored) {
            // No impedir el rollback si el archivo ya no está.
        }
    }

    private Path resolver(String clave) {
        if (clave == null || clave.isBlank() || clave.contains("..") || clave.startsWith("/") || clave.contains("\\")) {
            throw DomainException.badRequest("Clave de archivo no válida");
        }
        Path destino = raiz.resolve(clave).normalize();
        if (!destino.startsWith(raiz)) {
            throw DomainException.badRequest("Clave de archivo no válida");
        }
        return destino;
    }

    private static String sanitizar(String nombre) {
        String base = nombre == null ? "archivo" : nombre;
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        String limpio = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        if (limpio.isBlank() || limpio.equals(".") || limpio.equals("..")) {
            limpio = "archivo";
        }
        return limpio.length() > 80 ? limpio.substring(limpio.length() - 80) : limpio;
    }
}