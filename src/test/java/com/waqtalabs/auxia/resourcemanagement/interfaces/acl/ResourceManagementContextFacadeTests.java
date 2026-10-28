package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InsufficientStockException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationAlreadyExistsException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** What Emergency Management will call to check the stock and reserve it for its plans. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class ResourceManagementContextFacadeTests {

  @Autowired private ResourceManagementContextFacade facade;
  @Autowired private InventoryRepository inventories;

  private final UUID organizationId = UUID.randomUUID();
  private final UUID planId = UUID.randomUUID();
  private UUID water;
  private UUID food;

  @BeforeEach
  void inventoryWithWaterAndFood() {
    Inventory inventory = Inventory.create(new OrganizationId(organizationId), "Almacén Lima");
    ResourceId waterId =
        inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 0);
    ResourceId foodId =
        inventory.registerResource("Kit de alimentos", ResourceCategory.FOOD, UnitOfMeasure.KIT, 0);
    inventory.restock(waterId, 100);
    inventory.restock(foodId, 40);
    inventories.save(inventory);
    water = waterId.value();
    food = foodId.value();
  }

  @Test
  void availableStockListsEveryResourceOfTheOrganization() {
    assertThat(facade.fetchAvailableStock(organizationId))
        .containsExactly(
            new AvailableStockResource(water, "Agua 2.5 L", "WATER", "UNIT", 100),
            new AvailableStockResource(food, "Kit de alimentos", "FOOD", "KIT", 40));
    assertThat(facade.fetchAvailableStock(UUID.randomUUID())).isEmpty();
  }

  @Test
  void reservedStockIsNoLongerAvailableUntilItIsReleased() {
    facade.reserveResources(
        organizationId,
        planId,
        List.of(new ReservationLineResource(water, 60), new ReservationLineResource(food, 40)));

    assertThat(facade.fetchAvailableStock(organizationId))
        .extracting(AvailableStockResource::available)
        .containsExactly(40, 0);

    facade.releaseReservation(planId);

    assertThat(facade.fetchAvailableStock(organizationId))
        .extracting(AvailableStockResource::available)
        .containsExactly(100, 40);
  }

  @Test
  void reservationBeyondTheStockReservesNothing() {
    var lines =
        List.of(new ReservationLineResource(water, 60), new ReservationLineResource(food, 41));

    assertThatThrownBy(() -> facade.reserveResources(organizationId, planId, lines))
        .isInstanceOf(InsufficientStockException.class);
    assertThat(facade.fetchAvailableStock(organizationId))
        .extracting(AvailableStockResource::available)
        .containsExactly(100, 40);
  }

  @Test
  void changedReservationHoldsTheNewQuantities() {
    facade.reserveResources(
        organizationId, planId, List.of(new ReservationLineResource(water, 60)));

    facade.changeReservation(
        planId,
        List.of(new ReservationLineResource(water, 20), new ReservationLineResource(food, 40)));

    assertThat(facade.fetchAvailableStock(organizationId))
        .extracting(AvailableStockResource::available)
        .containsExactly(80, 0);
    var beyondTheStock = List.of(new ReservationLineResource(water, 101));
    assertThatThrownBy(() -> facade.changeReservation(planId, beyondTheStock))
        .isInstanceOf(InsufficientStockException.class);

    facade.releaseReservation(planId);

    assertThat(facade.fetchAvailableStock(organizationId))
        .extracting(AvailableStockResource::available)
        .containsExactly(100, 40);
  }

  @Test
  void planReservesOnlyOnce() {
    var lines = List.of(new ReservationLineResource(water, 10));
    facade.reserveResources(organizationId, planId, lines);

    assertThatThrownBy(() -> facade.reserveResources(organizationId, planId, lines))
        .isInstanceOf(ReservationAlreadyExistsException.class);
  }
}
