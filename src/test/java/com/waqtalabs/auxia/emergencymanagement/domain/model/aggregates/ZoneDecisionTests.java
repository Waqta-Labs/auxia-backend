package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZoneDecisionTests {

  private static final ZoneId ZONE = ZoneId.generate();
  private static final AuthorityId AUTHORITY = new AuthorityId(UUID.randomUUID());
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 15, 10, 30);

  @Test
  void aDecisionKeepsWhoTookItWhenAndWhy() {
    UUID plan = UUID.randomUUID();

    ZoneDecision decision =
        ZoneDecision.record(
            ZONE, ZoneDecisionType.PLAN_REJECTED, AUTHORITY, NOW, " Stock comprometido ", plan);

    assertThat(decision.zoneId()).isEqualTo(ZONE);
    assertThat(decision.type()).isEqualTo(ZoneDecisionType.PLAN_REJECTED);
    assertThat(decision.decidedBy()).isEqualTo(AUTHORITY);
    assertThat(decision.decidedAt()).isEqualTo(NOW);
    assertThat(decision.detail()).contains("Stock comprometido");
    assertThat(decision.subjectId()).contains(plan);
    assertThat(decision.handedOverTo()).isEmpty();
  }

  @Test
  void aShiftHandoverNamesTheNextResponsible() {
    var next = new AuthorityId(UUID.randomUUID());
    var handover = ShiftHandoverId.generate();

    ZoneDecision decision =
        ZoneDecision.shiftHandedOver(ZONE, handover, AUTHORITY, next, NOW, null);

    assertThat(decision.type()).isEqualTo(ZoneDecisionType.SHIFT_HANDED_OVER);
    assertThat(decision.decidedBy()).isEqualTo(AUTHORITY);
    assertThat(decision.handedOverTo()).contains(next);
    assertThat(decision.subjectId()).contains(handover.value());
    assertThat(decision.detail()).isEmpty();
  }

  @Test
  void onlyAShiftHandoverHasANextResponsible() {
    assertThatThrownBy(
            () ->
                ZoneDecision.reconstitute(
                    ZoneDecisionId.generate(),
                    ZONE,
                    ZoneDecisionType.MANUAL_PRIORITY_SET,
                    AUTHORITY,
                    NOW,
                    "Riesgo inminente",
                    null,
                    new AuthorityId(UUID.randomUUID())))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                ZoneDecision.record(
                    ZONE, ZoneDecisionType.SHIFT_HANDED_OVER, AUTHORITY, NOW, null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
