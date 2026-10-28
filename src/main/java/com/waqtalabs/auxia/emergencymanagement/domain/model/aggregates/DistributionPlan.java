package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanAlreadyApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotPendingException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DuplicateDistributionResourceException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmptyDistributionPlanException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.JustificationRequiredException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanModification;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Resources that an organization proposes to deliver to a zone, subject to the decision of an
 * authority. It is born {@code PROPOSED} from a recommendation; while it is proposed the
 * authority may change its quantities with a reason (US-13, US-14), and then approves it (US-12)
 * or rejects it with a reason. An approved plan becomes {@code EXECUTED} when its delivery is
 * confirmed.
 */
public final class DistributionPlan {

  private final DistributionPlanId id;
  private final EmergencyId emergencyId;
  private final ZoneId zoneId;
  private final OrganizationId organizationId;
  private final List<DistributionItem> items;
  private final Coverage coverage;
  private final String justification;
  private final String modelVersion;
  private final LocalDateTime createdAt;
  private DistributionPlanStatus status;
  private PlanModification modification;
  private PlanDecision decision;
  private String rejectedReason;

  private DistributionPlan(
      DistributionPlanId id,
      EmergencyId emergencyId,
      ZoneId zoneId,
      OrganizationId organizationId,
      List<DistributionItem> items,
      Coverage coverage,
      String justification,
      String modelVersion,
      LocalDateTime createdAt,
      DistributionPlanStatus status,
      PlanModification modification,
      PlanDecision decision,
      String rejectedReason) {
    this.id = Objects.requireNonNull(id, "id");
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.items = new ArrayList<>(items);
    this.coverage = Objects.requireNonNull(coverage, "coverage");
    if (coverage == Coverage.NONE) {
      throw new IllegalArgumentException("a zone without coverage gets no plan");
    }
    this.justification = justification;
    this.modelVersion = modelVersion;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.status = Objects.requireNonNull(status, "status");
    this.modification = modification;
    this.decision = decision;
    this.rejectedReason = rejectedReason;
  }

  /**
   * Proposes the plan of one zone from a recommendation.
   *
   * @param items recommended quantities, each one positive and of a different resource
   * @param coverage {@code FULL} or {@code PARTIAL}
   */
  public static DistributionPlan propose(
      EmergencyId emergencyId,
      ZoneId zoneId,
      OrganizationId organizationId,
      List<ResourceQuantity> items,
      Coverage coverage,
      String justification,
      String modelVersion,
      LocalDateTime proposedAt) {
    var id = DistributionPlanId.generate();
    requireDifferentResources(id, items);
    if (items.isEmpty() || items.stream().anyMatch(item -> item.quantity() == 0)) {
      throw new IllegalArgumentException("a proposed plan has positive quantities only");
    }
    return new DistributionPlan(
        id,
        emergencyId,
        zoneId,
        organizationId,
        items.stream()
            .map(item -> DistributionItem.recommended(item.resourceId(), item.quantity()))
            .toList(),
        coverage,
        justification,
        modelVersion,
        proposedAt,
        DistributionPlanStatus.PROPOSED,
        null,
        null,
        null);
  }

  /**
   * Rebuilds a plan that is already stored.
   *
   * @param modification {@code null} if the quantities were never changed
   * @param decision {@code null} while the plan is proposed
   * @param rejectedReason {@code null} unless the plan was rejected
   */
  public static DistributionPlan reconstitute(
      DistributionPlanId id,
      EmergencyId emergencyId,
      ZoneId zoneId,
      OrganizationId organizationId,
      List<DistributionItem> items,
      Coverage coverage,
      String justification,
      String modelVersion,
      LocalDateTime createdAt,
      DistributionPlanStatus status,
      PlanModification modification,
      PlanDecision decision,
      String rejectedReason) {
    return new DistributionPlan(
        id,
        emergencyId,
        zoneId,
        organizationId,
        items,
        coverage,
        justification,
        modelVersion,
        createdAt,
        status,
        modification,
        decision,
        rejectedReason);
  }

  /**
   * Changes quantities before the plan is decided. A resource not listed keeps its quantity, a
   * quantity of 0 takes a resource out of the plan, and a resource the recommendation did not
   * include is added. The recommended quantities are kept (US-13, US-14).
   *
   * @throws DistributionPlanNotPendingException if the plan was already decided
   * @throws JustificationRequiredException if the reason is blank
   * @throws DuplicateDistributionResourceException if a resource is listed twice
   * @throws EmptyDistributionPlanException if no resource is left to distribute
   */
  public void modifyItems(
      List<ResourceQuantity> changes,
      AuthorityId authority,
      String reason,
      LocalDateTime modifiedAt) {
    requirePending();
    String justified = requireJustification(reason);
    requireDifferentResources(id, changes);
    Map<ResourceId, Integer> quantities = new HashMap<>();
    items.forEach(item -> quantities.put(item.resourceId(), item.quantity()));
    changes.forEach(change -> quantities.put(change.resourceId(), change.quantity()));
    if (quantities.values().stream().noneMatch(quantity -> quantity > 0)) {
      throw new EmptyDistributionPlanException(id);
    }
    for (ResourceQuantity change : changes) {
      Optional<DistributionItem> existing = itemOf(change.resourceId());
      if (existing.isEmpty()) {
        if (change.quantity() > 0) {
          items.add(DistributionItem.added(change.resourceId(), change.quantity()));
        }
      } else if (change.quantity() == 0 && existing.get().recommendedQuantity().isEmpty()) {
        items.remove(existing.get());
      } else {
        existing.get().changeQuantity(change.quantity());
      }
    }
    modification =
        new PlanModification(
            Objects.requireNonNull(authority, "authority"), modifiedAt, justified);
  }

  /**
   * Approves the plan, which can then be executed (US-12). It always needs the authority who
   * approves it (TS-C01).
   *
   * @throws DistributionPlanAlreadyApprovedException if it was already approved
   * @throws DistributionPlanNotPendingException if it was rejected or executed
   */
  public void approve(AuthorityId authority, LocalDateTime approvedAt) {
    if (status == DistributionPlanStatus.APPROVED) {
      throw new DistributionPlanAlreadyApprovedException(id);
    }
    requirePending();
    decision = new PlanDecision(Objects.requireNonNull(authority, "authority"), approvedAt);
    status = DistributionPlanStatus.APPROVED;
  }

  /**
   * Rejects the plan with a reason that stays in the history of the zone (US-14).
   *
   * @throws DistributionPlanNotPendingException if the plan was already decided
   * @throws JustificationRequiredException if the reason is blank
   */
  public void reject(AuthorityId authority, String reason, LocalDateTime rejectedAt) {
    requirePending();
    String justified = requireJustification(reason);
    decision = new PlanDecision(Objects.requireNonNull(authority, "authority"), rejectedAt);
    rejectedReason = justified;
    status = DistributionPlanStatus.REJECTED;
  }

  /**
   * Marks the plan as executed once its delivery is confirmed.
   *
   * @throws DistributionPlanNotApprovedException if the plan is not approved
   */
  public void markExecuted() {
    if (status != DistributionPlanStatus.APPROVED) {
      throw new DistributionPlanNotApprovedException(id, status);
    }
    status = DistributionPlanStatus.EXECUTED;
  }

  /** Quantities the plan delivers now: the ones that hold stock in the inventory. */
  public List<ResourceQuantity> distributedQuantities() {
    return items.stream()
        .filter(DistributionItem::isDistributed)
        .map(item -> new ResourceQuantity(item.resourceId(), item.quantity()))
        .toList();
  }

  public boolean isPending() {
    return status == DistributionPlanStatus.PROPOSED;
  }

  public boolean isApproved() {
    return status == DistributionPlanStatus.APPROVED;
  }

  public boolean belongsTo(OrganizationId organizationId) {
    return this.organizationId.equals(organizationId);
  }

  public DistributionPlanId id() {
    return id;
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public List<DistributionItem> items() {
    return List.copyOf(items);
  }

  public Coverage coverage() {
    return coverage;
  }

  public String justification() {
    return justification;
  }

  public String modelVersion() {
    return modelVersion;
  }

  public LocalDateTime createdAt() {
    return createdAt;
  }

  public DistributionPlanStatus status() {
    return status;
  }

  /** Empty if the quantities were never changed. */
  public Optional<PlanModification> modification() {
    return Optional.ofNullable(modification);
  }

  /** Empty while the plan is proposed. */
  public Optional<PlanDecision> decision() {
    return Optional.ofNullable(decision);
  }

  /** Present only for a rejected plan. */
  public Optional<String> rejectedReason() {
    return Optional.ofNullable(rejectedReason);
  }

  private Optional<DistributionItem> itemOf(ResourceId resourceId) {
    return items.stream().filter(item -> item.resourceId().equals(resourceId)).findFirst();
  }

  private void requirePending() {
    if (!isPending()) {
      throw new DistributionPlanNotPendingException(id, status);
    }
  }

  private String requireJustification(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new JustificationRequiredException(id);
    }
    return reason.strip();
  }

  private static void requireDifferentResources(
      DistributionPlanId id, List<ResourceQuantity> quantities) {
    Set<ResourceId> resources = new HashSet<>();
    for (ResourceQuantity quantity : quantities) {
      if (!resources.add(quantity.resourceId())) {
        throw new DuplicateDistributionResourceException(id, quantity.resourceId());
      }
    }
  }
}
