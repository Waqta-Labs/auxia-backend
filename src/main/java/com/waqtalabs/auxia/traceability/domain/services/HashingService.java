package com.waqtalabs.auxia.traceability.domain.services;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;

/** Computes the SHA-256 of the record of a delivery and of each evidence file. */
public interface HashingService {

  /** Hash of the canonical JSON of the record. */
  HashValue hashRecord(CanonicalDeliveryRecord record);

  HashValue hashContent(byte[] content);
}
