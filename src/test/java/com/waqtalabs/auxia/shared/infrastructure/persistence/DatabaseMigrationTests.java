package com.waqtalabs.auxia.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** On an empty database, Flyway creates the whole schema starting from V1. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseMigrationTests {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void runsEveryMigrationFromTheBaseline() {
    List<String> applied =
        jdbcTemplate.queryForList(
            "SELECT version || ' ' || type FROM public.flyway_schema_history"
                + " WHERE version IS NOT NULL AND success ORDER BY installed_rank",
            String.class);

    assertThat(applied).first().isEqualTo("1 SQL");
    assertThat(applied).allMatch(migration -> migration.endsWith(" SQL"));
  }

  @Test
  void createsTheSchemaOfEveryBoundedContext() {
    List<String> schemas =
        jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name",
            String.class);

    assertThat(schemas)
        .contains(
            "citizen_transparency",
            "emergency_management",
            "identity_access",
            "resource_management",
            "traceability");
  }
}
