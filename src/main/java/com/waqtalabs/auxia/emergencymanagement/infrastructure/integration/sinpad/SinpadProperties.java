package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.sinpad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Connection to SINPAD, under {@code auxia.sinpad}.
 *
 * @param baseUrl URL of the service, without {@code /api/v1}; by default the simulator
 * @param apiKey key of AuxIA, sent in the {@code X-Api-Key} header
 * @param connectTimeout maximum time to open the connection
 * @param readTimeout maximum time to wait for the response
 */
@Validated
@ConfigurationProperties("auxia.sinpad")
record SinpadProperties(
    @NotNull URI baseUrl,
    @NotBlank String apiKey,
    @DefaultValue("2s") Duration connectTimeout,
    @DefaultValue("10s") Duration readTimeout) {}
