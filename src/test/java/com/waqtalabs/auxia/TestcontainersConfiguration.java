package com.waqtalabs.auxia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Disposable PostgreSQL for the tests that load the whole application. Import it in every
 * {@code @SpringBootTest}: tests never use Neon.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

  /** Same major version as Neon. */
  private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:18-alpine");

  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer(POSTGRES);
  }
}
