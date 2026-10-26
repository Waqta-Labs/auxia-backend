package com.waqtalabs.auxia.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata shown in Swagger UI ({@code /swagger-ui.html}). */
@Configuration
public class OpenApiConfiguration {

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
                .url("https://github.com/Waqta-Labs/auxia-backend"));
  }
}
