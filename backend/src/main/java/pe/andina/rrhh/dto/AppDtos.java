package pe.andina.rrhh.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.andina.rrhh.domain.enums.EstadoContrato;
import pe.andina.rrhh.domain.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.enums.EstadoSolicitud;
import pe.andina.rrhh.domain.enums.ModalidadContrato;
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
            @NotBlank(message = "Indique el código")
            @Pattern(regexp = "^[A-Z]{2,8}-[0-9]{3,6}$", message = "El código debe tener el formato AND-001")
            String codigoEmpleado,
            TipoDocumento tipoDocumento,
            @NotBlank(message = "Indique el documento")
            @Pattern(regexp = "^[A-Z0-9]{8,12}$", message = "El documento solo admite letras y números")
            String numeroDocumento,
            @NotBlank(message = "Indique los nombres")
            @Pattern(regexp = "^\\p{L}+(?:[ '\\-]\\p{L}+)*$", message = "Los nombres solo admiten letras")
            String nombres,
            @NotBlank(message = "Indique el apellido paterno")
            @Pattern(regexp = "^\\p{L}+(?:[ '\\-]\\p{L}+)*$", message = "El apellido paterno solo admite letras")
            String apellidoPaterno,
            @Pattern(regexp = "^$|^\\p{L}+(?:[ '\\-]\\p{L}+)*$", message = "El apellido materno solo admite letras")
            String apellidoMaterno,
            LocalDate fechaNacimiento,
            SexoEmpleado sexo,
            @NotBlank(message = "Indique el correo institucional")
            @Email(message = "Indique un correo institucional válido")
            @Size(max = 120)
            String correoInstitucional,
            @Email(message = "Indique un correo personal válido")
            @Size(max = 120)
            String correoPersonal,
            @Pattern(regexp = "^$|^\\d{7,9}$", message = "El teléfono debe tener entre 7 y 9 dígitos")
            String telefono,
            @Size(max = 200)
            @Pattern(regexp = "^[\\p{L}0-9\\s.,#\\-/°]*$", message = "La dirección contiene caracteres no permitidos")
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
            @NotNull(message = "Seleccione el perfil") Integer idRol,
            @NotBlank(message = "Indique el usuario")
            @Pattern(regexp = "^[a-z][a-z0-9._]{2,59}$", message = "El usuario solo admite minúsculas, números, punto o guion bajo")
            String nombreUsuario,
            @NotBlank(message = "Indique el correo")
            @Email(message = "Indique un correo válido")
            String correo,
            @Pattern(regexp = "^$|^.{6,80}$", message = "La contraseña debe tener al menos 6 caracteres")
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
            @NotBlank(message = "Indique el código")
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,39}$", message = "El código solo admite mayúsculas, números y guion")
            String codigo,
            @NotBlank(message = "Indique la etiqueta")
            @Pattern(regexp = "^[\\p{L}0-9 ._-]{2,80}$", message = "La etiqueta solo admite letras y números")
            String etiqueta,
            @NotBlank(message = "Indique la ruta")
            @Pattern(regexp = "^/[a-z0-9/_-]*$", message = "La ruta debe iniciar con / y usar minúsculas, números, guion o barra")
            String ruta,
            @NotBlank(message = "Seleccione el icono") String icono,
            @NotBlank(message = "Seleccione el grupo") String grupo,
            @Size(max = 200) String descripcion,
            @Min(value = 0, message = "El orden debe ser un número")
            @Max(value = 9999, message = "El orden no puede superar 9999")
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
            @NotNull(message = "Seleccione el tipo de permiso") Integer idTipoPermiso,
            @NotNull(message = "Indique la fecha de inicio") LocalDate fechaInicio,
            @NotNull(message = "Indique la fecha de fin") LocalDate fechaFin,
            LocalTime horaInicio,
            LocalTime horaFin,
            @NotBlank(message = "Indique el motivo")
            @Size(min = 5, max = 400, message = "El motivo debe tener entre 5 y 400 caracteres")
            String motivo
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
            @NotNull(message = "Indique la fecha") LocalDate fecha,
            @NotNull(message = "Indique la hora de inicio") LocalTime horaInicio,
            @NotNull(message = "Indique la hora de fin") LocalTime horaFin,
            @NotNull(message = "Indique la cantidad de horas")
            @DecimalMin(value = "0.5", message = "La cantidad mínima es 0.5 horas")
            @DecimalMax(value = "8", message = "La cantidad máxima es 8 horas")
            BigDecimal cantidadHoras,
            @NotBlank(message = "Indique el motivo")
            @Size(min = 5, max = 400, message = "El motivo debe tener entre 5 y 400 caracteres")
            String motivo
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

    public record DecisionRequest(
            @NotBlank(message = "Indique el comentario")
            @Size(max = 400, message = "El comentario no puede superar 400 caracteres")
            String comentario
    ) {}

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
            OffsetDateTime fechaInicio,
            String estadoSolicitud,
            boolean puedeDecidir
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
            @NotBlank(message = "Indique el código")
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,39}$", message = "El código solo admite mayúsculas, números y guion")
            String codigo,
            @NotBlank(message = "Indique el nombre")
            @Pattern(regexp = "^[\\p{L}0-9 ._-]{2,120}$", message = "El nombre solo admite letras y números")
            String nombre,
            @NotNull(message = "Seleccione el origen") TipoOrigenFlujo tipoOrigen,
            Integer idTipoPermiso,
            @Size(max = 200) String descripcion,
            Boolean activo,
            @Valid List<FlujoPasoRequest> pasos
    ) {}

    public record FlujoPasoRequest(
            @NotNull Integer numeroPaso,
            @NotBlank(message = "Indique el nombre del paso")
            @Pattern(regexp = "^[\\p{L}0-9 ._-]{2,80}$", message = "El nombre del paso solo admite letras y números")
            String nombrePaso,
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

    public record ContratoRequest(
            Integer idEmpleado,
            @NotNull ModalidadContrato modalidad,
            @NotNull Integer idHorario,
            @NotNull LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoContrato estado,
            @Size(max = 400, message = "Las observaciones no pueden superar 400 caracteres")
            String observaciones
    ) {}

    public record ContratoResponse(
            Integer idContrato,
            String codigo,
            Integer idEmpleado,
            String empleado,
            String codigoEmpleado,
            ModalidadContrato modalidad,
            Integer idHorario,
            String horario,
            String horaIngreso,
            String horaSalida,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            EstadoContrato estado,
            String observaciones,
            VacacionSaldoResponse vacaciones
    ) {}

    public record VacacionSaldoResponse(
            Integer idEmpleado,
            String empleado,
            ModalidadContrato modalidad,
            LocalDate fechaInicioAcumulacion,
            int mesesCompletos,
            BigDecimal tasaMensual,
            BigDecimal diasGanados,
            BigDecimal diasUsados,
            BigDecimal diasDisponibles
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
