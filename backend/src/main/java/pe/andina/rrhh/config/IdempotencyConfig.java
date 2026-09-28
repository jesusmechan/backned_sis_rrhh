package pe.andina.rrhh.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import pe.andina.rrhh.adapter.in.web.idempotency.IdempotencyFilter;
import pe.andina.rrhh.adapter.in.web.idempotency.IdempotencyService;

@Configuration
@EnableConfigurationProperties(IdempotencyProperties.class)
public class IdempotencyConfig {

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration(
            IdempotencyProperties properties,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new IdempotencyFilter(properties, idempotencyService, objectMapper));
        registration.addUrlPatterns("/api/*");
        // Después del rate limit; antes del resto de la cadena de aplicación.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 30);
        registration.setName("idempotencyFilter");
        return registration;
    }
}
