package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class InventoryNotFoundException extends NotFoundException {

  public InventoryNotFoundException(InventoryId id) {
    super("Inventory " + id.value() + " does not exist", id.value());
  }
}
