package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class ZoneNotFoundException extends NotFoundException {

  public ZoneNotFoundException(ZoneId id) {
    super("Zone " + id.value() + " does not exist", id.value());
  }
}
