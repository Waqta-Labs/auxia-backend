package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Reservation;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockLevel;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The JPA adapter stores and rebuilds an inventory with its stock items, reservations and
 * reservation lines. Flushing and clearing the persistence context makes every read come from
 * the database.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaInventoryRepositoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);

  @Autowired private InventoryRepository inventories;
  @Autowired private EntityManager entityManager;

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());

  @Test
  void inventoryIsStoredWithItsResourcesOrderedByName() {
    Inventory inventory = Inventory.create(organization, "Almacén Central Lima");
    ResourceId water =
        inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 50);
    inventory.registerResource("Carpas", ResourceCategory.SHELTER, UnitOfMeasure.UNIT, 5);
    inventory.restock(water, 120);
    inventories.save(inventory);
    clear();

    Inventory found = inventories.findById(inventory.id()).orElseThrow();

    assertThat(found.organizationId()).isEqualTo(organization);
    assertThat(found.name()).isEqualTo("Almacén Central Lima");
    assertThat(found.stockItems())
        .extracting(StockItem::name)
        .containsExactly("Agua 2.5 L", "Carpas");
    StockItem item = found.stockItem(water);
    assertThat(item.category()).isEqualTo(ResourceCategory.WATER);
    assertThat(item.unit()).isEqualTo(UnitOfMeasure.UNIT);
    assertThat(item.minimumStock()).isEqualTo(50);
    assertThat(item.stockLevel()).isEqualTo(new StockLevel(120, 0));
    assertThat(inventories.findByOrganizationId(organization)).isPresent();
  }

  @Test
  void reservationIsStoredWithItsLinesChangedAndLaterReleased() {
    Inventory inventory = Inventory.create(organization, "Almacén Central Lima");
    ResourceId water =
        inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 0);
    ResourceId food =
        inventory.registerResource("Kit de alimentos", ResourceCategory.FOOD, UnitOfMeasure.KIT, 0);
    inventory.restock(water, 100);
    inventory.restock(food, 40);
    var plan = new DistributionPlanId(UUID.randomUUID());
    inventory.reserve(
        plan, List.of(new ReservationLine(water, 60), new ReservationLine(food, 10)), NOW);
    inventories.save(inventory);
    clear();

    Inventory reserved = inventories.findByReservationPlanId(plan).orElseThrow();
    Reservation reservation = reserved.activeReservationOf(plan).orElseThrow();
    assertThat(reservation.lines())
        .containsExactlyInAnyOrder(new ReservationLine(water, 60), new ReservationLine(food, 10));
    assertThat(reservation.reservedAt()).isEqualTo(NOW);
    assertThat(reserved.stockItem(water).stockLevel()).isEqualTo(new StockLevel(100, 60));

    reserved.changeReservation(plan, List.of(new ReservationLine(water, 30)));
    inventories.save(reserved);
    clear();

    Inventory changed = inventories.findByReservationPlanId(plan).orElseThrow();
    assertThat(changed.activeReservationOf(plan).orElseThrow().lines())
        .containsExactly(new ReservationLine(water, 30));
    assertThat(changed.stockItem(water).stockLevel()).isEqualTo(new StockLevel(100, 30));
    assertThat(changed.availableQuantityOf(food)).isEqualTo(40);

    reserved = changed;
    reserved.releaseReservation(plan, NOW.plusHours(1));
    inventories.save(reserved);
    clear();

    Inventory released = inventories.findById(inventory.id()).orElseThrow();
    assertThat(released.reservations().getFirst().status()).isEqualTo(ReservationStatus.RELEASED);
    assertThat(released.reservations().getFirst().closedAt()).contains(NOW.plusHours(1));
    assertThat(released.availableQuantityOf(water)).isEqualTo(100);
  }

  @Test
  void unknownInventoryOrganizationOrPlanIsNotFound() {
    assertThat(inventories.findById(InventoryId.generate())).isEmpty();
    assertThat(inventories.findByOrganizationId(new OrganizationId(UUID.randomUUID()))).isEmpty();
    assertThat(inventories.findByReservationPlanId(new DistributionPlanId(UUID.randomUUID())))
        .isEmpty();
  }

  private void clear() {
    entityManager.flush();
    entityManager.clear();
  }
}
