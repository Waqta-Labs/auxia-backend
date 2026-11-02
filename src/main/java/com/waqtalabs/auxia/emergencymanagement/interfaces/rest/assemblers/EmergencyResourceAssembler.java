package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.AdministrativeLocationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.EmergencyResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterEmergencyRequest;

public final class EmergencyResourceAssembler {

  private EmergencyResourceAssembler() {}

  public static RegisterEmergencyCommand toCommand(RegisterEmergencyRequest request) {
    AdministrativeLocationResource location = request.location();
    return new RegisterEmergencyCommand(
        request.name(),
        request.type(),
        request.description(),
        new AdministrativeLocation(location.department(), location.province(), location.district()),
        request.startDate());
  }

  public static EmergencyResource toResource(Emergency emergency) {
    AdministrativeLocation location = emergency.location();
    return new EmergencyResource(
        emergency.id().value(),
        emergency.name(),
        emergency.type(),
        emergency.description().orElse(null),
        new AdministrativeLocationResource(
            location.department(), location.province(), location.district()),
        emergency.status(),
        emergency.startDate());
  }
}
