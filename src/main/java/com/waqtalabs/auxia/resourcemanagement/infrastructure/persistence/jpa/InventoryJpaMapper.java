package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Reservation;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockLevel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class InventoryJpaMapper {

  private InventoryJpaMapper() {}

  static Inventory toDomain(InventoryJpaEntity entity) {
    return Inventory.reconstitute(
        new InventoryId(entity.id),
        new OrganizationId(entity.organizationId),
        entity.name,
        entity.stockItems.stream().map(InventoryJpaMapper::toDomain).toList(),
        entity.reservations.stream().map(InventoryJpaMapper::toDomain).toList());
  }

  /**
   * Copies the aggregate into a new or managed entity. Stock items and reservations are matched
   * by id, so existing rows are updated instead of deleted and inserted again.
   */
  static void copy(Inventory inventory, InventoryJpaEntity entity) {
    entity.id = inventory.id().value();
    entity.organizationId = inventory.organizationId().value();
    entity.name = inventory.name();

    Map<UUID, StockItemJpaEntity> items =
        entity.stockItems.stream()
            .collect(Collectors.toMap(item -> item.resourceId, Function.identity()));
    for (StockItem item : inventory.stockItems()) {
      StockItemJpaEntity row = items.get(item.resourceId().value());
      if (row == null) {
        row = new StockItemJpaEntity();
        row.resourceId = item.resourceId().value();
        row.inventory = entity;
        entity.stockItems.add(row);
      }
      row.name = item.name();
      row.category = item.category();
      row.unit = item.unit();
      row.onHand = item.stockLevel().onHand();
      row.reserved = item.stockLevel().reserved();
      row.minimumStock = item.minimumStock();
    }

    Map<UUID, ReservationJpaEntity> reservations =
        entity.reservations.stream()
            .collect(Collectors.toMap(reservation -> reservation.id, Function.identity()));
    for (Reservation reservation : inventory.reservations()) {
      ReservationJpaEntity row = reservations.get(reservation.id().value());
      if (row == null) {
        row = new ReservationJpaEntity();
        row.id = reservation.id().value();
        row.inventory = entity;
        row.distributionPlanId = reservation.distributionPlanId().value();
        row.reservedAt = reservation.reservedAt();
        entity.reservations.add(row);
      }
      copyLines(reservation, row, entity.id);
      row.status = reservation.status();
      row.closedAt = reservation.closedAt().orElse(null);
    }
  }

  /**
   * Rewrites the lines of a reservation only when they changed, such as after a modification.
   * Each resource appears once, so the order of the lines does not matter.
   */
  private static void copyLines(
      Reservation reservation, ReservationJpaEntity row, UUID inventoryId) {
    Set<ReservationLine> stored =
        row.lines.stream()
            .map(line -> new ReservationLine(new ResourceId(line.resourceId), line.quantity))
            .collect(Collectors.toSet());
    if (stored.equals(Set.copyOf(reservation.lines()))) {
      return;
    }
    row.lines.clear();
    reservation.lines().stream()
        .map(
            line ->
                new ReservationLineEmbeddable(
                    inventoryId, line.resourceId().value(), line.quantity()))
        .forEach(row.lines::add);
  }

  private static StockItem toDomain(StockItemJpaEntity row) {
    return StockItem.reconstitute(
        new ResourceId(row.resourceId),
        row.name,
        row.category,
        row.unit,
        row.minimumStock,
        new StockLevel(row.onHand, row.reserved));
  }

  private static Reservation toDomain(ReservationJpaEntity row) {
    return Reservation.reconstitute(
        new ReservationId(row.id),
        new DistributionPlanId(row.distributionPlanId),
        row.lines.stream()
            .map(line -> new ReservationLine(new ResourceId(line.resourceId), line.quantity))
            .toList(),
        row.status,
        row.reservedAt,
        row.closedAt);
  }
}
