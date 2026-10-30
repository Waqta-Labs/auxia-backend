package com.waqtalabs.auxia.shared.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * V6 projects, once, the emergencies and zones that existed before Citizen Transparency. Its
 * script is run again here on data inserted by hand; it skips what is already projected.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PublicViewProjectionMigrationTests {

  private static final String HASH = "a".repeat(64);
  private static final String TRANSACTION = "0x" + "b".repeat(64);

  @Autowired private JdbcTemplate jdbc;
  @Autowired private DataSource dataSource;

  @Test
  void eachZoneGetsTheStageOfItsLatestPlanAndDelivery() {
    UUID emergency = emergency();
    UUID registered = zone(emergency, "Zona registrada", false);
    UUID prioritized = zone(emergency, "Zona priorizada", true);
    UUID approved = zone(emergency, "Zona aprobada", true);
    UUID approvedPlan = plan(emergency, approved);
    UUID delivered = zone(emergency, "Zona entregada", true);
    UUID deliveredPlan = plan(emergency, delivered);
    UUID delivery = certifiedDelivery(deliveredPlan, delivered);

    runV6();

    String district =
        jdbc.queryForObject(
            "SELECT district FROM citizen_transparency.emergency_public_views"
                + " WHERE emergency_id = ?",
            String.class,
            emergency);
    assertThat(district).isEqualTo("Lurigancho-Chosica");
    assertThat(statusOf(registered)).containsEntry("stage", "REGISTERED");
    assertThat(statusOf(prioritized)).containsEntry("stage", "PRIORITIZED");
    assertThat(statusOf(approved))
        .containsEntry("stage", "AID_APPROVED")
        .containsEntry("current_distribution_plan_id", approvedPlan)
        .containsEntry("deliveries_completed", 0);
    assertThat(statusOf(delivered))
        .containsEntry("stage", "AID_DELIVERED")
        .containsEntry("current_delivery_id", delivery)
        .containsEntry("deliveries_completed", 1)
        .containsEntry("last_delivery_verified", true);
  }

  @Test
  void zoneAlreadyProjectedIsLeftAsItIs() {
    UUID emergency = emergency();
    UUID zone = zone(emergency, "Zona proyectada", true);
    runV6();
    jdbc.update(
        "UPDATE citizen_transparency.zone_public_statuses SET zone_name = 'Sin cambios'"
            + " WHERE zone_id = ?",
        zone);

    runV6();

    assertThat(statusOf(zone)).containsEntry("zone_name", "Sin cambios");
  }

  private void runV6() {
    ScriptUtils.executeSqlScript(
        DataSourceUtils.getConnection(dataSource),
        new ClassPathResource("db/migration/V6__project_public_views.sql"));
  }

  private Map<String, Object> statusOf(UUID zone) {
    return jdbc.queryForMap(
        "SELECT * FROM citizen_transparency.zone_public_statuses WHERE zone_id = ?", zone);
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

  private UUID zone(UUID emergency, String name, boolean scored) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.zones"
            + " (id, emergency_id, name, damage_level, urgency_score, urgency_level,"
            + " urgency_model_version, urgency_calculated_at)"
            + " VALUES (?, ?, ?, 'SEVERE', ?, ?, ?, CASE WHEN ? THEN now() END)",
        id,
        emergency,
        name,
        scored ? 70.0 : null,
        scored ? "HIGH" : null,
        scored ? "rules-v1" : null,
        scored);
    return id;
  }

  private UUID plan(UUID emergency, UUID zone) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO emergency_management.distribution_plans"
            + " (id, emergency_id, zone_id, organization_id, status, coverage, approved_by,"
            + " approved_at) VALUES (?, ?, ?, ?, 'APPROVED', 'FULL', ?, now())",
        id,
        emergency,
        zone,
        UUID.randomUUID(),
        UUID.randomUUID());
    return id;
  }

  private UUID certifiedDelivery(UUID plan, UUID zone) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO traceability.deliveries"
            + " (id, distribution_plan_id, zone_id, brigade_id, organization_id, status,"
            + " registered_by, delivered_at, received_at, record_hash, anchor_status,"
            + " transaction_hash, anchored_at, anchor_attempts)"
            + " VALUES (?, ?, ?, ?, ?, 'CERTIFIED', ?, now(), now(), ?, 'ANCHORED', ?, now(), 1)",
        id,
        plan,
        zone,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        HASH,
        TRANSACTION);
    return id;
  }
}
