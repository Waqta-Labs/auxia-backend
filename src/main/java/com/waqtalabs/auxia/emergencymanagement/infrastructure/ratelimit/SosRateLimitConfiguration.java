package com.waqtalabs.auxia.emergencymanagement.infrastructure.ratelimit;

import com.waqtalabs.auxia.emergencymanagement.application.ports.SosRateLimiter;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Limit of new SOS that one address can send to the public endpoint. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SosRateLimitProperties.class)
class SosRateLimitConfiguration {

  @Bean
  SosRateLimiter sosRateLimiter(SosRateLimitProperties properties, Clock clock) {
    return new InMemorySosRateLimiter(properties.maxReports(), properties.window(), clock);
  }
}
