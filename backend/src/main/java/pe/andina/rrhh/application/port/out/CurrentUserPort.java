package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.domain.model.Usuario;

/** Puerto de salida: identidad de la sesión actual, sin Spring Security. */
public interface CurrentUserPort {
    Usuario usuario();
    Integer idUsuario();
    Integer idEmpleado();
    String codigoRol();
    boolean hasRole(String codigoRol);
    boolean isAdminOrRrhh();
    boolean puedeVerConjuntoOperativo();
}
