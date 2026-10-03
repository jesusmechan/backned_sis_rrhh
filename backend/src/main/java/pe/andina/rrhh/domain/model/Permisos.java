package pe.andina.rrhh.domain.model;

/**
 * Códigos de {@code permiso_funcional} que el código consulta. Qué perfil tiene cada permiso
 * se decide en el mantenedor de Roles, no aquí.
 */
public final class Permisos {

    private Permisos() {}

    public static final String PERSONAL_REGISTRAR = "PERSONAL_REGISTRAR";
    public static final String PERSONAL_ACTUALIZAR = "PERSONAL_ACTUALIZAR";
    public static final String PERSONAL_CONSULTAR = "PERSONAL_CONSULTAR";
    public static final String PERSONAL_CARGAR_EXCEL = "PERSONAL_CARGAR_EXCEL";
    public static final String ASISTENCIA_GESTIONAR = "ASISTENCIA_GESTIONAR";
    public static final String CONTRATO_GESTIONAR = "CONTRATO_GESTIONAR";
    public static final String DESEMPENO_REGISTRAR = "DESEMPENO_REGISTRAR";
    public static final String REPORTE_PERMISOS = "REPORTE_PERMISOS";
    public static final String REPORTE_HORAS_EXTRAS = "REPORTE_HORAS_EXTRAS";
    public static final String REPORTE_ASISTENCIA = "REPORTE_ASISTENCIA";
    public static final String REPORTE_USUARIOS = "REPORTE_USUARIOS";
    public static final String ROL_GESTIONAR = "ROL_GESTIONAR";
    public static final String MENU_GESTIONAR = "MENU_GESTIONAR";

    /** Ve y gestiona a cualquier trabajador. */
    public static final String ALCANCE_TOTAL = "ALCANCE_TOTAL";
    /** Ve a toda la organización en solicitudes y desempeño. */
    public static final String ALCANCE_ORGANIZACION = "ALCANCE_ORGANIZACION";
    /** Ve a su equipo directo. */
    public static final String ALCANCE_EQUIPO = "ALCANCE_EQUIPO";
}
