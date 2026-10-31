package com.waqtalabs.auxia.citizentransparency.interfaces.rest.assemblers;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicDeliveryVerification;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicEmergencyResource;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicLocationResource;
import com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources.PublicZoneStatusResource;

public final class PublicStatusResourceAssembler {

  /** Public verification of Traceability; the code is the identifier of the delivery. */
  private static final String VERIFY = "/api/v1/deliveries/%s/verify";

  private PublicStatusResourceAssembler() {}

  public static PublicEmergencyResource toResource(EmergencyPublicView view) {
    return new PublicEmergencyResource(
        view.emergencyId().value(),
        view.name(),
        view.type(),
        new PublicLocationResource(
            view.location().department(), view.location().province(), view.location().district()),
        view.startDate());
  }

  public static PublicZoneStatusResource toResource(ZonePublicStatus status) {
    PublicDeliveryVerification verification =
        status.deliveriesCompleted() == 0
            ? null
            : status.lastDeliveryVerified()
                ? PublicDeliveryVerification.VERIFIED
                : PublicDeliveryVerification.PENDING_VERIFICATION;
    return new PublicZoneStatusResource(
        status.zoneId().value(),
        status.emergencyId().value(),
        status.zoneName(),
        status.stage(),
        status.deliveriesCompleted(),
        status.lastDeliveryAt().orElse(null),
        verification,
        status.verifiedDelivery().map(delivery -> VERIFY.formatted(delivery.value())).orElse(null));
  }
}
