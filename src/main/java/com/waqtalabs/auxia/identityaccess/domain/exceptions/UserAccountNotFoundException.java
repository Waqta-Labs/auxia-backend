package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class UserAccountNotFoundException extends NotFoundException {

  public UserAccountNotFoundException(UserId id) {
    super("User " + id.value() + " does not exist", id.value());
  }
}
