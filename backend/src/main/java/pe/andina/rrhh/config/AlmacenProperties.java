package pe.andina.rrhh.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.almacen")
public class AlmacenProperties {

    /** {@code local} usa disco. {@code s3} queda reservado para el adaptador futuro. */
    private String tipo = "local";

    /** Directorio raíz cuando {@code tipo} es local. Relativo al directorio de trabajo. */
    private String directorio = "data/adjuntos";

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getDirectorio() { return directorio; }
    public void setDirectorio(String directorio) { this.directorio = directorio; }
}