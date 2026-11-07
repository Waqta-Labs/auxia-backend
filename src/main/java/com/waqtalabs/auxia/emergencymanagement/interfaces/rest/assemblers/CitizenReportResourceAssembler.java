package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.CitizenReportStatusResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterCitizenReportRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.VulnerablePeopleResource;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

public final class CitizenReportResourceAssembler {

  private CitizenReportResourceAssembler() {}

  /** The source of the request is ignored: an SOS of this endpoint is always of a citizen. */
  public static RegisterCitizenReportCommand toCommand(
      RegisterCitizenReportRequest request, String sender) {
    return new RegisterCitizenReportCommand(
        new CitizenReportId(request.clientReportId()),
        request.category(),
        request.text(),
        request.reportedAt().toInstant(ZoneOffset.UTC),
        Optional.ofNullable(request.location())
            .map(location -> new Geolocation(location.latitude(), location.longitude())),
        people(request.people()),
        request.accessConditions() == null ? Set.of() : Set.copyOf(request.accessConditions()),
        Optional.ofNullable(request.relatedReportId()).map(CitizenReportId::new),
        sender);
  }

  private static VulnerablePeople people(VulnerablePeopleResource people) {
    return people == null
        ? VulnerablePeople.NONE
        : new VulnerablePeople(people.children(), people.elderly(), people.injured());
  }

  public static CitizenReportStatusResource toResource(CitizenReportProgress progress) {
    return new CitizenReportStatusResource(
        progress.stage(), progress.score().orElse(null), progress.etaMinutes().orElse(null));
  }
}
