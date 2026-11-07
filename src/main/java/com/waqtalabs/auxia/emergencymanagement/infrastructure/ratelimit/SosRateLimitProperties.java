package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Limit of new SOS per sender, under {@code auxia.citizen-reports.rate-limit}.
 *
 * @param maxReports new SOS that one address can send in {@code window}
 * @param window period of the limit
 */
@Validated
@ConfigurationProperties("auxia.citizen-reports.rate-limit")
record SosRateLimitProperties(
    @DefaultValue("10") @Positive int maxReports, @DefaultValue("10m") @NotNull Duration window) {}
