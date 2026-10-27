package com.waqtalabs.auxia.identityaccess.domain.exceptions;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import com.waqtalabs.auxia.shared.domain.exceptions.ConflictException;

/** A RUC is registered by one organization only. */
public class DuplicateTaxIdException extends ConflictException {

  public DuplicateTaxIdException(TaxId taxId) {
    super("An organization with RUC " + taxId.value() + " already exists", taxId.value());
  }
}
