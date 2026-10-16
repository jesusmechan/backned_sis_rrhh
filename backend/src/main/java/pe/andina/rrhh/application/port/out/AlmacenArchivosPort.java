package pe.andina.rrhh.application.port.out;

/**
 * Almacén de archivos de la aplicación.
 *
 * <p>La clave que devuelve {@link #guardar} es lógica: el adaptador local la
 * resuelve dentro de su directorio y un adaptador S3 la usará como object key
 * sin cambiar a quienes llaman este puerto.</p>
 */
public interface AlmacenArchivosPort {

    String guardar(String carpeta, String nombreArchivo, byte[] contenido);

    byte[] leer(String clave);

    void eliminar(String clave);
}