package pe.andina.rrhh.service.aprobacion;

import pe.andina.rrhh.domain.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.enums.TipoAprobador;
import pe.andina.rrhh.security.UsuarioPrincipal;

import java.util.EnumMap;
import java.util.Map;

public final class BandejaAsignacion {

    @FunctionalInterface
    public interface Estrategia {
        boolean corresponde(SolicitudPasoAprobacion paso, UsuarioPrincipal usuario);
    }

    private static final Map<TipoAprobador, Estrategia> ESTRATEGIAS = new EnumMap<>(TipoAprobador.class);

    static {
        ESTRATEGIAS.put(TipoAprobador.JEFE_INMEDIATO, BandejaAsignacion::asignadoAlUsuario);
        ESTRATEGIAS.put(TipoAprobador.USUARIO, BandejaAsignacion::asignadoAlUsuario);
        ESTRATEGIAS.put(TipoAprobador.ROL, BandejaAsignacion::mismoRol);
    }

    private BandejaAsignacion() {}

    public static boolean corresponde(SolicitudPasoAprobacion paso, UsuarioPrincipal usuario) {
        if (paso == null || paso.getTipoAprobador() == null || usuario == null) {
            return false;
        }
        Estrategia estrategia = ESTRATEGIAS.get(paso.getTipoAprobador());
        return estrategia != null && estrategia.corresponde(paso, usuario);
    }

    private static boolean asignadoAlUsuario(SolicitudPasoAprobacion paso, UsuarioPrincipal usuario) {
        return paso.getUsuarioAsignado() != null
                && paso.getUsuarioAsignado().getIdUsuario().equals(usuario.getIdUsuario());
    }

    private static boolean mismoRol(SolicitudPasoAprobacion paso, UsuarioPrincipal usuario) {
        return paso.getRol() != null && paso.getRol().getCodigo().equals(usuario.getCodigoRol());
    }
}
