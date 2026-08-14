package dev.ivfrost.hydro_backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import java.util.Collections;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Hydro API", version = "v1"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@EnableMethodSecurity(prePostEnabled = true)
public class OpenApiConfig {

  /**
   * Public, client-facing API spec.
   */
  @Bean
  public GroupedOpenApi publicApi() {
    return GroupedOpenApi.builder()
        .group("public")
        .pathsToMatch("/v1/**")
        .pathsToExclude("/v1/internal/**")
        .build();
  }

  /**
   * Internal-only API spec for MQTT broker callbacks and device provisioning.
   */
  @Bean
  public GroupedOpenApi internalApi() {
    return GroupedOpenApi.builder()
        .group("internal")
        .pathsToMatch("/v1/internal/**")
        .addOpenApiCustomizer(openApi -> {
          openApi.setInfo(new io.swagger.v3.oas.models.info.Info()
              .title("Hydro API - Internal")
              .version("v1")
              .description("Internal endpoints for MQTT broker callbacks and device provisioning. "
                  + "Not intended for public API consumers."));
          // Internal endpoints use a raw provisioning bearer token, not the user-facing JWT
          // bearerAuth scheme, so drop the global security requirement for this spec.
          openApi.setSecurity(Collections.emptyList());
        })
        .build();
  }
}