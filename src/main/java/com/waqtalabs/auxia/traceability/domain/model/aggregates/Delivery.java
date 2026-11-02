package com.waqtalabs.auxia.traceability.domain.model.aggregates;

import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryAlreadyRegisteredException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DeliveryDateInFutureException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DuplicateDeliveryResourceException;
import com.waqtalabs.auxia.traceability.domain.exceptions.DuplicateEvidenceException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EmptyDeliveryException;
import com.waqtalabs.auxia.traceability.domain.exceptions.EvidenceRequiredException;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.services.HashingService;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Record of the delivery of a distribution plan, from the assignment of its brigade to its
 * certification in the blockchain.
 *
 * <ul>
 *   <li>Each plan has one delivery, opened in {@code INITIATED} when a brigade is assigned.
 *   <li>It is registered once, with at least one evidence (US-15). Registering computes the hash
 *       of its {@link CanonicalDeliveryRecord}, which has no personal data (TS-C02, US-18).
 *   <li>Its hash is anchored in the blockchain: the delivery is {@code CERTIFIED} once the
 *       transaction is confirmed (US-16). Until then it stays {@code REGISTERED}, and a failed
 *       anchoring is retried.
 * </ul>
 */
public final class Delivery {

  private final DeliveryId id;
  private final DistributionPlanId distributionPlanId;
  private final ZoneId zoneId;
  private final BrigadeId brigadeId;
  private final OrganizationId organizationId;
  private final List<DeliveryItem> items;
  private final List<Evidence> evidences;
  private DeliveryStatus status;
  private UserId registeredBy;
  private LocalDateTime deliveredAt;
  private LocalDateTime receivedAt;
  private IntegrityRecord integrity;

  private Delivery(
      DeliveryId id,
      DistributionPlanId distributionPlanId,
      ZoneId zoneId,
      BrigadeId brigadeId,
      OrganizationId organizationId,
      DeliveryStatus status,
      Registration registration,
      List<DeliveryItem> items,
      List<Evidence> evidences,
      IntegrityRecord integrity) {
    this.id = Objects.requireNonNull(id, "id");
    this.distributionPlanId = Objects.requireNonNull(distributionPlanId, "distributionPlanId");
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.brigadeId = Objects.requireNonNull(brigadeId, "brigadeId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.status = Objects.requireNonNull(status, "status");
    boolean registered = registration != null && integrity != null;
    if (status == DeliveryStatus.INITIATED
        ? registration != null || integrity != null
        : !registered) {
      throw new IllegalArgumentException("Only a registered delivery has a registration");
    }
    if (status == DeliveryStatus.CERTIFIED && !integrity.isAnchored()) {
      throw new IllegalArgumentException("A certified delivery has its hash anchored");
    }
    if (registration != null) {
      this.registeredBy = registration.registeredBy();
      this.deliveredAt = registration.deliveredAt();
      this.receivedAt = registration.receivedAt();
    }
    this.items = new ArrayList<>(items);
    this.evidences = new ArrayList<>(evidences);
    this.integrity = integrity;
  }

  /** Opens the delivery of a plan once a brigade is assigned to it. */
  public static Delivery initiate(
      DistributionPlanId distributionPlanId,
      ZoneId zoneId,
      BrigadeId brigadeId,
      OrganizationId organizationId) {
    return new Delivery(
        DeliveryId.generate(),
        distributionPlanId,
        zoneId,
        brigadeId,
        organizationId,
        DeliveryStatus.INITIATED,
        null,
        List.of(),
        List.of(),
        null);
  }

  /**
   * Rebuilds a delivery that is already stored.
   *
   * @param registration {@code null} while the delivery is {@code INITIATED}
   * @param integrity {@code null} while the delivery is {@code INITIATED}
   */
  public static Delivery reconstitute(
      DeliveryId id,
      DistributionPlanId distributionPlanId,
      ZoneId zoneId,
      BrigadeId brigadeId,
      OrganizationId organizationId,
      DeliveryStatus status,
      Registration registration,
      List<DeliveryItem> items,
      List<Evidence> evidences,
      IntegrityRecord integrity) {
    return new Delivery(
        id,
        distributionPlanId,
        zoneId,
        brigadeId,
        organizationId,
        status,
        registration,
        items,
        evidences,
        integrity);
  }

  /**
   * Registers what was delivered, with its evidence, and computes the hash of the record. The
   * delivery date is kept to the second, the precision of the record.
   *
   * @param receivedAt when the backend received the registration, which can be later than the
   *     delivery if the brigade had no signal
   * @throws DeliveryAlreadyRegisteredException if it is no longer {@code INITIATED}
   * @throws EvidenceRequiredException if there is no evidence
   * @throws EmptyDeliveryException if no resource was delivered
   * @throws DuplicateDeliveryResourceException if a resource appears twice
   * @throws DuplicateEvidenceException if the same file appears twice
   * @throws DeliveryDateInFutureException if the delivery is after its registration
   */
  public void register(
      List<DeliveryItem> deliveredItems,
      List<Evidence> attachedEvidences,
      UserId registeredBy,
      LocalDateTime deliveredAt,
      LocalDateTime receivedAt,
      HashingService hashing) {
    Objects.requireNonNull(registeredBy, "registeredBy");
    Objects.requireNonNull(deliveredAt, "deliveredAt");
    Objects.requireNonNull(receivedAt, "receivedAt");
    if (status != DeliveryStatus.INITIATED) {
      throw new DeliveryAlreadyRegisteredException(id, status);
    }
    if (attachedEvidences.isEmpty()) {
      throw new EvidenceRequiredException(id);
    }
    if (deliveredItems.isEmpty()) {
      throw new EmptyDeliveryException(id);
    }
    Set<ResourceId> resources = new HashSet<>();
    deliveredItems.forEach(
        item -> {
          if (!resources.add(item.resourceId())) {
            throw new DuplicateDeliveryResourceException(item.resourceId());
          }
        });
    Set<HashValue> files = new HashSet<>();
    attachedEvidences.forEach(
        evidence -> {
          if (!files.add(evidence.contentHash())) {
            throw new DuplicateEvidenceException(evidence.contentHash());
          }
        });
    LocalDateTime delivered = deliveredAt.truncatedTo(ChronoUnit.SECONDS);
    LocalDateTime received = receivedAt.truncatedTo(ChronoUnit.MICROS);
    if (delivered.isAfter(received)) {
      throw new DeliveryDateInFutureException(deliveredAt);
    }
    items.addAll(deliveredItems);
    evidences.addAll(attachedEvidences);
    this.registeredBy = registeredBy;
    this.deliveredAt = delivered;
    this.receivedAt = received;
    this.integrity = IntegrityRecord.pending(hashing.hashRecord(canonicalRecord()));
    this.status = DeliveryStatus.REGISTERED;
  }

  /** Keeps the transaction that carries the hash, before it is confirmed. */
  public void markSubmitted(String transactionHash) {
    requireRegistered();
    integrity = integrity.submitted(transactionHash);
  }

  /** The transaction with the hash is confirmed: the delivery is certified. */
  public void markAnchored(LocalDateTime anchoredAt) {
    requireRegistered();
    integrity = integrity.anchored(anchoredAt);
    status = DeliveryStatus.CERTIFIED;
  }

  /** The blockchain rejected the hash or did not answer; it will be sent again. */
  public void markAnchoringFailed() {
    requireRegistered();
    integrity = integrity.failed();
  }

  /** The record of the delivery as it is now, with the hashes of its evidence files. */
  public CanonicalDeliveryRecord canonicalRecord() {
    return canonicalRecord(evidences.stream().map(Evidence::contentHash).toList());
  }

  /**
   * The record of the delivery with other hashes for its files, computed again from the stored
   * files to verify that none changed (US-17).
   */
  public CanonicalDeliveryRecord canonicalRecord(List<HashValue> evidenceHashes) {
    if (deliveredAt == null) {
      throw new IllegalStateException("Delivery " + id.value() + " is not registered");
    }
    return new CanonicalDeliveryRecord(
        id,
        distributionPlanId,
        zoneId,
        items.stream()
            .map(item -> new CanonicalDeliveryRecord.Line(item.resourceId(), item.quantity()))
            .toList(),
        evidenceHashes,
        deliveredAt);
  }

  public boolean isInitiated() {
    return status == DeliveryStatus.INITIATED;
  }

  /** Registered, and its hash is not anchored yet. */
  public boolean awaitsAnchoring() {
    return status == DeliveryStatus.REGISTERED;
  }

  public boolean belongsTo(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId);
  }

  private void requireRegistered() {
    if (status != DeliveryStatus.REGISTERED) {
      throw new IllegalStateException("Delivery " + id.value() + " is " + status);
    }
  }

  public DeliveryId id() {
    return id;
  }

  public DistributionPlanId distributionPlanId() {
    return distributionPlanId;
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public BrigadeId brigadeId() {
    return brigadeId;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public DeliveryStatus status() {
    return status;
  }

  /** Who registered the delivery and when; empty while it is {@code INITIATED}. */
  public Optional<Registration> registration() {
    return registeredBy == null
        ? Optional.empty()
        : Optional.of(new Registration(registeredBy, deliveredAt, receivedAt));
  }

  public List<DeliveryItem> items() {
    return List.copyOf(items);
  }

  public List<Evidence> evidences() {
    return List.copyOf(evidences);
  }

  /** Hash of the record and its anchoring; empty while it is {@code INITIATED}. */
  public Optional<IntegrityRecord> integrity() {
    return Optional.ofNullable(integrity);
  }

  /**
   * Who registered the delivery, when it happened in the field and when the backend received it.
   */
  public record Registration(
      UserId registeredBy, LocalDateTime deliveredAt, LocalDateTime receivedAt) {

    public Registration {
      Objects.requireNonNull(registeredBy, "registeredBy");
      Objects.requireNonNull(deliveredAt, "deliveredAt");
      Objects.requireNonNull(receivedAt, "receivedAt");
    }
  }
}
