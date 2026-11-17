package pe.andina.rrhh.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Al levantar la API regenera el dashboard HTML con los XML de la última corrida de tests.
 * En producción (sin {@code target/*-reports}) no hace nada útil: queda un HTML vacío o previo.
 */
@Component
@Order(100)
public class SecurityTestReportOnStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityTestReportOnStartup.class);

    @Value("${app.security-report.on-startup:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            SecurityTestReportGenerator.generate();
        } catch (Exception ex) {
            log.warn("No se pudo regenerar el reporte de seguridad al arrancar: {}", ex.getMessage());
        }
    }
}