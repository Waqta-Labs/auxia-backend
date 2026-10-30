package com.waqtalabs.auxia.traceability.domain.model.aggregates;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryItemId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import java.util.Objects;

/**
 * Quantity of a resource that the brigade really delivered, which can be less than planned.
 * Entity inside the {@link Delivery} aggregate.
 */
public final class DeliveryItem {

  private final DeliveryItemId id;
  private final ResourceId resourceId;
  private final int quantity;

  private DeliveryItem(DeliveryItemId id, ResourceId resourceId, int quantity) {
    this.id = Objects.requireNonNull(id, "id");
    this.resourceId = Objects.requireNonNull(resourceId, "resourceId");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    this.quantity = quantity;
  }

  public static DeliveryItem of(ResourceId resourceId, int quantity) {
    return new DeliveryItem(DeliveryItemId.generate(), resourceId, quantity);
  }

  public static DeliveryItem reconstitute(DeliveryItemId id, ResourceId resourceId, int quantity) {
    return new DeliveryItem(id, resourceId, quantity);
  }

  public DeliveryItemId id() {
    return id;
  }

  public ResourceId resourceId() {
    return resourceId;
  }

  public int quantity() {
    return quantity;
  }
}
