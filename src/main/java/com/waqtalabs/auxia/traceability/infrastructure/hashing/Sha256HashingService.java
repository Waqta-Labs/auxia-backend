package com.waqtalabs.auxia.traceability.infrastructure.hashing;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

/** SHA-256 with {@link MessageDigest}; the record is hashed as UTF-8 canonical JSON. */
@Component
class Sha256HashingService implements HashingService {

  @Override
  public HashValue hashRecord(CanonicalDeliveryRecord record) {
    return hashContent(record.toCanonicalJson().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public HashValue hashContent(byte[] content) {
    try {
      return HashValue.of(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Every Java platform must support SHA-256", ex);
    }
  }
}
