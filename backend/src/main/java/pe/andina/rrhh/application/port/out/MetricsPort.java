package pe.andina.rrhh.application.port.out;

/**
 * Puerto de observabilidad de negocio (contadores y tiempos).
 * La implementación concreta usa Micrometer → Prometheus.
 */
public interface MetricsPort {

    /** Login exitoso o fallido (reason: ok, bad_credentials, inactive, unknown_user). */
    void login(String result, String reason);

    /** Solicitud creada: tipo = PERMISO | HORA_EXTRA. */
    void solicitudCreada(String tipo);

    /**
     * Decisión de aprobación.
     * decision = APROBAR | RECHAZAR | DEVOLVER | COMPLETAR; origen = PERMISO | HORA_EXTRA.
     */
    void aprobacionDecision(String decision, String origen);

    /** Acción de planilla: crear | calcular | cerrar. */
    void planillaAccion(String accion);

    /** Duración del cálculo de planilla en milisegundos. */
    void planillaCalculoDuration(long millis);

    /** Marcación de asistencia (tipo e origen). */
    void asistenciaMarcacion(String tipo, String origen);

    /** Request rechazado por rate limit (scope: api | auth). */
    void rateLimitRejected(String scope);
}
