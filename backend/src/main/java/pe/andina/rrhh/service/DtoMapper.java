package pe.andina.rrhh.service;

import pe.andina.rrhh.domain.Auditoria;
import pe.andina.rrhh.domain.ConfiguracionAprobacion;
import pe.andina.rrhh.domain.ConfiguracionAprobacionDetalle;
import pe.andina.rrhh.domain.Empleado;
import pe.andina.rrhh.domain.HistorialSolicitud;
import pe.andina.rrhh.domain.Marcacion;
import pe.andina.rrhh.domain.SolicitudHoraExtra;
import pe.andina.rrhh.domain.SolicitudPasoAprobacion;
import pe.andina.rrhh.domain.SolicitudPermiso;
import pe.andina.rrhh.domain.Usuario;
import pe.andina.rrhh.dto.AppDtos.AuditoriaResponse;
import pe.andina.rrhh.dto.AppDtos.EmpleadoResponse;
import pe.andina.rrhh.dto.AppDtos.FlujoPasoRequest;
import pe.andina.rrhh.dto.AppDtos.FlujoResponse;
import pe.andina.rrhh.dto.AppDtos.HistorialResponse;
import pe.andina.rrhh.dto.AppDtos.HoraExtraResponse;
import pe.andina.rrhh.dto.AppDtos.MarcacionResponse;
import pe.andina.rrhh.dto.AppDtos.PasoResponse;
import pe.andina.rrhh.dto.AppDtos.PermisoResponse;
import pe.andina.rrhh.dto.AppDtos.UsuarioResponse;

import java.util.List;

public final class DtoMapper {
    private DtoMapper() {}

    public static EmpleadoResponse empleado(Empleado e) {
        return new EmpleadoResponse(
                e.getIdEmpleado(), e.getCodigoEmpleado(), e.getTipoDocumento(), e.getNumeroDocumento(),
                e.getNombres(), e.getApellidoPaterno(), e.getApellidoMaterno(), e.nombreCompleto(),
                e.getFechaNacimiento(), e.getSexo(), e.getCorreoInstitucional(), e.getCorreoPersonal(),
                e.getTelefono(), e.getDireccion(), e.getFechaIngreso(), e.getFechaCese(),
                e.getArea().getIdArea(), e.getArea().getNombre(),
                e.getCargo().getIdCargo(), e.getCargo().getNombre(),
                e.getHorario().getIdHorario(), e.getHorario().getNombre(),
                e.getTipoContrato(), e.getEstado(),
                e.getJefeInmediato() != null ? e.getJefeInmediato().getIdEmpleado() : null,
                e.getJefeInmediato() != null ? e.getJefeInmediato().nombreCompleto() : null
        );
    }

    public static UsuarioResponse usuario(Usuario u) {
        return new UsuarioResponse(
                u.getIdUsuario(), u.getNombreUsuario(), u.getCorreo(), u.getActivo(), u.getUltimoAcceso(),
                u.getRol().getIdRol(), u.getRol().getCodigo(),
                u.getEmpleado() != null ? u.getEmpleado().getIdEmpleado() : null,
                u.getEmpleado() != null ? u.getEmpleado().nombreCompleto() : null
        );
    }

    public static PasoResponse paso(SolicitudPasoAprobacion p) {
        return new PasoResponse(
                p.getIdPasoSolicitud(), p.getNumeroPaso(), p.getNombrePaso(), p.getTipoAprobador(),
                p.getRol() != null ? p.getRol().getNombre() : null,
                p.getUsuarioAsignado() != null ? p.getUsuarioAsignado().getNombreUsuario() : null,
                p.getEstado(),
                p.getUsuarioDecision() != null ? p.getUsuarioDecision().getNombreUsuario() : null,
                p.getFechaInicio(), p.getFechaDecision(), p.getComentario()
        );
    }

    public static PermisoResponse permiso(SolicitudPermiso s, List<SolicitudPasoAprobacion> pasos) {
        return new PermisoResponse(
                s.getIdSolicitudPermiso(),
                s.getEmpleado().getIdEmpleado(), s.getEmpleado().nombreCompleto(),
                s.getTipoPermiso().getIdTipoPermiso(), s.getTipoPermiso().getNombre(),
                s.getConfiguracion() != null ? s.getConfiguracion().getNombre() : null,
                s.getFechaInicio(), s.getFechaFin(), s.getHoraInicio(), s.getHoraFin(),
                s.getMotivo(), s.getEstado(), s.getFechaCreacion(),
                pasos.stream().map(DtoMapper::paso).toList()
        );
    }

    public static HoraExtraResponse horaExtra(SolicitudHoraExtra s, List<SolicitudPasoAprobacion> pasos) {
        return new HoraExtraResponse(
                s.getIdSolicitudHoraExtra(),
                s.getEmpleado().getIdEmpleado(), s.getEmpleado().nombreCompleto(),
                s.getConfiguracion() != null ? s.getConfiguracion().getNombre() : null,
                s.getFecha(), s.getHoraInicio(), s.getHoraFin(), s.getCantidadHoras(),
                s.getMotivo(), s.getEstado(), s.getFechaCreacion(),
                pasos.stream().map(DtoMapper::paso).toList()
        );
    }

    public static MarcacionResponse marcacion(Marcacion m) {
        return new MarcacionResponse(
                m.getIdMarcacion(), m.getEmpleado().getIdEmpleado(), m.getEmpleado().nombreCompleto(),
                m.getTipo(), m.getFechaHora(), m.getFecha(), m.getOrigen(), m.getObservacion()
        );
    }

    public static FlujoResponse flujo(ConfiguracionAprobacion c, List<ConfiguracionAprobacionDetalle> pasos) {
        return new FlujoResponse(
                c.getIdConfiguracion(), c.getCodigo(), c.getNombre(), c.getTipoOrigen(),
                c.getTipoPermiso() != null ? c.getTipoPermiso().getIdTipoPermiso() : null,
                c.getTipoPermiso() != null ? c.getTipoPermiso().getNombre() : null,
                c.getDescripcion(), c.getActivo(),
                pasos.stream().map(p -> new FlujoPasoRequest(
                        p.getNumeroPaso(), p.getNombrePaso(), p.getTipoAprobador(),
                        p.getRol() != null ? p.getRol().getIdRol() : null,
                        p.getUsuario() != null ? p.getUsuario().getIdUsuario() : null,
                        p.getEsObligatorio()
                )).toList()
        );
    }

    public static HistorialResponse historial(HistorialSolicitud h) {
        return new HistorialResponse(
                h.getIdHistorial(), h.getAccion().name(),
                h.getEstadoAnterior() != null ? h.getEstadoAnterior().name() : null,
                h.getEstadoNuevo().name(),
                h.getUsuario().getNombreUsuario(),
                h.getComentario(), h.getFechaHora()
        );
    }

    public static AuditoriaResponse auditoria(Auditoria a) {
        return new AuditoriaResponse(
                a.getIdAuditoria(),
                a.getUsuario() != null ? a.getUsuario().getNombreUsuario() : null,
                a.getAccion(), a.getEntidad(), a.getIdEntidad(), a.getDetalle(), a.getFechaHora()
        );
    }
}
