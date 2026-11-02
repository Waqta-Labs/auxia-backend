package com.waqtalabs.auxia.traceability.interfaces.rest.assemblers;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.VerificationResult;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.DeliveryItemResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.DeliveryResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.EvidenceResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.GeoPointResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.IntegrityResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.VerificationResource;

public final class DeliveryResourceAssembler {

  private DeliveryResourceAssembler() {}

  public static DeliveryResource toResource(Delivery delivery) {
    var registration = delivery.registration();
    return new DeliveryResource(
        delivery.id().value(),
        delivery.distributionPlanId().value(),
        delivery.zoneId().value(),
        delivery.brigadeId().value(),
        delivery.status(),
        registration.map(found -> found.registeredBy().value()).orElse(null),
        registration.map(Delivery.Registration::deliveredAt).orElse(null),
        registration.map(Delivery.Registration::receivedAt).orElse(null),
        delivery.items().stream()
            .map(item -> new DeliveryItemResource(item.resourceId().value(), item.quantity()))
            .toList(),
        delivery.evidences().stream().map(DeliveryResourceAssembler::toResource).toList(),
        delivery.integrity().map(DeliveryResourceAssembler::toResource).orElse(null));
  }

  public static VerificationResource toResource(VerificationResult result) {
    return new VerificationResource(
        result.deliveryId().value(),
        result.recordHash().value(),
        result.anchoredHash().map(HashValue::value).orElse(null),
        result.transactionHash().orElse(null),
        result.verified());
  }

  private static EvidenceResource toResource(Evidence evidence) {
    return new EvidenceResource(
        evidence.id().value(),
        evidence.type(),
        evidence.contentHash().value(),
        evidence.capturedAt(),
        evidence.location().map(DeliveryResourceAssembler::toResource).orElse(null));
  }

  private static IntegrityResource toResource(IntegrityRecord integrity) {
    return new IntegrityResource(
        integrity.recordHash().value(),
        integrity.status(),
        integrity.transactionHash(),
        integrity.anchoredAt(),
        integrity.attempts());
  }

  private static GeoPointResource toResource(GeoPoint location) {
    return new GeoPointResource(location.latitude(), location.longitude());
  }
}
