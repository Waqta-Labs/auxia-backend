package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class ResourceNotFoundException extends NotFoundException {

  public ResourceNotFoundException(ResourceId resourceId, InventoryId inventoryId) {
    super(
        "Resource " + resourceId.value() + " does not exist in inventory " + inventoryId.value(),
        resourceId.value(),
        inventoryId.value());
  }
}
