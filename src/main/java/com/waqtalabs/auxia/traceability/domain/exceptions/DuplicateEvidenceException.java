package com.waqtalabs.auxia.traceability.domain.exceptions;

import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;

/** The same file is attached only once to a delivery. */
public class DuplicateEvidenceException extends RuleViolationException {

  public DuplicateEvidenceException(HashValue contentHash) {
    super(
        "The evidence " + contentHash.value() + " is attached more than once",
        contentHash.value());
  }
}
