package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Limit of new SOS relayed by Mesh per sender, under {@code
 * auxia.citizen-reports.relay-rate-limit}. It is wider than the direct one because one phone with
 * signal uploads the SOS of many neighbors.
 *
 * @param maxReports new relayed SOS that one address can upload in {@code window}
 * @param window period of the limit
 */
@Validated
@ConfigurationProperties("auxia.citizen-reports.relay-rate-limit")
record RelaySosRateLimitProperties(
    @DefaultValue("60") @Positive int maxReports, @DefaultValue("10m") @NotNull Duration window) {}
