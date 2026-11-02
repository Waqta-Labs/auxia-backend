package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.DeliveryItem;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Evidence;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryItemId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.HashValue;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.IntegrityRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class DeliveryJpaMapper {

  private DeliveryJpaMapper() {}

  static Delivery toDomain(DeliveryJpaEntity entity) {
    return Delivery.reconstitute(
        new DeliveryId(entity.id),
        new DistributionPlanId(entity.distributionPlanId),
        new ZoneId(entity.zoneId),
        new BrigadeId(entity.brigadeId),
        new OrganizationId(entity.organizationId),
        entity.status,
        entity.registeredBy == null
            ? null
            : new Delivery.Registration(
                new UserId(entity.registeredBy), entity.deliveredAt, entity.receivedAt),
        entity.items.stream().map(DeliveryJpaMapper::toDomain).toList(),
        entity.evidences.stream().map(DeliveryJpaMapper::toDomain).toList(),
        entity.recordHash == null
            ? null
            : new IntegrityRecord(
                new HashValue(entity.recordHash),
                entity.anchorStatus,
                entity.transactionHash,
                entity.anchoredAt,
                entity.anchorAttempts));
  }

  /**
   * Copies the aggregate into a new or managed entity. Items and evidence never change once
   * registered, so only the missing rows are added.
   */
  static void copy(Delivery delivery, DeliveryJpaEntity entity) {
    entity.id = delivery.id().value();
    entity.distributionPlanId = delivery.distributionPlanId().value();
    entity.zoneId = delivery.zoneId().value();
    entity.brigadeId = delivery.brigadeId().value();
    entity.organizationId = delivery.organizationId().value();
    entity.status = delivery.status();
    delivery
        .registration()
        .ifPresent(
            registration -> {
              entity.registeredBy = registration.registeredBy().value();
              entity.deliveredAt = registration.deliveredAt();
              entity.receivedAt = registration.receivedAt();
            });
    delivery
        .integrity()
        .ifPresent(
            integrity -> {
              entity.recordHash = integrity.recordHash().value();
              entity.anchorStatus = integrity.status();
              entity.transactionHash = integrity.transactionHash();
              entity.anchoredAt = integrity.anchoredAt();
              entity.anchorAttempts = integrity.attempts();
            });

    Set<UUID> items = entity.items.stream().map(row -> row.id).collect(Collectors.toSet());
    for (DeliveryItem item : delivery.items()) {
      if (!items.contains(item.id().value())) {
        var row = new DeliveryItemJpaEntity();
        row.id = item.id().value();
        row.delivery = entity;
        row.resourceId = item.resourceId().value();
        row.quantity = item.quantity();
        entity.items.add(row);
      }
    }
    Set<UUID> evidences = entity.evidences.stream().map(row -> row.id).collect(Collectors.toSet());
    for (Evidence evidence : delivery.evidences()) {
      if (!evidences.contains(evidence.id().value())) {
        var row = new EvidenceJpaEntity();
        row.id = evidence.id().value();
        row.delivery = entity;
        row.type = evidence.type();
        row.storageUrl = evidence.storageUrl();
        row.contentHash = evidence.contentHash().value();
        row.capturedAt = evidence.capturedAt();
        row.latitude = evidence.location().map(GeoPoint::latitude).orElse(null);
        row.longitude = evidence.location().map(GeoPoint::longitude).orElse(null);
        entity.evidences.add(row);
      }
    }
  }

  private static DeliveryItem toDomain(DeliveryItemJpaEntity row) {
    return DeliveryItem.reconstitute(
        new DeliveryItemId(row.id), new ResourceId(row.resourceId), row.quantity);
  }

  private static Evidence toDomain(EvidenceJpaEntity row) {
    return Evidence.reconstitute(
        new EvidenceId(row.id),
        row.type,
        row.storageUrl,
        new HashValue(row.contentHash),
        row.capturedAt,
        row.latitude == null ? null : new GeoPoint(row.latitude, row.longitude));
  }
}
