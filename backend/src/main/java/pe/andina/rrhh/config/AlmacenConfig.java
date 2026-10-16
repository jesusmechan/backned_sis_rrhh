package pe.andina.rrhh.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AlmacenProperties.class)
public class AlmacenConfig {
}