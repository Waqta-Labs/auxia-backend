package com.waqtalabs.auxia.traceability.application.commandhandlers;

import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.traceability.application.commands.AnchorDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort;
import com.waqtalabs.auxia.traceability.application.ports.BlockchainLedgerPort.TransactionOutcome;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryAnchoringFailed;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryCertified;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryNotFoundException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Anchors the hash of a registered delivery in the {@code DeliveryRegistry} contract (US-16).
 *
 * <p>The transaction is stored as soon as it is sent, before waiting for its block. If the wait
 * ends first, or the process stops, the next attempt checks that transaction instead of sending
 * the hash again. A rejected transaction, or a network that does not answer, leaves the anchoring
 * {@code FAILED}; {@code PendingAnchoringScheduler} tries again later.
 *
 * <p>It does not run in one transaction: each change is saved as soon as it happens, because a
 * transaction of the blockchain cannot be undone by a rollback of the database.
 */
@Service
public class AnchorDeliveryCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnchorDeliveryCommandHandler.class);

  private final DeliveryRepository deliveryRepository;
  private final BlockchainLedgerPort ledger;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public AnchorDeliveryCommandHandler(
      DeliveryRepository deliveryRepository,
      BlockchainLedgerPort ledger,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.deliveryRepository = deliveryRepository;
    this.ledger = ledger;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  /** The delivery after the attempt; one already certified is returned as it is. */
  public Delivery handle(AnchorDeliveryCommand command) {
    Delivery delivery =
        deliveryRepository
            .findById(command.deliveryId())
            .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));
    if (!delivery.awaitsAnchoring()) {
      return delivery;
    }
    IntegrityRecord integrity = delivery.integrity().orElseThrow();
    Optional<String> sent = integrity.transaction();
    String transaction;
    if (sent.isPresent()) {
      transaction = sent.get();
    } else {
      try {
        Optional<HashValue> anchored = ledger.deliveryHash(delivery.id());
        if (anchored.isPresent()) {
          LOGGER.error(
              "Delivery {} already has hash {} in the contract, from a transaction it did not"
                  + " keep; its own hash is {}",
              delivery.id().value(),
              anchored.get().value(),
              integrity.recordHash().value());
          return failed(delivery);
        }
        transaction = ledger.registerDelivery(delivery.id(), integrity.recordHash());
      } catch (ExternalServiceUnavailableException ex) {
        LOGGER.warn("Delivery {} was not anchored: {}", delivery.id().value(), ex.getMessage());
        return failed(delivery);
      }
      delivery.markSubmitted(transaction);
      delivery = deliveryRepository.save(delivery);
    }
    return confirm(delivery, transaction);
  }

  private Delivery confirm(Delivery delivery, String transaction) {
    TransactionOutcome outcome;
    try {
      outcome = ledger.confirmation(transaction);
    } catch (ExternalServiceUnavailableException ex) {
      LOGGER.warn("Transaction {} could not be checked: {}", transaction, ex.getMessage());
      return delivery;
    }
    return switch (outcome) {
      case CONFIRMED -> {
        delivery.markAnchored(LocalDateTime.now(clock));
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publish(
            new DeliveryCertified(saved.id(), saved.zoneId(), transaction));
        yield saved;
      }
      case REVERTED -> {
        LOGGER.warn(
            "Transaction {} of delivery {} was reverted", transaction, delivery.id().value());
        yield failed(delivery);
      }
      case PENDING -> delivery;
    };
  }

  private Delivery failed(Delivery delivery) {
    delivery.markAnchoringFailed();
    Delivery saved = deliveryRepository.save(delivery);
    eventPublisher.publish(
        new DeliveryAnchoringFailed(saved.id(), saved.integrity().orElseThrow().attempts()));
    return saved;
  }
}
