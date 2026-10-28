package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionItemId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import java.util.Objects;
import java.util.Optional;

/**
 * Quantity of a resource in a distribution plan. Entity inside the {@code DistributionPlan}
 * aggregate. It keeps the recommended quantity next to the current one, so the original
 * recommendation survives a modification (US-13, scenario 2). A quantity of 0 takes the resource
 * out of the plan without losing what was recommended.
 */
public final class DistributionItem {

  private final DistributionItemId id;
  private final ResourceId resourceId;
  private final Integer recommendedQuantity;
  private int quantity;

  private DistributionItem(
      DistributionItemId id, ResourceId resourceId, Integer recommendedQuantity, int quantity) {
    this.id = Objects.requireNonNull(id, "id");
    this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    if (recommendedQuantity != null && recommendedQuantity <= 0) {
      throw new IllegalArgumentException("recommendedQuantity must be positive");
    }
    if (quantity < 0 || (quantity == 0 && recommendedQuantity == null)) {
      throw new IllegalArgumentException("quantity must be positive for an added resource");
    }
    this.recommendedQuantity = recommendedQuantity;
    this.quantity = quantity;
  }

  /** A line of the recommendation: its current quantity starts as the recommended one. */
  public static DistributionItem recommended(ResourceId resourceId, int quantity) {
    return new DistributionItem(DistributionItemId.generate(), resourceId, quantity, quantity);
  }

  /** A resource that the authority adds to the plan and the recommendation did not include. */
  public static DistributionItem added(ResourceId resourceId, int quantity) {
    return new DistributionItem(DistributionItemId.generate(), resourceId, null, quantity);
  }

  /**
   * Rebuilds a line that is already stored.
   *
   * @param recommendedQuantity {@code null} for a resource the authority added
   */
  public static DistributionItem reconstitute(
      DistributionItemId id, ResourceId resourceId, Integer recommendedQuantity, int quantity) {
    return new DistributionItem(id, resourceId, recommendedQuantity, quantity);
  }

  void changeQuantity(int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must not be negative");
    }
    this.quantity = quantity;
  }

  /** Whether the plan still distributes this resource. */
  public boolean isDistributed() {
    return quantity > 0;
  }

  public DistributionItemId id() {
    return id;
  }

  public ResourceId resourceId() {
    return resourceId;
  }

  /** Empty for a resource the authority added to the recommendation. */
  public Optional<Integer> recommendedQuantity() {
    return Optional.ofNullable(recommendedQuantity);
  }

  public int quantity() {
    return quantity;
  }
}
