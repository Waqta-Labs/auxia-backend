package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DuplicateResourceException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InsufficientStockException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationAlreadyExistsException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ResourceNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stock of humanitarian resources of an organization and the reservations made on it.
 *
 * <p>Each organization has a single inventory (TS-C08), so reserving the resources of a plan
 * changes one aggregate in one transaction: every line is reserved or none is. The available
 * quantity of a resource never goes below zero.
 */
public final class Inventory {

  private final InventoryId id;
  private final OrganizationId organizationId;
  private final String name;
  private final List<StockItem> stockItems;
  private final List<Reservation> reservations;

  private Inventory(
      InventoryId id,
      OrganizationId organizationId,
      String name,
      List<StockItem> stockItems,
      List<Reservation> reservations) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.name = requireText(name, "name");
    this.stockItems = new ArrayList<>(stockItems);
    this.reservations = new ArrayList<>(reservations);
  }

  /** Creates an empty inventory for an organization, such as "Almacén Central Lima". */
  public static Inventory create(OrganizationId organizationId, String name) {
    return new Inventory(InventoryId.generate(), organizationId, name, List.of(), List.of());
  }

  /** Rebuilds an inventory that is already stored. */
  public static Inventory reconstitute(
      InventoryId id,
      OrganizationId organizationId,
      String name,
      List<StockItem> stockItems,
      List<Reservation> reservations) {
    return new Inventory(id, organizationId, name, stockItems, reservations);
  }

  /**
   * Adds a resource without stock.
   *
   * @param minimumStock available quantity below which the resource needs restocking
   * @throws DuplicateResourceException if a resource already has that name, ignoring case
   */
  public ResourceId registerResource(
      String name, ResourceCategory category, UnitOfMeasure unit, int minimumStock) {
    StockItem item = StockItem.register(name, category, unit, minimumStock);
    if (stockItems.stream().anyMatch(existing -> existing.hasName(item.name()))) {
      throw new DuplicateResourceException(item.name());
    }
    stockItems.add(item);
    return item.resourceId();
  }

  /** Adds a quantity received, such as a donation or a purchase (US-08). */
  public void restock(ResourceId resourceId, int quantity) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    stockItem(resourceId).increase(quantity);
  }

  /**
   * Corrects the physical quantity, for example after a count or a loss. The reason is required
   * so every manual change can be justified.
   *
   * @param quantityChange positive to add, negative to remove
   * @throws InsufficientStockException if it removes more than the available quantity (TS-03)
   */
  public void adjustStock(ResourceId resourceId, int quantityChange, String reason) {
    if (quantityChange == 0) {
      throw new IllegalArgumentException("quantityChange must not be zero");
    }
    requireText(reason, "reason");
    StockItem item = stockItem(resourceId);
    if (-quantityChange > item.available()) {
      throw new InsufficientStockException(resourceId, item.available(), -quantityChange);
    }
    item.adjust(quantityChange);
  }

  /**
   * Holds the stock of every line for a distribution plan. Nothing is reserved if a line fails.
   *
   * @throws ReservationAlreadyExistsException if the plan already reserved its resources
   * @throws ResourceNotFoundException if a line names a resource of another inventory
   * @throws InsufficientStockException if a line asks for more than the available quantity
   */
  public void reserve(
      DistributionPlanId planId, List<ReservationLine> lines, LocalDateTime reservedAt) {
    Objects.requireNonNull(planId, "planId");
    if (reservations.stream().anyMatch(reservation -> reservation.isFor(planId))) {
      throw new ReservationAlreadyExistsException(planId);
    }
    Set<ResourceId> resources = new HashSet<>();
    for (ReservationLine line : lines) {
      if (!resources.add(line.resourceId())) {
        throw new IllegalArgumentException("each resource appears in one line only");
      }
      StockItem item = stockItem(line.resourceId());
      if (line.quantity() > item.available()) {
        throw new InsufficientStockException(
            line.resourceId(), item.available(), line.quantity());
      }
    }
    Reservation reservation = Reservation.open(planId, lines, reservedAt);
    lines.forEach(line -> stockItem(line.resourceId()).reserve(line.quantity()));
    reservations.add(reservation);
  }

  /**
   * Returns the stock held for a plan to the available quantity, for example when the plan is
   * rejected.
   *
   * @throws ReservationNotFoundException if the plan has no active reservation
   */
  public void releaseReservation(DistributionPlanId planId, LocalDateTime releasedAt) {
    Reservation reservation =
        activeReservationOf(planId).orElseThrow(() -> new ReservationNotFoundException(planId));
    reservation.lines().forEach(line -> stockItem(line.resourceId()).release(line.quantity()));
    reservation.release(releasedAt);
  }

  public int availableQuantityOf(ResourceId resourceId) {
    return stockItem(resourceId).available();
  }

  /**
   * The resource with that id.
   *
   * @throws ResourceNotFoundException if the resource does not belong to this inventory
   */
  public StockItem stockItem(ResourceId resourceId) {
    return stockItems.stream()
        .filter(item -> item.resourceId().equals(resourceId))
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(resourceId, id));
  }

  public Optional<Reservation> activeReservationOf(DistributionPlanId planId) {
    return reservations.stream()
        .filter(reservation -> reservation.isFor(planId) && reservation.isActive())
        .findFirst();
  }

  public boolean belongsTo(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId);
  }

  public InventoryId id() {
    return id;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public String name() {
    return name;
  }

  public List<StockItem> stockItems() {
    return List.copyOf(stockItems);
  }

  public List<Reservation> reservations() {
    return List.copyOf(reservations);
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
