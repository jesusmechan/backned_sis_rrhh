package pe.andina.rrhh.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("RRHH Andina API")
                        .version("1.0.0")
                        .description("""
                                API del Sistema de Gestión de Recursos Humanos — Consultora Contable Andina S.A.C.

                                1. Ejecuta **POST /api/auth/login** (usuario de prueba: `jesus.mechan` / `Andina2026`).
                                2. Copia el `accessToken`.
                                3. Pulsa **Authorize** y pégalo (sin la palabra Bearer).
                                """))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
