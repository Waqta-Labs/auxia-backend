package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.BrigadeAssignment;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** The JPA adapter stores and rebuilds a brigade with its members and assignments. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaBrigadeRepositoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final Location CHOSICA = new Location(-11.9436, -76.7078);

  @Autowired private BrigadeRepository brigades;
  @Autowired private EntityManager entityManager;

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());

  @Test
  void brigadeIsStoredWithItsMembersAndLaterLosesOne() {
    Brigade brigade = Brigade.create(organization, "Brigada Chosica 1", CHOSICA);
    StaffMember leader = StaffMember.register("Rosa Quispe", StaffRole.LEADER, "987654321");
    StaffMember driver = StaffMember.register("Luis Huamán", StaffRole.DRIVER, null);
    brigade.addMember(leader);
    brigade.addMember(driver);
    brigades.save(brigade);
    clear();

    Brigade found = brigades.findById(brigade.id()).orElseThrow();
    assertThat(found.name()).isEqualTo("Brigada Chosica 1");
    assertThat(found.baseLocation()).isEqualTo(CHOSICA);
    assertThat(found.status()).isEqualTo(BrigadeStatus.AVAILABLE);
    assertThat(found.members())
        .extracting(StaffMember::fullName)
        .containsExactly("Luis Huamán", "Rosa Quispe");
    assertThat(found.members().getLast().phone()).contains("987654321");

    found.removeMember(driver.id());
    brigades.save(found);
    clear();

    assertThat(brigades.findById(brigade.id()).orElseThrow().members())
        .extracting(StaffMember::id)
        .containsExactly(leader.id());
  }

  @Test
  void assignmentIsStoredAndLaterCompleted() {
    Brigade brigade = Brigade.create(organization, "Brigada Chosica 1", CHOSICA);
    brigade.addMember(StaffMember.register("Rosa Quispe", StaffRole.LEADER, null));
    var plan = new DistributionPlanId(UUID.randomUUID());
    var zone = new ZoneId(UUID.randomUUID());
    brigade.assignTo(plan, zone, NOW);
    brigades.save(brigade);
    clear();

    Brigade assigned = brigades.findById(brigade.id()).orElseThrow();
    assertThat(assigned.status()).isEqualTo(BrigadeStatus.ASSIGNED);
    BrigadeAssignment assignment = assigned.currentAssignment().orElseThrow();
    assertThat(assignment.distributionPlanId()).isEqualTo(plan);
    assertThat(assignment.zoneId()).isEqualTo(zone);
    assertThat(assignment.assignedAt()).isEqualTo(NOW);

    assigned.completeAssignment(plan, NOW.plusHours(4));
    brigades.save(assigned);
    clear();

    Brigade completed = brigades.findById(brigade.id()).orElseThrow();
    assertThat(completed.status()).isEqualTo(BrigadeStatus.AVAILABLE);
    assertThat(completed.assignments().getFirst().status()).isEqualTo(AssignmentStatus.COMPLETED);
    assertThat(completed.assignments().getFirst().completedAt()).contains(NOW.plusHours(4));
  }

  @Test
  void brigadesOfAnOrganizationAreOrderedByName() {
    brigades.save(Brigade.create(organization, "Brigada Pedregal", CHOSICA));
    brigades.save(Brigade.create(organization, "Brigada Alto Perú", CHOSICA));
    brigades.save(Brigade.create(new OrganizationId(UUID.randomUUID()), "Otra", CHOSICA));

    assertThat(brigades.findByOrganizationId(organization))
        .extracting(Brigade::name)
        .containsExactly("Brigada Alto Perú", "Brigada Pedregal");
    assertThat(brigades.findById(BrigadeId.generate())).isEmpty();
  }

  private void clear() {
    entityManager.flush();
    entityManager.clear();
  }
}
