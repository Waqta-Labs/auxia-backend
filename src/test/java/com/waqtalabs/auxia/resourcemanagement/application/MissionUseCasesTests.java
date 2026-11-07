package com.waqtalabs.auxia.resourcemanagement.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones.Item;
import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones.MissionZone;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetMyMissionsQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService.Mission;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService.MyMissions;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MissionUseCasesTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 9, 30);
  private static final Location CHOSICA = new Location(-11.9436, -76.7078);

  private final InMemoryBrigadeRepository brigades = new InMemoryBrigadeRepository();
  private final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();
  private final Map<DistributionPlanId, MissionZone> zones = new HashMap<>();
  private final Map<DistributionPlanId, UUID> deliveries = new HashMap<>();
  private final MissionQueryService missions =
      new MissionQueryService(
          brigades,
          inventories,
          (plan, base) -> Optional.ofNullable(zones.get(plan)),
          plan -> Optional.ofNullable(deliveries.get(plan)));

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final UserId rosa = new UserId(UUID.randomUUID());
  private final Inventory inventory = Inventory.create(organization, "Almacén Lima");
  private final ResourceId water =
      inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 0);
  private final ResourceId medicine =
      inventory.registerResource("Botiquín", ResourceCategory.MEDICINE, UnitOfMeasure.KIT, 0);

  @Test
  void brigadistSeesTheMissionInProgressFirstAndThenTheCompletedOnes() {
    inventories.save(inventory);
    Brigade brigade = brigadeOf(rosa);
    DistributionPlanId done = mission(brigade, NOW.minusDays(1), water);
    brigade.completeAssignment(done, NOW.minusHours(20));
    DistributionPlanId current = mission(brigade, NOW, medicine);
    brigades.save(brigade);
    UUID delivery = UUID.randomUUID();
    deliveries.put(current, delivery);

    MyMissions mine = missions.handle(new GetMyMissionsQuery(rosa));

    assertThat(mine.brigade()).contains(brigade);
    assertThat(mine.missions()).extracting(Mission::planId)
        .containsExactly(current.value(), done.value());
    Mission first = mine.missions().getFirst();
    assertThat(first.status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
    assertThat(first.deliveryId()).contains(delivery);
    assertThat(first.category()).isEqualTo("MEDICAL");
    assertThat(first.assignedAt()).isEqualTo(NOW);
    assertThat(first.items())
        .containsExactly(
            new MissionQueryService.Item(medicine, "Botiquín", UnitOfMeasure.KIT, 20));
    Mission second = mine.missions().get(1);
    assertThat(second.status()).isEqualTo(AssignmentStatus.COMPLETED);
    assertThat(second.completedAt()).contains(NOW.minusHours(20));
    assertThat(second.category()).isEqualTo("SUPPLIES");
    assertThat(second.deliveryId()).isEmpty();
  }

  @Test
  void accountOutsideEveryBrigadeHasNoMissions() {
    brigadeOf(new UserId(UUID.randomUUID()));

    assertThat(missions.handle(new GetMyMissionsQuery(rosa)))
        .isEqualTo(new MyMissions(Optional.empty(), List.of()));
  }

  private Brigade brigadeOf(UserId account) {
    Brigade brigade = Brigade.create(organization, "Brigada Alfa", CHOSICA);
    brigade.addMember(StaffMember.register("Rosa Quispe", StaffRole.LEADER, null, account));
    return brigades.save(brigade);
  }

  private DistributionPlanId mission(Brigade brigade, LocalDateTime at, ResourceId resource) {
    var plan = new DistributionPlanId(UUID.randomUUID());
    var zone = new ZoneId(UUID.randomUUID());
    brigade.assignTo(plan, zone, at);
    zones.put(
        plan,
        new MissionZone(
            zone.value(),
            "Sector Santa Rosa",
            94.0,
            "CRITICAL",
            List.of(),
            300,
            3,
            1,
            0,
            -12.0432,
            -77.0428,
            3.2,
            7,
            List.of(new Item(resource, 20))));
    return plan;
  }
}
