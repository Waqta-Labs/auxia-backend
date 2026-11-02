package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * A resource whose available quantity fell below its minimum stock (US-10). It is raised once per
 * fall and resolved when the resource is restocked above the minimum; a new fall raises a new
 * alert. It keeps the name and the quantities of the moment it was raised.
 */
public final class StockAlert {

  private final StockAlertId id;
  private final InventoryId inventoryId;
  private final ResourceId resourceId;
  private final String resourceName;
  private final int availableQuantity;
  private final int minimumStock;
  private final LocalDateTime raisedAt;
  private StockAlertStatus status;
  private LocalDateTime resolvedAt;

  private StockAlert(
      StockAlertId id,
      InventoryId inventoryId,
      ResourceId resourceId,
      String resourceName,
      int availableQuantity,
      int minimumStock,
      StockAlertStatus status,
      LocalDateTime raisedAt,
      LocalDateTime resolvedAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.inventoryId = Objects.requireNonNull(inventoryId, "inventoryId");
    this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
    this.status = Objects.requireNonNull(status, "status");
    this.raisedAt = Objects.requireNonNull(raisedAt, "raisedAt");
    if ((status == StockAlertStatus.RESOLVED) != (resolvedAt != null)) {
      throw new IllegalArgumentException("Only a resolved alert has a resolution date");
    }
    this.availableQuantity = availableQuantity;
    this.minimumStock = minimumStock;
    this.resolvedAt = resolvedAt;
  }

  /** Raises the alert of a resource that is below its minimum stock. */
  public static StockAlert raise(InventoryId inventoryId, StockItem item, LocalDateTime raisedAt) {
    if (!item.isBelowMinimum()) {
      throw new IllegalArgumentException("Resource " + item.resourceId().value() + " is not low");
    }
    return new StockAlert(
        StockAlertId.generate(),
        inventoryId,
        item.resourceId(),
        item.name(),
        item.available(),
        item.minimumStock(),
        StockAlertStatus.OPEN,
        raisedAt,
        null);
  }

  /**
   * Rebuilds an alert that is already stored.
   *
   * @param resolvedAt {@code null} while the alert is open
   */
  public static StockAlert reconstitute(
      StockAlertId id,
      InventoryId inventoryId,
      ResourceId resourceId,
      String resourceName,
      int availableQuantity,
      int minimumStock,
      StockAlertStatus status,
      LocalDateTime raisedAt,
      LocalDateTime resolvedAt) {
    return new StockAlert(
        id,
        inventoryId,
        resourceId,
        resourceName,
        availableQuantity,
        minimumStock,
        status,
        raisedAt,
        resolvedAt);
  }

  /** The resource is above its minimum again (US-10, scenario 2). */
  public void resolve(LocalDateTime at) {
    if (status == StockAlertStatus.OPEN) {
      status = StockAlertStatus.RESOLVED;
      resolvedAt = Objects.requireNonNull(at, "at");
    }
  }

  public boolean isOpen() {
    return status == StockAlertStatus.OPEN;
  }

  public StockAlertId id() {
    return id;
  }

  public InventoryId inventoryId() {
    return inventoryId;
  }

  public ResourceId resourceId() {
    return resourceId;
  }

  public String resourceName() {
    return resourceName;
  }

  /** Available quantity when the alert was raised. */
  public int availableQuantity() {
    return availableQuantity;
  }

  public int minimumStock() {
    return minimumStock;
  }

  public StockAlertStatus status() {
    return status;
  }

  public LocalDateTime raisedAt() {
    return raisedAt;
  }

  public Optional<LocalDateTime> resolvedAt() {
    return Optional.ofNullable(resolvedAt);
  }
}
