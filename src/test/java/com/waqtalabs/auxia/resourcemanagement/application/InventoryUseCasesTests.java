package com.waqtalabs.auxia.resourcemanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.AdjustStockCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CreateInventoryCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RegisterResourceCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReleaseReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ReserveResourcesCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.RestockResourceCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.AdjustStockCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateInventoryCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RegisterResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReleaseReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.RestockResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.InventoryQueryService;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InsufficientStockException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryAlreadyExistsException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.OrganizationInventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Reservation;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockLevel;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventoryUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-14T14:30:00Z"), ZoneOffset.ofHours(-5));

  private final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();
  private final CreateInventoryCommandHandler create =
      new CreateInventoryCommandHandler(inventories);
  private final RegisterResourceCommandHandler registerResource =
      new RegisterResourceCommandHandler(inventories);
  private final RestockResourceCommandHandler restock =
      new RestockResourceCommandHandler(inventories);
  private final AdjustStockCommandHandler adjust = new AdjustStockCommandHandler(inventories);
  private final ReserveResourcesCommandHandler reserve =
      new ReserveResourcesCommandHandler(inventories, CLOCK);
  private final ReleaseReservationCommandHandler release =
      new ReleaseReservationCommandHandler(inventories, CLOCK);
  private final InventoryQueryService queries = new InventoryQueryService(inventories);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final DistributionPlanId plan = new DistributionPlanId(UUID.randomUUID());

  @Test
  void organizationCreatesItsSingleInventory() {
    Inventory inventory = create.handle(new CreateInventoryCommand(organization, "Almacén Lima"));

    assertThat(queries.handle(new GetInventoryByOrganizationIdQuery(organization)))
        .isEqualTo(inventory);
    assertThatThrownBy(() -> create.handle(new CreateInventoryCommand(organization, "Otro")))
        .isInstanceOf(InventoryAlreadyExistsException.class);
  }

  @Test
  void registeredAndRestockedResourceShowsItsAvailableQuantity() {
    Inventory inventory = create.handle(new CreateInventoryCommand(organization, "Almacén Lima"));
    StockItem water = water(inventory);

    StockItem restocked =
        restock.handle(new RestockResourceCommand(inventory.id(), water.resourceId(), 120));

    assertThat(restocked.stockLevel()).isEqualTo(new StockLevel(120, 0));
    assertThat(queries.handle(new GetInventoryByIdQuery(inventory.id())).stockItems())
        .extracting(StockItem::available)
        .containsExactly(120);
  }

  @Test
  void adjustmentBeyondTheAvailableQuantityChangesNothing() {
    Inventory inventory = create.handle(new CreateInventoryCommand(organization, "Almacén Lima"));
    StockItem water = water(inventory);
    restock.handle(new RestockResourceCommand(inventory.id(), water.resourceId(), 10));

    assertThatThrownBy(
            () ->
                adjust.handle(
                    new AdjustStockCommand(inventory.id(), water.resourceId(), -11, "Pérdida")))
        .isInstanceOf(InsufficientStockException.class);
    StockItem adjusted =
        adjust.handle(new AdjustStockCommand(inventory.id(), water.resourceId(), -4, "Pérdida"));
    assertThat(adjusted.available()).isEqualTo(6);
  }

  @Test
  void planReservesAndReleasesItsResourcesWithTheCurrentTime() {
    Inventory inventory = create.handle(new CreateInventoryCommand(organization, "Almacén Lima"));
    StockItem water = water(inventory);
    restock.handle(new RestockResourceCommand(inventory.id(), water.resourceId(), 100));

    reserve.handle(
        new ReserveResourcesCommand(
            organization, plan, List.of(new ReservationLine(water.resourceId(), 60))));

    Inventory reserved = inventories.findById(inventory.id()).orElseThrow();
    assertThat(reserved.availableQuantityOf(water.resourceId())).isEqualTo(40);
    Reservation reservation = reserved.activeReservationOf(plan).orElseThrow();
    assertThat(reservation.reservedAt()).isEqualTo(LocalDateTime.of(2026, 3, 14, 9, 30));

    release.handle(new ReleaseReservationCommand(plan));

    Inventory released = inventories.findById(inventory.id()).orElseThrow();
    assertThat(released.availableQuantityOf(water.resourceId())).isEqualTo(100);
  }

  @Test
  void organizationWithoutInventoryCannotReserve() {
    var command = new ReserveResourcesCommand(organization, plan, List.of());

    assertThatThrownBy(() -> reserve.handle(command))
        .isInstanceOf(OrganizationInventoryNotFoundException.class);
  }

  @Test
  void unknownInventoryOrReservationIsNotFound() {
    InventoryId unknown = InventoryId.generate();

    assertThatThrownBy(() -> queries.handle(new GetInventoryByIdQuery(unknown)))
        .isInstanceOf(InventoryNotFoundException.class);
    assertThatThrownBy(
            () ->
                registerResource.handle(
                    new RegisterResourceCommand(
                        unknown, "Agua", ResourceCategory.WATER, UnitOfMeasure.UNIT, 0)))
        .isInstanceOf(InventoryNotFoundException.class);
    assertThatThrownBy(() -> release.handle(new ReleaseReservationCommand(plan)))
        .isInstanceOf(ReservationNotFoundException.class);
    assertThatThrownBy(() -> queries.handle(new GetInventoryByOrganizationIdQuery(organization)))
        .isInstanceOf(OrganizationInventoryNotFoundException.class);
  }

  private StockItem water(Inventory inventory) {
    return registerResource.handle(
        new RegisterResourceCommand(
            inventory.id(), "Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 50));
  }
}
