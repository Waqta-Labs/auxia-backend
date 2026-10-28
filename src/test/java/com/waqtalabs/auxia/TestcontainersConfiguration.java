package com.waqtalabs.auxia;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Disposable PostgreSQL and Azure Storage for the tests that load the whole application. Import
 * it in every {@code @SpringBootTest}: tests never use Neon nor a real storage account.
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

  /** Evidence files of the deliveries. */
  @Bean
  GenericContainer<?> azuriteContainer() {
    return Azurite.container();
  }

  @Bean
  DynamicPropertyRegistrar evidenceStorageProperties(GenericContainer<?> azuriteContainer) {
    return registry ->
        registry.add(
            "auxia.evidence-storage.connection-string",
            () -> Azurite.connectionString(azuriteContainer));
  }
}
