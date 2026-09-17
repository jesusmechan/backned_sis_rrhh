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
import pe.andina.rrhh.domain.enums.EstadoAsiento;
import pe.andina.rrhh.domain.enums.EstadoContrato;
import pe.andina.rrhh.domain.enums.EstadoConvocatoria;
import pe.andina.rrhh.domain.enums.EstadoEmpleado;
import pe.andina.rrhh.domain.enums.EstadoEvaluacion;
import pe.andina.rrhh.domain.enums.EstadoPasoAprobacion;
import pe.andina.rrhh.domain.enums.EstadoPlanilla;
import pe.andina.rrhh.domain.enums.EstadoPostulacion;
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
    public record PermisoFuncionalItem(Integer id, String codigo, String nombre, String modulo) {}

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

    public record CambioPasswordRequest(
            @NotBlank(message = "Indique la contraseña actual") String actual,
            @NotBlank(message = "Indique la nueva contraseña")
            @Size(min = 6, max = 80, message = "La nueva contraseña debe tener entre 6 y 80 caracteres")
            String nueva
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

    public record RolRequest(
            @NotBlank(message = "Indique el código")
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,29}$", message = "El código solo admite mayúsculas, números y guion")
            String codigo,
            @NotBlank(message = "Indique el nombre")
            @Size(max = 80)
            String nombre,
            @Size(max = 250) String descripcion,
            Boolean activo,
            List<Integer> idMenus,
            List<Integer> idPermisos
    ) {}

    public record RolResponse(
            Integer idRol,
            String codigo,
            String nombre,
            String descripcion,
            Boolean activo,
            int usuarios,
            List<Integer> idMenus,
            List<String> menus,
            List<Integer> idPermisos,
            List<String> permisos
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
            @DecimalMin(value = "0", message = "La remuneración no puede ser negativa")
            BigDecimal remuneracionBasica,
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
            BigDecimal remuneracionBasica,
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

    public record PlanillaRequest(
            @NotNull @Min(2020) @Max(2100) Integer anio,
            @NotNull @Min(1) @Max(12) Integer mes,
            @Size(max = 400) String observaciones
    ) {}

    public record PlanillaDetalleResponse(
            Integer idDetalle,
            Integer idEmpleado,
            String empleado,
            String modalidad,
            BigDecimal remuneracionBasica,
            BigDecimal horasExtras,
            BigDecimal montoHorasExtras,
            BigDecimal diasNoLaborados,
            BigDecimal descuentoAusencias,
            BigDecimal onp,
            BigDecimal essalud,
            BigDecimal bruto,
            BigDecimal neto
    ) {}

    public record PlanillaResponse(
            Integer idPlanilla,
            Integer anio,
            Integer mes,
            String periodo,
            EstadoPlanilla estado,
            BigDecimal totalBruto,
            BigDecimal totalDescuentos,
            BigDecimal totalAportes,
            BigDecimal totalNeto,
            String observaciones,
            OffsetDateTime fechaCalculo,
            OffsetDateTime fechaCierre,
            List<PlanillaDetalleResponse> boletas
    ) {}

    public record AsientoLineaResponse(
            Integer idLinea,
            String cuenta,
            String nombreCuenta,
            BigDecimal debe,
            BigDecimal haber
    ) {}

    public record AsientoResponse(
            Integer idAsiento,
            String codigo,
            Integer idPlanilla,
            String periodoPlanilla,
            LocalDate fecha,
            String glosa,
            EstadoAsiento estado,
            BigDecimal totalDebe,
            BigDecimal totalHaber,
            List<AsientoLineaResponse> lineas
    ) {}

    public record EvaluacionRequest(
            @NotNull Integer idEmpleado,
            @NotBlank @Size(max = 20) String periodo,
            LocalDate fecha,
            @NotNull @Min(1) @Max(5) Integer puntualidad,
            @NotNull @Min(1) @Max(5) Integer calidad,
            @NotNull @Min(1) @Max(5) Integer cooperacion,
            @NotNull @Min(1) @Max(5) Integer iniciativa,
            @Size(max = 400) String comentario
    ) {}

    public record EvaluacionResponse(
            Integer idEvaluacion,
            Integer idEmpleado,
            String empleado,
            String evaluador,
            String periodo,
            LocalDate fecha,
            Integer puntualidad,
            Integer calidad,
            Integer cooperacion,
            Integer iniciativa,
            BigDecimal promedio,
            String comentario,
            EstadoEvaluacion estado
    ) {}

    public record ConvocatoriaRequest(
            @NotBlank @Size(max = 120) String puesto,
            Integer idArea,
            @Min(1) Integer vacantes,
            @NotNull LocalDate fechaInicio,
            LocalDate fechaFin,
            @Size(max = 400) String descripcion,
            EstadoConvocatoria estado
    ) {}

    public record ConvocatoriaResponse(
            Integer idConvocatoria,
            String codigo,
            String puesto,
            Integer idArea,
            String area,
            Integer vacantes,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            String descripcion,
            EstadoConvocatoria estado,
            long postulantes
    ) {}

    public record PostulacionRequest(
            @NotBlank @Size(max = 80) String nombres,
            @NotBlank @Size(max = 80) String apellidos,
            @NotBlank @Size(max = 20) String documento,
            @Size(max = 120) String correo,
            @Size(max = 20) String telefono,
            @Min(0) @Max(100) Integer puntaje,
            EstadoPostulacion estado,
            @Size(max = 400) String observacion
    ) {}

    public record PostulacionResponse(
            Integer idPostulacion,
            Integer idConvocatoria,
            String convocatoria,
            String nombres,
            String apellidos,
            String nombreCompleto,
            String documento,
            String correo,
            String telefono,
            Integer puntaje,
            EstadoPostulacion estado,
            String observacion
    ) {}

    public record AreaRequest(
            @NotBlank @Size(max = 80) String nombre,
            @Size(max = 250) String descripcion,
            Boolean activo
    ) {}

    public record AreaResponse(Integer idArea, String nombre, String descripcion, Boolean activo) {}

    public record CargoRequest(
            @NotBlank @Size(max = 80) String nombre,
            @Size(max = 250) String descripcion,
            Boolean activo
    ) {}

    public record CargoResponse(Integer idCargo, String nombre, String descripcion, Boolean activo) {}

    public record HorarioRequest(
            @NotBlank @Size(max = 80) String nombre,
            @NotNull LocalTime horaIngreso,
            @NotNull LocalTime horaSalida,
            @Min(0) @Max(180) Integer minutosRefrigerio,
            Boolean activo
    ) {}

    public record HorarioResponse(
            Integer idHorario,
            String nombre,
            LocalTime horaIngreso,
            LocalTime horaSalida,
            Integer minutosRefrigerio,
            Boolean activo
    ) {}

    public record TipoPermisoRequest(
            @NotBlank @Size(max = 30) String codigo,
            @NotBlank @Size(max = 80) String nombre,
            Boolean requiereSustento,
            Boolean activo
    ) {}

    public record TipoPermisoMaestroResponse(
            Integer idTipoPermiso,
            String codigo,
            String nombre,
            Boolean requiereSustento,
            Boolean activo
    ) {}

    public record ParametroRequest(
            @NotBlank @Size(max = 80) String clave,
            @NotBlank @Size(max = 200) String valor,
            @Size(max = 300) String descripcion
    ) {}

    public record ParametroResponse(String clave, String valor, String descripcion) {}

    public record CuentaRequest(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 80) String nombre,
            @NotBlank @Size(max = 40) String uso,
            @Size(max = 12) String naturaleza,
            Boolean activo
    ) {}

    public record CuentaResponse(
            Integer idCuenta,
            String codigo,
            String nombre,
            String uso,
            String naturaleza,
            Boolean activo
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
