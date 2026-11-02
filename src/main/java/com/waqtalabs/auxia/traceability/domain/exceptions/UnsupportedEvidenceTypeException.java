package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;

/** An evidence is a photo (JPEG, PNG, WebP or HEIC) or a PDF document. */
public class UnsupportedEvidenceTypeException extends RuleViolationException {

  public UnsupportedEvidenceTypeException(String contentType) {
    super("Evidence of type " + contentType + " is not supported", contentType);
  }
}
