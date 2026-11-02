package com.waqtalabs.auxia.resourcemanagement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.EvaluateStockAlertsCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.EvaluateStockAlertsCommand;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetStockAlertsQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.StockAlertQueryService;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StockAlertUseCasesTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-15T14:00:00Z"), ZoneOffset.ofHours(-5));
  private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

  private final InMemoryInventoryRepository inventories = new InMemoryInventoryRepository();
  private final InMemoryStockAlertRepository alerts = new InMemoryStockAlertRepository();
  private final List<StockAlert> notified = new ArrayList<>();
  private final EvaluateStockAlertsCommandHandler evaluate =
      new EvaluateStockAlertsCommandHandler(inventories, alerts, notified::add, CLOCK);
  private final StockAlertQueryService queries = new StockAlertQueryService(inventories, alerts);

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final Inventory inventory = Inventory.create(organization, "Almacén Lima");
  private final ResourceId water =
      inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 50);
  private final ResourceId food =
      inventory.registerResource("Kit de alimentos", ResourceCategory.FOOD, UnitOfMeasure.KIT, 0);

  @Test
  void resourceBelowItsMinimumRaisesOneAlertAndNotifiesIt() {
    inventory.restock(water, 30);
    inventories.save(inventory);

    evaluate();
    evaluate();

    StockAlert alert = open().getFirst();
    assertThat(open()).hasSize(1);
    assertThat(alert.resourceId()).isEqualTo(water);
    assertThat(alert.resourceName()).isEqualTo("Agua 2.5 L");
    assertThat(alert.availableQuantity()).isEqualTo(30);
    assertThat(alert.minimumStock()).isEqualTo(50);
    assertThat(alert.raisedAt()).isEqualTo(NOW);
    assertThat(notified).containsExactly(alert);
  }

  @Test
  void restockAboveTheMinimumResolvesTheAlertAndANewFallRaisesAnother() {
    inventories.save(inventory);
    evaluate();

    inventory.restock(water, 80);
    inventories.save(inventory);
    evaluate();

    assertThat(open()).isEmpty();
    StockAlert resolved = all().getFirst();
    assertThat(resolved.status()).isEqualTo(StockAlertStatus.RESOLVED);
    assertThat(resolved.resolvedAt()).contains(NOW);

    inventory.adjustStock(water, -40, "Botellas dañadas");
    inventories.save(inventory);
    evaluate();

    assertThat(open()).hasSize(1);
    assertThat(all()).hasSize(2);
  }

  @Test
  void reservedStockAlsoCountsAsNotAvailable() {
    inventory.restock(water, 100);
    inventories.save(inventory);
    evaluate();
    assertThat(open()).isEmpty();

    inventory.reserve(
        new DistributionPlanId(UUID.randomUUID()), List.of(new ReservationLine(water, 60)), NOW);
    inventories.save(inventory);
    evaluate();

    assertThat(open()).extracting(StockAlert::availableQuantity).containsExactly(40);
  }

  @Test
  void alertsOfAnotherOrganizationAreNotFound() {
    inventories.save(inventory);
    var other = new OrganizationId(UUID.randomUUID());

    assertThatThrownBy(
            () -> queries.handle(new GetStockAlertsQuery(inventory.id(), other, null)))
        .isInstanceOf(InventoryNotFoundException.class);
  }

  private void evaluate() {
    evaluate.handle(new EvaluateStockAlertsCommand(inventory.id()));
  }

  private List<StockAlert> open() {
    return queries.handle(
        new GetStockAlertsQuery(inventory.id(), organization, StockAlertStatus.OPEN));
  }

  private List<StockAlert> all() {
    return queries.handle(new GetStockAlertsQuery(inventory.id(), organization, null));
  }
}
