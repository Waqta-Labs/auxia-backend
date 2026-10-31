package com.waqtalabs.auxia.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * V9 starts the history of each zone with the decisions the data already kept. Its backfill is run
 * again here on data inserted by hand.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ZoneHistoryMigrationTests {

  private static final String BACKFILL = "-- The decisions taken before this migration";

  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;

  @Test
  void theDecisionsKeptByTheDataStartTheHistoryOfTheirZone() throws IOException {
    UUID authority = UUID.randomUUID();
    UUID emergency = emergency();
    UUID zone = zone(emergency, authority);
    UUID approved = plan(emergency, zone, "APPROVED", authority, null);
    jdbc.update(
        "UPDATE emergency_management.distribution_plans SET modified_by = ?,"
            + " modified_at = now(), modification_reason = 'Solo hay 80' WHERE id = ?",
        authority,
        approved);
    UUID rejected = plan(emergency, zone, "REJECTED", authority, "La carretera está cortada");
    plan(emergency, zone, "PROPOSED", null, null);
    UUID report = correctedReport(emergency, zone, authority);
    correctedReport(emergency, null, authority);

    runBackfill();

    List<Map<String, Object>> history =
        jdbc.queryForList(
            "SELECT type, decided_by, detail, subject_id FROM emergency_management.zone_decisions"
                + " WHERE zone_id = ? ORDER BY type",
            zone);
    assertThat(history)
        .extracting(row -> row.get("type"))
        .containsExactly(
            "MANUAL_PRIORITY_SET",
            "PLAN_APPROVED",
            "PLAN_MODIFIED",
            "PLAN_REJECTED",
            "REPORT_CORRECTED");
    assertThat(history).extracting(row -> row.get("decided_by")).containsOnly(authority);
    assertThat(history)
        .extracting(row -> row.get("subject_id"))
        .containsExactly(null, approved, approved, rejected, report);
    assertThat(history)
        .extracting(row -> row.get("detail"))
        .containsExactly(
            "Riesgo inminente",
            null,
            "Solo hay 80",
            "La carretera está cortada",
            "Conteo en campo");
  }

  private void runBackfill() throws IOException {
    String script =
        new ClassPathResource("db/migration/V9__add_zone_history_and_shifts.sql")
            .getContentAsString(StandardCharsets.UTF_8);
    ScriptUtils.executeSqlScript(
        DataSourceUtils.getConnection(dataSource),
        new ByteArrayResource(
            script.substring(script.indexOf(BACKFILL)).getBytes(StandardCharsets.UTF_8)));
  }

  private UUID emergency() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.emergencies"
            + " (id, name, type, department, province, district, start_date)"
            + " VALUES (?, 'Huaico Chosica 2026', 'LANDSLIDE', 'Lima', 'Lima',"
            + " 'Lurigancho-Chosica', DATE '2026-03-14')",
        id);
    return id;
  }

  private UUID zone(UUID emergency, UUID authority) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.zones"
            + " (id, emergency_id, name, damage_level, manual_priority_by, manual_priority_at,"
            + " manual_priority_reason)"
            + " VALUES (?, ?, 'Sector Alto Perú', 'SEVERE', ?, now(), 'Riesgo inminente')",
        id,
        emergency,
        authority);
    return id;
  }

  private UUID plan(UUID emergency, UUID zone, String status, UUID decidedBy, String reason) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.distribution_plans"
            + " (id, emergency_id, zone_id, organization_id, status, coverage, approved_by,"
            + " approved_at, rejected_reason)"
            + " VALUES (?, ?, ?, ?, ?, 'FULL', ?,"
            + " CASE WHEN CAST(? AS UUID) IS NULL THEN NULL ELSE now() END, ?)",
        id,
        emergency,
        zone,
        UUID.randomUUID(),
        status,
        decidedBy,
        decidedBy,
        reason);
    return id;
  }

  private UUID correctedReport(UUID emergency, UUID zone, UUID authority) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.field_reports"
            + " (id, emergency_id, zone_id, raw_text, source, extracted_indicators,"
            + " corrected_indicators, corrected_by, corrected_at, correction_reason)"
            + " VALUES (?, ?, ?, 'Huaico en Alto Perú', 'AUTHORITY', '{}'::jsonb, '{}'::jsonb,"
            + " ?, now(), 'Conteo en campo')",
        id,
        emergency,
        zone,
        authority);
    return id;
  }
}
