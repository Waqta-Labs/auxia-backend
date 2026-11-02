package com.waqtalabs.auxia.traceability.application.eventhandlers;

import com.waqtalabs.auxia.traceability.application.commandhandlers.AnchorDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commands.AnchorDeliveryCommand;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryConfirmed;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tries again, periodically, the deliveries whose hash is not anchored: the failed ones and the
 * ones whose transaction was not confirmed yet. It leaves alone the deliveries registered during
 * the last interval, which the event handler may still be anchoring, and gives up on a delivery
 * after the maximum number of transactions.
 *
 * <p>Once no delivery is left to retry, it stops reading the database until a new delivery is
 * confirmed, so an idle API lets the database sleep. The deliveries are confirmed by this same
 * instance, which already sends the transactions of the wallet one after another.
 */
@Component
@ConditionalOnProperty(
    name = "auxia.traceability.anchoring.retry-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class PendingAnchoringScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PendingAnchoringScheduler.class);

  private final DeliveryRepository deliveryRepository;
  private final AnchorDeliveryCommandHandler anchorDelivery;
  private final Duration retryInterval;
  private final int maxAttempts;
  private final Clock clock;
  private final AtomicBoolean pendingMayExist = new AtomicBoolean(true);

  public PendingAnchoringScheduler(
      DeliveryRepository deliveryRepository,
      AnchorDeliveryCommandHandler anchorDelivery,
      @Value("${auxia.traceability.anchoring.retry-interval}") Duration retryInterval,
      @Value("${auxia.traceability.anchoring.max-attempts}") int maxAttempts,
      Clock clock) {
    this.deliveryRepository = deliveryRepository;
    this.anchorDelivery = anchorDelivery;
    this.retryInterval = retryInterval;
    this.maxAttempts = maxAttempts;
    this.clock = clock;
  }

  @EventListener
  public void on(DeliveryConfirmed event) {
    pendingMayExist.set(true);
  }

  @Scheduled(
      fixedDelayString = "${auxia.traceability.anchoring.retry-interval}",
      initialDelayString = "${auxia.traceability.anchoring.retry-interval}")
  public void retryPendingAnchoring() {
    if (!pendingMayExist.getAndSet(false)) {
      return;
    }
    List<Delivery> retryable =
        deliveryRepository.findPendingAnchoring().stream().filter(this::canBeRetried).toList();
    if (!retryable.isEmpty()) {
      pendingMayExist.set(true);
    }
    LocalDateTime registeredBefore = LocalDateTime.now(clock).minus(retryInterval);
    retryable.stream()
        .filter(delivery -> isRegisteredBefore(delivery, registeredBefore))
        .forEach(this::anchor);
  }

  private boolean canBeRetried(Delivery delivery) {
    boolean waitingForItsTransaction =
        delivery.integrity().orElseThrow().transaction().isPresent();
    return waitingForItsTransaction || delivery.integrity().orElseThrow().attempts() < maxAttempts;
  }

  private static boolean isRegisteredBefore(Delivery delivery, LocalDateTime registeredBefore) {
    return delivery.registration().orElseThrow().receivedAt().isBefore(registeredBefore);
  }

  private void anchor(Delivery delivery) {
    try {
      anchorDelivery.handle(new AnchorDeliveryCommand(delivery.id()));
    } catch (RuntimeException ex) {
      LOGGER.error("Delivery {} could not be anchored", delivery.id().value(), ex);
    }
  }
}
