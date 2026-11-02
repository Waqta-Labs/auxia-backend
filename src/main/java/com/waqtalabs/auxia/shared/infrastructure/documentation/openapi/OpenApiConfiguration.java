package com.waqtalabs.auxia.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata shown in Swagger UI ({@code /swagger-ui.html}). Every operation asks for the
 * bearer token returned by {@code POST /api/v1/auth/sign-in}, which Swagger UI sends once it is
 * set in Authorize.
 */
@Configuration
public class OpenApiConfiguration {

  static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  OpenAPI auxiaOpenApi(@Value("${info.app.version}") String version) {
    return new OpenAPI()
        .info(
            new Info()
                .title("AuxIA Backend API")
                .description(
                    "RESTful API of AuxIA for emergency management, resource management, delivery"
                        + " traceability, identity and access, and citizen transparency.")
                .version(version))
        .externalDocs(
            new ExternalDocumentation()
                .description("AuxIA backend repository")
                .url("https://github.com/Waqta-Labs/auxia-backend"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token returned by POST /api/v1/auth/sign-in")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
