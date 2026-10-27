package com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockLevel;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import java.util.Objects;

/**
 * A resource of an inventory with its physical, reserved and minimum quantity. Entity inside the
 * {@link Inventory} aggregate: only the inventory changes its stock.
 */
public final class StockItem {

  private final ResourceId resourceId;
  private final String name;
  private final ResourceCategory category;
  private final UnitOfMeasure unit;
  private final int minimumStock;
  private StockLevel stockLevel;

  private StockItem(
      ResourceId resourceId,
      String name,
      ResourceCategory category,
      UnitOfMeasure unit,
      int minimumStock,
      StockLevel stockLevel) {
    this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    this.name = requireText(name);
    this.category = Objects.requireNonNull(category, "category");
    this.unit = Objects.requireNonNull(unit, "unit");
    if (minimumStock < 0) {
      throw new IllegalArgumentException("minimumStock must not be negative");
    }
    this.minimumStock = minimumStock;
    this.stockLevel = Objects.requireNonNull(stockLevel, "stockLevel");
  }

  /** A new resource starts without stock. */
  static StockItem register(
      String name, ResourceCategory category, UnitOfMeasure unit, int minimumStock) {
    return new StockItem(
        ResourceId.generate(), name, category, unit, minimumStock, StockLevel.empty());
  }

  /** Rebuilds a resource that is already stored. */
  public static StockItem reconstitute(
      ResourceId resourceId,
      String name,
      ResourceCategory category,
      UnitOfMeasure unit,
      int minimumStock,
      StockLevel stockLevel) {
    return new StockItem(resourceId, name, category, unit, minimumStock, stockLevel);
  }

  void increase(int quantity) {
    stockLevel = new StockLevel(stockLevel.onHand() + quantity, stockLevel.reserved());
  }

  /** Adds a positive or negative quantity to the physical stock. */
  void adjust(int quantityChange) {
    stockLevel = new StockLevel(stockLevel.onHand() + quantityChange, stockLevel.reserved());
  }

  void reserve(int quantity) {
    stockLevel = new StockLevel(stockLevel.onHand(), stockLevel.reserved() + quantity);
  }

  void release(int quantity) {
    stockLevel = new StockLevel(stockLevel.onHand(), stockLevel.reserved() - quantity);
  }

  public int available() {
    return stockLevel.available();
  }

  /** Whether the available quantity is below the minimum, which calls for restocking. */
  public boolean isBelowMinimum() {
    return available() < minimumStock;
  }

  public boolean hasName(String other) {
    return name.equalsIgnoreCase(other.strip());
  }

  public ResourceId resourceId() {
    return resourceId;
  }

  public String name() {
    return name;
  }

  public ResourceCategory category() {
    return category;
  }

  public UnitOfMeasure unit() {
    return unit;
  }

  public int minimumStock() {
    return minimumStock;
  }

  public StockLevel stockLevel() {
    return stockLevel;
  }

  private static String requireText(String value) {
    Objects.requireNonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return value.strip();
  }
}
