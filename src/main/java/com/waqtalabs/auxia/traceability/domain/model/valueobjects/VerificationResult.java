package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of comparing the hash of a delivery, computed again from its current data and files,
 * with the hash anchored in the blockchain (US-17).
 *
 * @param recordHash hash of the delivery as it is now in PostgreSQL and in the evidence storage
 * @param anchoredHash hash stored in the smart contract; empty if it was not anchored yet
 * @param transactionHash transaction that anchored it; empty if it was not anchored yet
 */
public record VerificationResult(
    DeliveryId deliveryId,
    HashValue recordHash,
    Optional<HashValue> anchoredHash,
    Optional<String> transactionHash) {

  public VerificationResult {
    Objects.requireNonNull(deliveryId, "deliveryId");
    Objects.requireNonNull(recordHash, "recordHash");
    Objects.requireNonNull(anchoredHash, "anchoredHash");
    Objects.requireNonNull(transactionHash, "transactionHash");
  }

  /** The delivery was not altered since it was anchored. */
  public boolean verified() {
    return anchoredHash.filter(recordHash::equals).isPresent();
  }
}
