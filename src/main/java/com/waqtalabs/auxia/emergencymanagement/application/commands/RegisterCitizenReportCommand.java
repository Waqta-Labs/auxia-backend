package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Registers the SOS of a citizen, sent without signing in.
 *
 * @param reportedAt when the citizen sent it, by the clock of the phone
 * @param location empty when the phone had no GPS
 * @param relatedReportId earlier SOS this one adds detail to
 * @param sender address of the client, for the limit of SOS per sender
 * @param relayed whether another phone passed the SOS by Mesh to the one that uploads it
 */
public record RegisterCitizenReportCommand(
    CitizenReportId clientReportId,
    SosCategory category,
    String text,
    Instant reportedAt,
    Optional<Geolocation> location,
    VulnerablePeople people,
    Set<AccessCondition> accessConditions,
    Optional<CitizenReportId> relatedReportId,
    String sender,
    boolean relayed) {

  /** An SOS uploaded by the phone that sent it. */
  public RegisterCitizenReportCommand(
      CitizenReportId clientReportId,
      SosCategory category,
      String text,
      Instant reportedAt,
      Optional<Geolocation> location,
      VulnerablePeople people,
      Set<AccessCondition> accessConditions,
      Optional<CitizenReportId> relatedReportId,
      String sender) {
    this(
        clientReportId,
        category,
        text,
        reportedAt,
        location,
        people,
        accessConditions,
        relatedReportId,
        sender,
        false);
  }

  public RegisterCitizenReportCommand {
    Objects.requireNonNull(clientReportId, "clientReportId");
    Objects.requireNonNull(reportedAt, "reportedAt");
    Objects.requireNonNull(location, "location");
    Objects.requireNonNull(relatedReportId, "relatedReportId");
    accessConditions = accessConditions == null ? Set.of() : Set.copyOf(accessConditions);
  }
}
