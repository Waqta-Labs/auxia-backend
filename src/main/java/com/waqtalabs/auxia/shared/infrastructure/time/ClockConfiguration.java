package com.waqtalabs.auxia.shared.infrastructure.time;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The use cases read the time from this clock, so tests can fix it. */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

  /** Time zone of the JVM, the same one Hibernate uses for created_at and updated_at. */
  @Bean
  Clock clock() {
    return Clock.systemDefaultZone();
  }
}
