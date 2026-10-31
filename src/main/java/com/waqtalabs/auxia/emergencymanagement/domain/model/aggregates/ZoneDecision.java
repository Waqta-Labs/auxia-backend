package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entry of the history of a zone: a decision of an authority, when it was taken and why (US-20,
 * TS-C01). Entries are only added, never changed, so the history keeps every decision even when
 * the zone or the plan changes afterwards.
 */
public final class ZoneDecision {

  private final ZoneDecisionId id;
  private final ZoneId zoneId;
  private final ZoneDecisionType type;
  private final AuthorityId decidedBy;
  private final LocalDateTime decidedAt;
  private final String detail;
  private final UUID subjectId;
  private final AuthorityId handedOverTo;

  private ZoneDecision(
      ZoneDecisionId id,
      ZoneId zoneId,
      ZoneDecisionType type,
      AuthorityId decidedBy,
      LocalDateTime decidedAt,
      String detail,
      UUID subjectId,
      AuthorityId handedOverTo) {
    this.id = Objects.requireNonNull(id, "id");
    this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    this.type = Objects.requireNonNull(type, "type");
    this.decidedBy = Objects.requireNonNull(decidedBy, "decidedBy");
    this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt");
    if ((type == ZoneDecisionType.SHIFT_HANDED_OVER) != (handedOverTo != null)) {
      throw new IllegalArgumentException("Only a shift handover names the next responsible");
    }
    this.detail = detail == null || detail.isBlank() ? null : detail.strip();
    this.subjectId = subjectId;
    this.handedOverTo = handedOverTo;
  }

  /**
   * Records a decision on the zone.
   *
   * @param detail reason or justification of the decision; {@code null} if it has none
   * @param subjectId plan or field report the decision is about; {@code null} for the zone itself
   */
  public static ZoneDecision record(
      ZoneId zoneId,
      ZoneDecisionType type,
      AuthorityId decidedBy,
      LocalDateTime decidedAt,
      String detail,
      UUID subjectId) {
    return new ZoneDecision(
        ZoneDecisionId.generate(), zoneId, type, decidedBy, decidedAt, detail, subjectId, null);
  }

  /** Records that the zone passed from one authority to the next at a shift change (US-23). */
  public static ZoneDecision shiftHandedOver(
      ZoneId zoneId,
      ShiftHandoverId handoverId,
      AuthorityId outgoing,
      AuthorityId incoming,
      LocalDateTime handedOverAt,
      String notes) {
    return new ZoneDecision(
        ZoneDecisionId.generate(),
        zoneId,
        ZoneDecisionType.SHIFT_HANDED_OVER,
        outgoing,
        handedOverAt,
        notes,
        handoverId.value(),
        Objects.requireNonNull(incoming, "incoming"));
  }

  /** Rebuilds an entry that is already stored. */
  public static ZoneDecision reconstitute(
      ZoneDecisionId id,
      ZoneId zoneId,
      ZoneDecisionType type,
      AuthorityId decidedBy,
      LocalDateTime decidedAt,
      String detail,
      UUID subjectId,
      AuthorityId handedOverTo) {
    return new ZoneDecision(
        id, zoneId, type, decidedBy, decidedAt, detail, subjectId, handedOverTo);
  }

  public ZoneDecisionId id() {
    return id;
  }

  public ZoneId zoneId() {
    return zoneId;
  }

  public ZoneDecisionType type() {
    return type;
  }

  public AuthorityId decidedBy() {
    return decidedBy;
  }

  public LocalDateTime decidedAt() {
    return decidedAt;
  }

  /** Reason or justification given by the authority. */
  public Optional<String> detail() {
    return Optional.ofNullable(detail);
  }

  /** Plan, field report or shift handover the decision is about. */
  public Optional<UUID> subjectId() {
    return Optional.ofNullable(subjectId);
  }

  /** The next responsible of the zone, only in a shift handover. */
  public Optional<AuthorityId> handedOverTo() {
    return Optional.ofNullable(handedOverTo);
  }
}
