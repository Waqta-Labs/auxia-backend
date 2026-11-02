package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Row of {@code emergency_management.shift_handovers}, with its zones. */
@Entity
@Table(schema = "emergency_management", name = "shift_handovers")
class ShiftHandoverJpaEntity {

  @Id UUID id;

  @Column(name = "organization_id")
  UUID organizationId;

  @Column(name = "outgoing_authority_id")
  UUID outgoingAuthorityId;

  @Column(name = "incoming_authority_id")
  UUID incomingAuthorityId;

  @Column(name = "handed_over_at")
  LocalDateTime handedOverAt;

  String notes;

  @ElementCollection
  @CollectionTable(
      schema = "emergency_management",
      name = "shift_handover_zones",
      joinColumns = @JoinColumn(name = "shift_handover_id"))
  @Column(name = "zone_id")
  List<UUID> zoneIds = new ArrayList<>();

  protected ShiftHandoverJpaEntity() {}
}
