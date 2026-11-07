package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** What the citizen marked in the SOS (A_04); the text is the one of the field report. */
public record SosDetailsResource(
    UUID clientReportId,
    SosCategory category,
    LocalDateTime reportedAt,
    @Schema(description = "Null when the phone had no location") GeolocationResource location,
    VulnerablePeopleResource people,
    List<AccessCondition> accessConditions,
    @Schema(description = "The earlier SOS that this one adds detail to, if any")
        UUID relatedReportId) {

  public static SosDetailsResource from(CitizenReport sos) {
    return new SosDetailsResource(
        sos.id().value(),
        sos.category(),
        sos.reportedAt(),
        sos.location()
            .map(place -> new GeolocationResource(place.latitude(), place.longitude()))
            .orElse(null),
        new VulnerablePeopleResource(
            sos.people().children(), sos.people().elderly(), sos.people().injured()),
        List.copyOf(sos.accessConditions()),
        sos.relatedReportId().map(CitizenReportId::value).orElse(null));
  }
}
