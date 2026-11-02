package com.waqtalabs.auxia.shared.infrastructure.aiservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Connection to auxia-ai-service, under {@code auxia.ai-service}.
 *
 * @param baseUrl URL of the service, without {@code /api/v1}
 * @param apiKey shared secret sent in the {@code X-API-Key} header
 * @param connectTimeout maximum time to open the connection
 * @param readTimeout maximum time to wait for the response
 */
@Validated
@ConfigurationProperties("auxia.ai-service")
public record AiServiceProperties(
    @NotNull URI baseUrl,
    @NotBlank String apiKey,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout) {}
