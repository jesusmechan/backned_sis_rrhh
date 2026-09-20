package pe.andina.rrhh.domain.service;

import pe.andina.rrhh.domain.model.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.model.enums.TipoAprobador;

import java.util.EnumMap;
import java.util.Map;

public final class BandejaAsignacion {

    @FunctionalInterface
    public interface Estrategia {
        boolean corresponde(SolicitudPasoAprobacion paso, Integer idUsuario, String codigoRol);
    }

    private static final Map<TipoAprobador, Estrategia> ESTRATEGIAS = new EnumMap<>(TipoAprobador.class);

    static {
        ESTRATEGIAS.put(TipoAprobador.JEFE_INMEDIATO, BandejaAsignacion::asignadoAlUsuario);
        ESTRATEGIAS.put(TipoAprobador.USUARIO, BandejaAsignacion::asignadoAlUsuario);
        ESTRATEGIAS.put(TipoAprobador.ROL, BandejaAsignacion::mismoRol);
    }

    private BandejaAsignacion() {}

    public static boolean corresponde(SolicitudPasoAprobacion paso, Integer idUsuario, String codigoRol) {
        if (paso == null || paso.getTipoAprobador() == null || idUsuario == null) {
            return false;
        }
        Estrategia estrategia = ESTRATEGIAS.get(paso.getTipoAprobador());
        return estrategia != null && estrategia.corresponde(paso, idUsuario, codigoRol);
    }

    private static boolean asignadoAlUsuario(SolicitudPasoAprobacion paso, Integer idUsuario, String codigoRol) {
        return paso.getUsuarioAsignado() != null
                && paso.getUsuarioAsignado().getIdUsuario().equals(idUsuario);
    }

    private static boolean mismoRol(SolicitudPasoAprobacion paso, Integer idUsuario, String codigoRol) {
        return paso.getRol() != null
                && codigoRol != null
                && (paso.getRol().getCodigo().equals(codigoRol)
                    || ("GERENCIA".equals(paso.getRol().getCodigo()) && "ADMIN".equals(codigoRol)));
    }
}
