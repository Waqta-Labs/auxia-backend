package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Hash of the record of a delivery and the state of its anchoring in the blockchain.
 *
 * <p>The transaction is kept as soon as it is sent, while it is still {@code PENDING}, so a retry
 * checks that transaction instead of sending the hash again. It is cleared when the anchoring
 * fails.
 *
 * @param attempts transactions sent so far
 */
public record IntegrityRecord(
    HashValue recordHash,
    AnchorStatus status,
    String transactionHash,
    LocalDateTime anchoredAt,
    int attempts) {

  private static final Pattern TRANSACTION = Pattern.compile("0x[0-9a-f]{64}");

  public IntegrityRecord {
    Objects.requireNonNull(recordHash, "recordHash");
    Objects.requireNonNull(status, "status");
    if (transactionHash != null && !TRANSACTION.matcher(transactionHash).matches()) {
      throw new IllegalArgumentException("transactionHash must be 0x and 64 hex characters");
    }
    if ((status == AnchorStatus.ANCHORED) != (anchoredAt != null)
        || (status == AnchorStatus.ANCHORED && transactionHash == null)
        || (status == AnchorStatus.FAILED && transactionHash != null)) {
      throw new IllegalArgumentException("transaction and anchoredAt do not match " + status);
    }
    if (attempts < 0) {
      throw new IllegalArgumentException("attempts must not be negative");
    }
  }

  /** A hash that was never sent. */
  public static IntegrityRecord pending(HashValue recordHash) {
    return new IntegrityRecord(recordHash, AnchorStatus.PENDING, null, null, 0);
  }

  /** The hash was sent in a transaction that is not confirmed yet. */
  public IntegrityRecord submitted(String transactionHash) {
    Objects.requireNonNull(transactionHash, "transactionHash");
    return new IntegrityRecord(
        recordHash, AnchorStatus.PENDING, transactionHash, null, attempts + 1);
  }

  public IntegrityRecord anchored(LocalDateTime at) {
    Objects.requireNonNull(at, "at");
    if (transactionHash == null) {
      throw new IllegalStateException("The hash was not sent in any transaction");
    }
    return new IntegrityRecord(recordHash, AnchorStatus.ANCHORED, transactionHash, at, attempts);
  }

  /** The blockchain rejected the hash or did not answer; it will be sent again. */
  public IntegrityRecord failed() {
    return new IntegrityRecord(recordHash, AnchorStatus.FAILED, null, null, attempts);
  }

  public boolean isAnchored() {
    return status == AnchorStatus.ANCHORED;
  }

  /** The transaction that carries the hash, once it was sent. */
  public Optional<String> transaction() {
    return Optional.ofNullable(transactionHash);
  }
}
