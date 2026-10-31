package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ShiftHandoverToSelfException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ShiftHandoverTests {

  private static final OrganizationId COER = new OrganizationId(UUID.randomUUID());
  private static final AuthorityId OUTGOING = new AuthorityId(UUID.randomUUID());
  private static final AuthorityId INCOMING = new AuthorityId(UUID.randomUUID());
  private static final LocalDateTime END_OF_SHIFT = LocalDateTime.of(2026, 3, 15, 20, 0);

  @Test
  void recordsTheZonesThatPassToTheIncomingAuthority() {
    List<ZoneId> zones = new ArrayList<>(List.of(ZoneId.generate(), ZoneId.generate()));

    ShiftHandover handover =
        ShiftHandover.record(
            COER, OUTGOING, INCOMING, zones, "  Sin acceso por la carretera ", END_OF_SHIFT);
    zones.clear();

    assertThat(handover.id()).isNotNull();
    assertThat(handover.outgoing()).isEqualTo(OUTGOING);
    assertThat(handover.incoming()).isEqualTo(INCOMING);
    assertThat(handover.zoneIds()).hasSize(2);
    assertThat(handover.notes()).contains("Sin acceso por la carretera");
    assertThat(handover.handedOverAt()).isEqualTo(END_OF_SHIFT);
  }

  @Test
  void notesAreOptionalAndAShiftMayHaveNoZones() {
    ShiftHandover handover =
        ShiftHandover.record(COER, OUTGOING, INCOMING, List.of(), " ", END_OF_SHIFT);

    assertThat(handover.notes()).isEmpty();
    assertThat(handover.zoneIds()).isEmpty();
  }

  @Test
  void theShiftIsNeverHandedOverToTheSameAuthority() {
    assertThatThrownBy(
            () -> ShiftHandover.record(COER, OUTGOING, OUTGOING, List.of(), null, END_OF_SHIFT))
        .isInstanceOf(ShiftHandoverToSelfException.class);
  }
}
