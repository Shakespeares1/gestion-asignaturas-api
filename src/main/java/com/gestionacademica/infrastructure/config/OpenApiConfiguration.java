package com.gestionacademica.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Gestión Académica API", version = "1.0.0",
        description = "API de asignaturas, usuarios, autenticación y reportes."))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfiguration {
    @Bean
    OpenAPI apiWithRelativeServerUrl() {
        return new OpenAPI().servers(List.of(new Server().url("/")));
    }
}
