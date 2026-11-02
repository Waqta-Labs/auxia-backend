package com.waqtalabs.auxia.traceability.application.commandhandlers;

import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import com.waqtalabs.auxia.shared.application.exceptions.ActionNotAllowedException;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.DeliveredQuantity;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.EvidenceFile;
import com.waqtalabs.auxia.traceability.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.traceability.application.ports.DistributionPlanPort;
import com.waqtalabs.auxia.traceability.application.ports.EvidenceStoragePort;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryConfirmed;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveredBeyondPlanException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryNotFoundException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EvidenceRequiredException;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.DeliveryItem;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a delivery with its evidence (US-15, TS-05): checks the role of the user, stores each
 * file, computes its hash, registers the delivery and announces it. Its hash is anchored in the
 * blockchain after the commit, so the brigade does not wait for the network.
 *
 * <p>The field app keeps a registration while it has no signal and sends it when it reconnects
 * (TS-C06), so it may arrive twice. A delivery that is already registered is returned as it is,
 * without failing and without registering it again.
 */
@Service
public class RegisterDeliveryCommandHandler {

  private final DeliveryRepository deliveryRepository;
  private final DistributionPlanPort distributionPlans;
  private final EvidenceStoragePort evidenceStorage;
  private final AuthorizationPort authorization;
  private final HashingService hashing;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public RegisterDeliveryCommandHandler(
      DeliveryRepository deliveryRepository,
      DistributionPlanPort distributionPlans,
      EvidenceStoragePort evidenceStorage,
      AuthorizationPort authorization,
      HashingService hashing,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.deliveryRepository = deliveryRepository;
    this.distributionPlans = distributionPlans;
    this.evidenceStorage = evidenceStorage;
    this.authorization = authorization;
    this.hashing = hashing;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  /**
   * @throws ActionNotAllowedException if the user is no longer an active brigade member or
   *     authority
   * @throws DeliveryNotFoundException if the delivery is not of the organization
   * @throws EvidenceRequiredException if no evidence file has content
   * @throws DeliveredBeyondPlanException if a resource is not in the plan or exceeds it
   */
  @Transactional
  public RegisteredDelivery handle(RegisterDeliveryCommand command) {
    if (!authorization.canRegisterDeliveries(command.registeredBy())) {
      throw new ActionNotAllowedException(
          "User " + command.registeredBy().value() + " cannot register deliveries");
    }
    Delivery delivery =
        deliveryRepository
            .findById(command.deliveryId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new DeliveryNotFoundException(command.deliveryId()));
    if (!delivery.isInitiated()) {
      return new RegisteredDelivery(delivery, false);
    }
    List<EvidenceFile> files =
        command.evidences().stream().filter(file -> file.content().length > 0).toList();
    if (files.isEmpty()) {
      throw new EvidenceRequiredException(delivery.id());
    }
    files.forEach(file -> EvidenceType.of(file.contentType()));
    checkAgainstPlan(delivery, command.items());
    LocalDateTime receivedAt = LocalDateTime.now(clock);
    GeoPoint location = command.location().orElse(null);
    LocalDateTime capturedAt = command.deliveredAt().truncatedTo(ChronoUnit.SECONDS);
    List<Evidence> evidences =
        files.stream().map(file -> store(delivery, file, capturedAt, location)).toList();
    delivery.register(
        command.items().stream()
            .map(item -> DeliveryItem.of(item.resourceId(), item.quantity()))
            .toList(),
        evidences,
        command.registeredBy(),
        command.deliveredAt(),
        receivedAt,
        hashing);
    Delivery saved = deliveryRepository.save(delivery);
    eventPublisher.publish(
        new DeliveryConfirmed(
            saved.id(),
            saved.distributionPlanId(),
            saved.zoneId(),
            saved.brigadeId(),
            saved.organizationId(),
            saved.canonicalRecord().items(),
            saved.registration().orElseThrow().deliveredAt()));
    return new RegisteredDelivery(saved, true);
  }

  /** Each delivered resource must be in the plan, with at most its planned quantity. */
  private void checkAgainstPlan(Delivery delivery, List<DeliveredQuantity> items) {
    Map<ResourceId, Integer> planned =
        distributionPlans.plannedQuantities(delivery.distributionPlanId());
    items.forEach(
        item -> {
          int plannedQuantity = planned.getOrDefault(item.resourceId(), 0);
          if (item.quantity() > plannedQuantity) {
            throw new DeliveredBeyondPlanException(
                item.resourceId(), item.quantity(), plannedQuantity);
          }
        });
  }

  /** The file is named by its hash, so storing the same file twice keeps one copy. */
  private Evidence store(
      Delivery delivery, EvidenceFile file, LocalDateTime capturedAt, GeoPoint location) {
    HashValue contentHash = hashing.hashContent(file.content());
    String name = delivery.id().value() + "/" + contentHash.value();
    String storageUrl = evidenceStorage.store(name, file.contentType(), file.content());
    return Evidence.attach(
        EvidenceType.of(file.contentType()), storageUrl, contentHash, capturedAt, location);
  }

  /**
   * The delivery after the request.
   *
   * @param registeredNow false when it was already registered and nothing changed
   */
  public record RegisteredDelivery(Delivery delivery, boolean registeredNow) {}
}
