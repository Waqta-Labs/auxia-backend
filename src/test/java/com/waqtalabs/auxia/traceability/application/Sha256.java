package com.waqtalabs.auxia.traceability.application;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 for the use case tests, the same algorithm as the application. */
public final class Sha256 implements HashingService {

  @Override
  public HashValue hashRecord(CanonicalDeliveryRecord record) {
    return hashContent(record.toCanonicalJson().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public HashValue hashContent(byte[] content) {
    try {
      return HashValue.of(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
