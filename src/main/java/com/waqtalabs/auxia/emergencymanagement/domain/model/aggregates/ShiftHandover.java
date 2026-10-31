package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ShiftHandoverToSelfException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shift change between two authorities of the same organization (US-23). The zones the outgoing
 * authority was following pass to the incoming one, so their follow-up is not lost.
 */
public final class ShiftHandover {

  private final ShiftHandoverId id;
  private final OrganizationId organizationId;
  private final AuthorityId outgoing;
  private final AuthorityId incoming;
  private final LocalDateTime handedOverAt;
  private final String notes;
  private final List<ZoneId> zoneIds;

  private ShiftHandover(
      ShiftHandoverId id,
      OrganizationId organizationId,
      AuthorityId outgoing,
      AuthorityId incoming,
      LocalDateTime handedOverAt,
      String notes,
      List<ZoneId> zoneIds) {
    this.id = Objects.requireNonNull(id, "id");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.outgoing = Objects.requireNonNull(outgoing, "outgoing");
    this.incoming = Objects.requireNonNull(incoming, "incoming");
    if (outgoing.equals(incoming)) {
      throw new ShiftHandoverToSelfException(outgoing);
    }
    this.handedOverAt = Objects.requireNonNull(handedOverAt, "handedOverAt");
    this.notes = notes == null || notes.isBlank() ? null : notes.strip();
    this.zoneIds = List.copyOf(zoneIds);
  }

  /**
   * Records the shift change. It may have no zones, if the outgoing authority was following none.
   *
   * @param zoneIds zones of active emergencies that pass to the incoming authority
   * @param notes what the incoming authority should know; optional
   * @throws ShiftHandoverToSelfException if both authorities are the same
   */
  public static ShiftHandover record(
      OrganizationId organizationId,
      AuthorityId outgoing,
      AuthorityId incoming,
      List<ZoneId> zoneIds,
      String notes,
      LocalDateTime handedOverAt) {
    return new ShiftHandover(
        ShiftHandoverId.generate(),
        organizationId,
        outgoing,
        incoming,
        handedOverAt,
        notes,
        zoneIds);
  }

  /** Rebuilds a shift handover that is already stored. */
  public static ShiftHandover reconstitute(
      ShiftHandoverId id,
      OrganizationId organizationId,
      AuthorityId outgoing,
      AuthorityId incoming,
      LocalDateTime handedOverAt,
      String notes,
      List<ZoneId> zoneIds) {
    return new ShiftHandover(
        id, organizationId, outgoing, incoming, handedOverAt, notes, zoneIds);
  }

  public ShiftHandoverId id() {
    return id;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public AuthorityId outgoing() {
    return outgoing;
  }

  public AuthorityId incoming() {
    return incoming;
  }

  public LocalDateTime handedOverAt() {
    return handedOverAt;
  }

  public Optional<String> notes() {
    return Optional.ofNullable(notes);
  }

  public List<ZoneId> zoneIds() {
    return zoneIds;
  }
}
