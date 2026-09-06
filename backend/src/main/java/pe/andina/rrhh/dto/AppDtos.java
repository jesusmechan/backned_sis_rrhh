package pe.andina.rrhh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pe.andina.rrhh.domain.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.enums.SexoEmpleado;
import pe.andina.rrhh.domain.enums.TipoAprobador;
import pe.andina.rrhh.domain.enums.TipoContrato;
import pe.andina.rrhh.domain.enums.TipoDocumento;
import pe.andina.rrhh.domain.enums.TipoMarcacion;
import pe.andina.rrhh.domain.enums.TipoOrigenFlujo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

public final class AppDtos {
    private AppDtos() {}

    public record IdNombre(Integer id, String nombre) {}
    public record CatalogoItem(Integer id, String codigo, String nombre) {}

    public record EmpleadoRequest(
            @NotBlank String codigoEmpleado,
            TipoDocumento tipoDocumento,
            @NotBlank String numeroDocumento,
            @NotBlank String nombres,
            @NotBlank String apellidoPaterno,
            @NotBlank String apellidoMaterno,
            LocalDate fechaNacimiento,
            SexoEmpleado sexo,
            @NotBlank String correoInstitucional,
            String correoPersonal,
            String telefono,
            String direccion,
            @NotNull LocalDate fechaIngreso,
            LocalDate fechaCese,
            @NotNull Integer idArea,
            @NotNull Integer idCargo,
            @NotNull Integer idHorario,
            TipoContrato tipoContrato,
            EstadoEmpleado estado,
            Integer idJefeInmediato
    ) {}

    public record EmpleadoResponse(
            Integer idEmpleado,
            String codigoEmpleado,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String nombres,
            String apellidoPaterno,
            String apellidoMaterno,
            String nombreCompleto,
            LocalDate fechaNacimiento,
            SexoEmpleado sexo,
            String correoInstitucional,
            String correoPersonal,
            String telefono,
            String direccion,
            LocalDate fechaIngreso,
            LocalDate fechaCese,
            Integer idArea,
            String area,
            Integer idCargo,
            String cargo,
            Integer idHorario,
            String horario,
            TipoContrato tipoContrato,
            EstadoEmpleado estado,
            Integer idJefeInmediato,
            String jefeInmediato
    ) {}

    public record UsuarioRequest(
            Integer idEmpleado,
            @NotNull Integer idRol,
            @NotBlank String nombreUsuario,
            @NotBlank String correo,
            String password,
            Boolean activo
    ) {}

    public record UsuarioResponse(
            Integer idUsuario,
            String nombreUsuario,
            String correo,
            Boolean activo,
            OffsetDateTime ultimoAcceso,
            Integer idRol,
            String rol,
            String perfil,
            Integer idEmpleado,
            String nombreCompleto
    ) {}

    public record MenuItemResponse(
            String codigo,
            String etiqueta,
            String ruta,
            String icono,
            String descripcion
    ) {}

    public record MenuGrupoResponse(
            String grupo,
            List<MenuItemResponse> items
    ) {}

    public record MenuAdminRequest(
            @NotBlank String codigo,
            @NotBlank String etiqueta,
            @NotBlank String ruta,
            @NotBlank String icono,
            @NotBlank String grupo,
            String descripcion,
            Integer orden,
            Boolean activo,
            List<Integer> idPerfiles
    ) {}

    public record MenuAdminResponse(
            Integer idMenu,
            String codigo,
            String etiqueta,
            String ruta,
            String icono,
            String grupo,
            String descripcion,
            Integer orden,
            Boolean activo,
            List<Integer> idPerfiles,
            List<String> perfiles
    ) {}

    public record SesionResponse(
            Integer idUsuario,
            String nombreUsuario,
            String correo,
            String rol,
            String perfil,
            Integer idRol,
            Integer idEmpleado,
            String nombreCompleto,
            List<String> permisos,
            List<MenuGrupoResponse> menu
    ) {}

    public record PermisoRequest(
            Integer idEmpleado,
            @NotNull Integer idTipoPermiso,
            @NotNull LocalDate fechaInicio,
            @NotNull LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            @NotBlank String motivo
    ) {}

    public record PermisoResponse(
            Integer idSolicitudPermiso,
            Integer idEmpleado,
            String empleado,
            Integer idTipoPermiso,
            String tipoPermiso,
            String flujo,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            String motivo,
            EstadoSolicitud estado,
            OffsetDateTime fechaCreacion,
            List<PasoResponse> pasos
    ) {}

    public record HoraExtraRequest(
            Integer idEmpleado,
            @NotNull LocalDate fecha,
            @NotNull LocalTime horaInicio,
            @NotNull LocalTime horaFin,
            @NotNull BigDecimal cantidadHoras,
            @NotBlank String motivo
    ) {}

    public record HoraExtraResponse(
            Integer idSolicitudHoraExtra,
            Integer idEmpleado,
            String empleado,
            String flujo,
            LocalDate fecha,
            LocalTime horaInicio,
            LocalTime horaFin,
            BigDecimal cantidadHoras,
            String motivo,
            EstadoSolicitud estado,
            OffsetDateTime fechaCreacion,
            List<PasoResponse> pasos
    ) {}

    public record PasoResponse(
            Integer idPasoSolicitud,
            Integer numeroPaso,
            String nombrePaso,
            TipoAprobador tipoAprobador,
            String rol,
            String usuarioAsignado,
            EstadoPasoAprobacion estado,
            String usuarioDecision,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaDecision,
            String comentario
    ) {}

    public record DecisionRequest(@NotBlank String comentario) {}

    public record BandejaItem(
            Integer idPasoSolicitud,
            String tipoSolicitud,
            Integer idSolicitud,
            String solicitante,
            String tipoTramite,
            Integer numeroPaso,
            String nombrePaso,
            TipoAprobador tipoAprobador,
            String motivo,
            OffsetDateTime fechaInicio
    ) {}

    public record MarcacionRequest(
            Integer idEmpleado,
            @NotNull TipoMarcacion tipo,
            OffsetDateTime fechaHora,
            String origen,
            String observacion
    ) {}

    public record MarcacionResponse(
            Integer idMarcacion,
            Integer idEmpleado,
            String empleado,
            TipoMarcacion tipo,
            OffsetDateTime fechaHora,
            LocalDate fecha,
            String origen,
            String observacion
    ) {}

    public record FlujoRequest(
            @NotBlank String codigo,
            @NotBlank String nombre,
            @NotNull TipoOrigenFlujo tipoOrigen,
            Integer idTipoPermiso,
            String descripcion,
            Boolean activo,
            List<FlujoPasoRequest> pasos
    ) {}

    public record FlujoPasoRequest(
            @NotNull Integer numeroPaso,
            @NotBlank String nombrePaso,
            @NotNull TipoAprobador tipoAprobador,
            Integer idRol,
            Integer idUsuario,
            Boolean esObligatorio,
            String rol,
            String usuario
    ) {}

    public record FlujoResponse(
            Integer idConfiguracion,
            String codigo,
            String nombre,
            TipoOrigenFlujo tipoOrigen,
            Integer idTipoPermiso,
            String tipoPermiso,
            String descripcion,
            Boolean activo,
            List<FlujoPasoRequest> pasos
    ) {}

    public record HistorialResponse(
            Integer idHistorial,
            String accion,
            String estadoAnterior,
            String estadoNuevo,
            String usuario,
            String comentario,
            OffsetDateTime fechaHora
    ) {}

    public record AuditoriaResponse(
            Long idAuditoria,
            String usuario,
            String accion,
            String entidad,
            Integer idEntidad,
            String detalle,
            OffsetDateTime fechaHora
    ) {}

    public record CargaResponse(
            Integer idCarga,
            String nombreArchivo,
            Integer totalFilas,
            Integer filasExitosas,
            Integer filasFallidas,
            String estado,
            String mensaje,
            List<CargaFilaResponse> detalle
    ) {}

    public record CargaFilaResponse(Integer numeroFila, String resultado, String mensaje) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
