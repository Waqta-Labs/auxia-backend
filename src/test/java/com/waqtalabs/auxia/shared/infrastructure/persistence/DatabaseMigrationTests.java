package com.waqtalabs.auxia.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** On an empty database, Flyway creates the whole schema starting from V1. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
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

  @Test
  void zonePopulationIsOptional() {
    UUID emergencyId = insertEmergency();

    assertThatCode(() -> insertZone(emergencyId, null, 140)).doesNotThrowAnyException();
    assertThatCode(() -> insertZone(emergencyId, 600, 140)).doesNotThrowAnyException();
  }

  @Test
  void zonePopulationMustBePositive() {
    UUID emergencyId = insertEmergency();

    assertThatThrownBy(() -> insertZone(emergencyId, 0, 0))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("chk_zone_population");
  }

  @Test
  void peopleAffectedCannotExceedZonePopulation() {
    UUID emergencyId = insertEmergency();

    assertThatThrownBy(() -> insertZone(emergencyId, 100, 140))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("chk_zone_people_affected_population");
  }

  private UUID insertEmergency() {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO emergency_management.emergencies"
            + " (id, name, type, department, province, district, start_date)"
            + " VALUES (?, 'Huaico Chosica 2026', 'LANDSLIDE', 'Lima', 'Lima', 'Chosica',"
            + " DATE '2026-03-14')",
        id);
    return id;
  }

  private UUID insertZone(UUID emergencyId, Integer population, int peopleAffected) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO emergency_management.zones"
            + " (id, emergency_id, name, damage_level, population, people_affected)"
            + " VALUES (?, ?, 'Sector Alto Peru', 'SEVERE', ?, ?)",
        id,
        emergencyId,
        population,
        peopleAffected);
    return id;
  }
}
