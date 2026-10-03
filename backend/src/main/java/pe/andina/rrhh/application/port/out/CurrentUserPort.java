package pe.andina.rrhh.application.port.out;

import pe.andina.rrhh.domain.model.Permisos;
import pe.andina.rrhh.domain.model.Usuario;

/** Puerto de salida: identidad de la sesión actual, sin Spring Security. */
public interface CurrentUserPort {
    Usuario usuario();
    Integer idUsuario();
    Integer idEmpleado();
    String codigoRol();
    boolean tienePermiso(String codigoPermiso);

    default boolean alcanceTotal() {
        return tienePermiso(Permisos.ALCANCE_TOTAL);
    }

    default boolean alcanceOrganizacion() {
        return alcanceTotal() || tienePermiso(Permisos.ALCANCE_ORGANIZACION);
    }

    default boolean alcanceEquipo() {
        return tienePermiso(Permisos.ALCANCE_EQUIPO);
    }

    /** Ve solicitudes de otros trabajadores (organización completa o su equipo). */
    default boolean veConjuntoOperativo() {
        return alcanceOrganizacion() || alcanceEquipo();
    }
}
