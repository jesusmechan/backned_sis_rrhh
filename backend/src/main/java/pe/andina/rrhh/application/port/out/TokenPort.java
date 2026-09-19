package pe.andina.rrhh.application.port.out;

/** Puerto de salida: emisión de access tokens. */
public interface TokenPort {
    String generateAccessToken(Integer idUsuario, String nombreUsuario, String codigoRol);
    long getAccessExpirationMs();
    long getRefreshExpirationMs();
}
