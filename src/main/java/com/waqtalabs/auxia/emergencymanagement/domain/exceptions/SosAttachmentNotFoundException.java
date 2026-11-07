package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class SosAttachmentNotFoundException extends NotFoundException {

  public SosAttachmentNotFoundException(SosAttachmentId id) {
    super("SOS attachment " + id.value() + " does not exist", id.value());
  }
}
