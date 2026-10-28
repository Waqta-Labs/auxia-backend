package com.waqtalabs.auxia.traceability.application.eventhandlers;

import com.waqtalabs.auxia.traceability.application.commandhandlers.AnchorDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commands.AnchorDeliveryCommand;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Tries again, periodically, the deliveries whose hash is not anchored: the failed ones and the
 * ones whose transaction was not confirmed yet. It leaves alone the deliveries registered during
 * the last interval, which the event handler may still be anchoring, and gives up on a delivery
 * after the maximum number of transactions.
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

  @Scheduled(
      fixedDelayString = "${auxia.traceability.anchoring.retry-interval}",
      initialDelayString = "${auxia.traceability.anchoring.retry-interval}")
  public void retryPendingAnchoring() {
    LocalDateTime registeredBefore = LocalDateTime.now(clock).minus(retryInterval);
    deliveryRepository.findPendingAnchoring().stream()
        .filter(delivery -> isDue(delivery, registeredBefore))
        .forEach(this::anchor);
  }

  private boolean isDue(Delivery delivery, LocalDateTime registeredBefore) {
    boolean registeredLongAgo =
        delivery.registration().orElseThrow().receivedAt().isBefore(registeredBefore);
    boolean waitingForItsTransaction =
        delivery.integrity().orElseThrow().transaction().isPresent();
    return registeredLongAgo
        && (waitingForItsTransaction
            || delivery.integrity().orElseThrow().attempts() < maxAttempts);
  }

  private void anchor(Delivery delivery) {
    try {
      anchorDelivery.handle(new AnchorDeliveryCommand(delivery.id()));
    } catch (RuntimeException ex) {
      LOGGER.error("Delivery {} could not be anchored", delivery.id().value(), ex);
    }
  }
}
