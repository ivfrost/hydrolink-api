package dev.ivfrost.hydro_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Hydro API",
                version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@EnableMethodSecurity(prePostEnabled = true)
public class OpenApiConfig {
}