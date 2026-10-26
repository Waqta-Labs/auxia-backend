package com.waqtalabs.auxia.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * On a database that already has the schema of the report and no Flyway history, as Neon had,
 * V1 is recorded as the baseline without running and only the later migrations run.
 */
@SpringBootTest
@Import(DatabaseBaselineTests.ExistingSchemaConfiguration.class)
class DatabaseBaselineTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void recordsTheExistingSchemaAsTheBaseline() {
    List<String> history =
        jdbcTemplate.queryForList(
            "SELECT version || ' ' || type FROM public.flyway_schema_history"
                + " WHERE version IS NOT NULL AND success ORDER BY installed_rank",
            String.class);

    assertThat(history).first().isEqualTo("1 BASELINE");
    assertThat(history.subList(1, history.size()))
        .allMatch(migration -> migration.endsWith(" SQL"));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ExistingSchemaConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresWithExistingSchema() {
      return new PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
          .withInitScript("db/migration/V1__baseline.sql");
    }
  }
}
