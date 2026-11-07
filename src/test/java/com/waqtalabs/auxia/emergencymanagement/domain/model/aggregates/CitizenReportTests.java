package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CitizenReportTests {

  private static final LocalDateTime RECEIVED_AT = LocalDateTime.of(2026, 11, 6, 9, 41);

  private final CitizenReportId id = new CitizenReportId(UUID.randomUUID());

  @Test
  void sosWithoutLocationNorDetailOfAnotherOneHasNeither() {
    CitizenReport sos = receive(id, "  Necesitamos agua  ", null, List.of());

    assertThat(sos.text()).isEqualTo("Necesitamos agua");
    assertThat(sos.location()).isEmpty();
    assertThat(sos.relatedReportId()).isEmpty();
    assertThat(sos.fieldReportId()).isEmpty();
    assertThat(sos.accessConditions()).isEmpty();
  }

  @Test
  void repeatedAccessConditionsAreKeptOnce() {
    CitizenReport sos =
        receive(
            id,
            "Vía bloqueada",
            null,
            List.of(
                AccessCondition.DAMAGED_BUILDING,
                AccessCondition.ROAD_BLOCKED,
                AccessCondition.DAMAGED_BUILDING));

    assertThat(sos.accessConditions())
        .containsExactly(AccessCondition.ROAD_BLOCKED, AccessCondition.DAMAGED_BUILDING);
  }

  @Test
  void blankTextOrDetailOfItselfIsRejected() {
    assertThatThrownBy(() -> receive(id, " ", null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> receive(id, "Más heridos", id, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negativePeopleAreRejected() {
    assertThatThrownBy(() -> new VulnerablePeople(0, -1, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportedTimeIsThatOfThePhoneUnlessItIsAheadOfTheServer() {
    LocalDateTime queuedOffline = RECEIVED_AT.minusHours(3);

    assertThat(CitizenReport.reportedTime(queuedOffline, RECEIVED_AT)).isEqualTo(queuedOffline);
    assertThat(CitizenReport.reportedTime(RECEIVED_AT.plusHours(5), RECEIVED_AT))
        .isEqualTo(RECEIVED_AT);
  }

  private static CitizenReport receive(
      CitizenReportId id,
      String text,
      CitizenReportId relatedReportId,
      List<AccessCondition> accessConditions) {
    return CitizenReport.receive(
        id,
        SosCategory.SUPPLIES,
        text,
        RECEIVED_AT,
        RECEIVED_AT,
        null,
        VulnerablePeople.NONE,
        accessConditions,
        relatedReportId,
        null);
  }
}
