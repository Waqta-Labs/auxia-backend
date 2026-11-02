package com.waqtalabs.auxia.traceability.application;

import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test double of the {@code DeliveryRegistry} contract. A sent hash is stored when its
 * transaction is confirmed, which is the default outcome. {@link #willRevert()}, {@link
 * #willStayPending()} and {@link #becomeUnavailable()} change what happens next.
 */
public class FakeBlockchainLedger implements BlockchainLedgerPort {

  private final Map<DeliveryId, HashValue> anchored = new ConcurrentHashMap<>();
  private final Map<String, Pending> transactions = new ConcurrentHashMap<>();
  private final List<String> sent = new CopyOnWriteArrayList<>();
  private final AtomicLong nonce = new AtomicLong();
  private volatile TransactionOutcome outcome = TransactionOutcome.CONFIRMED;
  private volatile boolean unavailable;

  public void willRevert() {
    outcome = TransactionOutcome.REVERTED;
  }

  public void willStayPending() {
    outcome = TransactionOutcome.PENDING;
  }

  public void willConfirm() {
    outcome = TransactionOutcome.CONFIRMED;
  }

  public void becomeUnavailable() {
    unavailable = true;
  }

  public void becomeAvailable() {
    unavailable = false;
  }

  /** Forgets every hash and transaction, for tests that share the application context. */
  public void reset() {
    anchored.clear();
    transactions.clear();
    sent.clear();
    outcome = TransactionOutcome.CONFIRMED;
    unavailable = false;
  }

  /** Writes a hash in the contract directly, as if another transaction had stored it. */
  public void store(DeliveryId deliveryId, HashValue hash) {
    anchored.put(deliveryId, hash);
  }

  /** Transactions sent, in order. */
  public List<String> sentTransactions() {
    return new ArrayList<>(sent);
  }

  @Override
  public String registerDelivery(DeliveryId deliveryId, HashValue hash) {
    failIfUnavailable();
    String transaction = "0x%064x".formatted(nonce.incrementAndGet());
    transactions.put(transaction, new Pending(deliveryId, hash));
    sent.add(transaction);
    return transaction;
  }

  @Override
  public TransactionOutcome confirmation(String transactionHash) {
    failIfUnavailable();
    Pending pending = transactions.get(transactionHash);
    if (pending == null || outcome == TransactionOutcome.PENDING) {
      return TransactionOutcome.PENDING;
    }
    if (outcome == TransactionOutcome.REVERTED || anchored.containsKey(pending.deliveryId())) {
      return TransactionOutcome.REVERTED;
    }
    anchored.put(pending.deliveryId(), pending.hash());
    return TransactionOutcome.CONFIRMED;
  }

  @Override
  public Optional<HashValue> deliveryHash(DeliveryId deliveryId) {
    failIfUnavailable();
    return Optional.ofNullable(anchored.get(deliveryId));
  }

  private void failIfUnavailable() {
    if (unavailable) {
      throw new ExternalServiceUnavailableException("Blockchain", null);
    }
  }

  private record Pending(DeliveryId deliveryId, HashValue hash) {}
}
