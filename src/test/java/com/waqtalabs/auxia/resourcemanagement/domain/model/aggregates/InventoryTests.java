package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DuplicateResourceException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InsufficientStockException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationAlreadyExistsException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ResourceNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockLevel;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InventoryTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final DistributionPlanId PLAN = new DistributionPlanId(UUID.randomUUID());

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final Inventory inventory = Inventory.create(organization, " Almacén Central Lima ");
  private final ResourceId water =
      inventory.registerResource("Agua 2.5 L", ResourceCategory.WATER, UnitOfMeasure.UNIT, 50);
  private final ResourceId food =
      inventory.registerResource("Kit de alimentos", ResourceCategory.FOOD, UnitOfMeasure.KIT, 20);

  @Test
  void newInventoryBelongsToItsOrganization() {
    assertThat(inventory.id()).isNotNull();
    assertThat(inventory.name()).isEqualTo("Almacén Central Lima");
    assertThat(inventory.belongsTo(organization)).isTrue();
    assertThat(inventory.reservations()).isEmpty();
  }

  @Test
  void registeredResourceStartsWithoutStockAndBelowItsMinimum() {
    StockItem item = inventory.stockItem(water);

    assertThat(item.name()).isEqualTo("Agua 2.5 L");
    assertThat(item.category()).isEqualTo(ResourceCategory.WATER);
    assertThat(item.unit()).isEqualTo(UnitOfMeasure.UNIT);
    assertThat(item.minimumStock()).isEqualTo(50);
    assertThat(item.stockLevel()).isEqualTo(StockLevel.empty());
    assertThat(item.isBelowMinimum()).isTrue();
    assertThat(inventory.stockItems())
        .extracting(StockItem::resourceId)
        .containsExactly(water, food);
  }

  @Test
  void resourceNameIsUniqueIgnoringCase() {
    assertThatThrownBy(
            () ->
                inventory.registerResource(
                    "agua 2.5 l ", ResourceCategory.WATER, UnitOfMeasure.LITER, 0))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void restockAddsTheReceivedQuantity() {
    inventory.restock(water, 120);
    inventory.restock(water, 30);

    assertThat(inventory.availableQuantityOf(water)).isEqualTo(150);
    assertThat(inventory.stockItem(water).isBelowMinimum()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -5})
  void restockWithoutAPositiveQuantityIsRejectedAndChangesNothing(int quantity) {
    inventory.restock(water, 10);

    assertThatThrownBy(() -> inventory.restock(water, quantity))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(inventory.availableQuantityOf(water)).isEqualTo(10);
  }

  @Test
  void resourceOfAnotherInventoryIsNotFound() {
    assertThatThrownBy(() -> inventory.restock(ResourceId.generate(), 10))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void adjustmentAddsOrRemovesWithAReason() {
    inventory.restock(water, 100);

    inventory.adjustStock(water, -30, "Botellas dañadas en el traslado");
    inventory.adjustStock(water, 5, "Conteo físico");

    assertThat(inventory.availableQuantityOf(water)).isEqualTo(75);
  }

  @Test
  void adjustmentCannotRemoveMoreThanTheAvailableQuantity() {
    inventory.restock(water, 100);
    inventory.reserve(PLAN, List.of(new ReservationLine(water, 80)), NOW);

    assertThatThrownBy(() -> inventory.adjustStock(water, -21, "Pérdida"))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessage("Resource " + water.value() + " has 20 available and 21 were requested");
    assertThat(inventory.stockItem(water).stockLevel()).isEqualTo(new StockLevel(100, 80));
  }

  @Test
  void adjustmentNeedsAReasonAndAChange() {
    inventory.restock(water, 100);

    assertThatThrownBy(() -> inventory.adjustStock(water, -5, " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> inventory.adjustStock(water, 0, "Conteo"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reservationHoldsEveryLineAsNotAvailable() {
    inventory.restock(water, 100);
    inventory.restock(food, 40);

    inventory.reserve(
        PLAN, List.of(new ReservationLine(water, 60), new ReservationLine(food, 40)), NOW);

    assertThat(inventory.stockItem(water).stockLevel()).isEqualTo(new StockLevel(100, 60));
    assertThat(inventory.availableQuantityOf(water)).isEqualTo(40);
    assertThat(inventory.availableQuantityOf(food)).isZero();
    Reservation reservation = inventory.activeReservationOf(PLAN).orElseThrow();
    assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    assertThat(reservation.reservedAt()).isEqualTo(NOW);
    assertThat(reservation.closedAt()).isEmpty();
  }

  @Test
  void reservationIsAllOrNothing() {
    inventory.restock(water, 100);
    inventory.restock(food, 10);

    assertThatThrownBy(
            () ->
                inventory.reserve(
                    PLAN,
                    List.of(new ReservationLine(water, 60), new ReservationLine(food, 11)),
                    NOW))
        .isInstanceOf(InsufficientStockException.class);
    assertThat(inventory.availableQuantityOf(water)).isEqualTo(100);
    assertThat(inventory.reservations()).isEmpty();
  }

  @Test
  void planReservesItsResourcesOnlyOnce() {
    inventory.restock(water, 100);
    inventory.reserve(PLAN, List.of(new ReservationLine(water, 10)), NOW);

    assertThatThrownBy(
            () -> inventory.reserve(PLAN, List.of(new ReservationLine(water, 10)), NOW))
        .isInstanceOf(ReservationAlreadyExistsException.class);
  }

  @Test
  void reservationRejectsARepeatedResourceAndAnUnknownOne() {
    inventory.restock(water, 100);

    assertThatThrownBy(
            () ->
                inventory.reserve(
                    PLAN,
                    List.of(new ReservationLine(water, 1), new ReservationLine(water, 2)),
                    NOW))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                inventory.reserve(
                    PLAN, List.of(new ReservationLine(ResourceId.generate(), 1)), NOW))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void releasingAReservationMakesItsStockAvailableAgain() {
    inventory.restock(water, 100);
    inventory.reserve(PLAN, List.of(new ReservationLine(water, 60)), NOW);

    inventory.releaseReservation(PLAN, NOW.plusHours(2));

    assertThat(inventory.availableQuantityOf(water)).isEqualTo(100);
    assertThat(inventory.activeReservationOf(PLAN)).isEmpty();
    Reservation released = inventory.reservations().getFirst();
    assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
    assertThat(released.closedAt()).contains(NOW.plusHours(2));
  }

  @Test
  void changedReservationHoldsItsNewLinesAndCanUseWhatItAlreadyHeld() {
    inventory.restock(water, 100);
    inventory.restock(food, 40);
    inventory.reserve(PLAN, List.of(new ReservationLine(water, 60)), NOW);

    inventory.changeReservation(
        PLAN, List.of(new ReservationLine(water, 100), new ReservationLine(food, 15)));

    assertThat(inventory.stockItem(water).stockLevel()).isEqualTo(new StockLevel(100, 100));
    assertThat(inventory.availableQuantityOf(food)).isEqualTo(25);
    assertThat(inventory.activeReservationOf(PLAN).orElseThrow().lines())
        .containsExactly(new ReservationLine(water, 100), new ReservationLine(food, 15));
    assertThat(inventory.reservations()).hasSize(1);
  }

  @Test
  void changedReservationBeyondTheStockChangesNothing() {
    inventory.restock(water, 100);
    inventory.restock(food, 10);
    inventory.reserve(PLAN, List.of(new ReservationLine(water, 60)), NOW);

    assertThatThrownBy(
            () ->
                inventory.changeReservation(
                    PLAN, List.of(new ReservationLine(water, 30), new ReservationLine(food, 11))))
        .isInstanceOf(InsufficientStockException.class);
    assertThat(inventory.availableQuantityOf(water)).isEqualTo(40);
    assertThat(inventory.availableQuantityOf(food)).isEqualTo(10);
    assertThat(inventory.activeReservationOf(PLAN).orElseThrow().lines())
        .containsExactly(new ReservationLine(water, 60));
  }

  @Test
  void onlyAnActiveReservationCanChange() {
    inventory.restock(water, 100);
    var lines = List.of(new ReservationLine(water, 10));
    assertThatThrownBy(() -> inventory.changeReservation(PLAN, lines))
        .isInstanceOf(ReservationNotFoundException.class);

    inventory.reserve(PLAN, lines, NOW);
    inventory.releaseReservation(PLAN, NOW);

    assertThatThrownBy(() -> inventory.changeReservation(PLAN, lines))
        .isInstanceOf(ReservationNotFoundException.class);
  }

  @Test
  void releasingWithoutAnActiveReservationIsRejected() {
    assertThatThrownBy(() -> inventory.releaseReservation(PLAN, NOW))
        .isInstanceOf(ReservationNotFoundException.class);
  }
}
