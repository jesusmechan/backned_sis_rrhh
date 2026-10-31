package pe.andina.rrhh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI(@Value("${PUBLIC_API_URL:http://localhost:8080}") String publicApiUrl) {
        return new OpenAPI()
                .info(new Info()
                        .title("RRHH Andina API")
                        .version("1.0.0")
                        .description("""
                                API del Sistema de Gestión de Recursos Humanos.

                                1. Ejecuta **POST /api/auth/login** con sus credenciales.
                                2. Copia el `accessToken`.
                                3. Pulsa **Authorize** y pégalo (sin la palabra Bearer).
                                """))
                .servers(List.of(new Server().url(publicApiUrl).description("API pública")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
