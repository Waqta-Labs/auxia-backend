package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import java.util.Optional;

/**
 * The {@code DeliveryRegistry} smart contract. Traceability adopts its model as it is
 * (Conformist): an identifier of 16 bytes and a hash of 32. Every method throws {@link
 * ExternalServiceUnavailableException} when the network does not answer.
 */
public interface BlockchainLedgerPort {

  /**
   * Sends {@code registerDelivery(deliveryId, hash)} and returns the hash of its transaction,
   * without waiting for it to be confirmed.
   */
  String registerDelivery(DeliveryId deliveryId, HashValue hash);

  /** Waits, for a limited time, until the transaction is confirmed or rejected. */
  TransactionOutcome confirmation(String transactionHash);

  /** {@code getDeliveryHash(deliveryId)}; empty when no hash was anchored for the delivery. */
  Optional<HashValue> deliveryHash(DeliveryId deliveryId);

  /** What happened to a transaction. */
  enum TransactionOutcome {
    CONFIRMED,
    /** The contract rejected it, for example because the delivery already had a hash. */
    REVERTED,
    /** Still not in a block when the wait ended. */
    PENDING
  }
}
