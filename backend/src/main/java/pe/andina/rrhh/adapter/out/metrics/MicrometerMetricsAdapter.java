package pe.andina.rrhh.adapter.out.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.application.port.out.MetricsPort;

/**
 * Publica métricas de dominio vía Micrometer (endpoint /actuator/prometheus).
 */
@Component
public class MicrometerMetricsAdapter implements MetricsPort {

    private final MeterRegistry registry;

    public MicrometerMetricsAdapter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void login(String result, String reason) {
        registry.counter("rrhh.auth.login", "result", safe(result), "reason", safe(reason)).increment();
    }

    @Override
    public void solicitudCreada(String tipo) {
        registry.counter("rrhh.solicitud.creada", "tipo", safe(tipo)).increment();
    }

    @Override
    public void aprobacionDecision(String decision, String origen) {
        registry.counter("rrhh.aprobacion.decision", "decision", safe(decision), "origen", safe(origen)).increment();
    }

    @Override
    public void planillaAccion(String accion) {
        registry.counter("rrhh.planilla.accion", "accion", safe(accion)).increment();
    }

    @Override
    public void planillaCalculoDuration(long millis) {
        Timer.builder("rrhh.planilla.calculo")
                .description("Duración del cálculo de planilla")
                .publishPercentileHistogram()
                .register(registry)
                .record(Math.max(0L, millis), TimeUnit.MILLISECONDS);
    }

    @Override
    public void asistenciaMarcacion(String tipo, String origen) {
        registry.counter("rrhh.asistencia.marcacion", "tipo", safe(tipo), "origen", safe(origen)).increment();
    }

    @Override
    public void rateLimitRejected(String scope) {
        registry.counter("rrhh.rate_limit.rejected", "scope", safe(scope)).increment();
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
