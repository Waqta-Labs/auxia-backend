package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class BrigadeNotFoundException extends NotFoundException {

  public BrigadeNotFoundException(BrigadeId id) {
    super("Brigade " + id.value() + " does not exist", id.value());
  }
}
