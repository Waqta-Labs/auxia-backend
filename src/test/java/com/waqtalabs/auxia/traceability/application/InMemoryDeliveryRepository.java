package com.waqtalabs.auxia.traceability.application;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository. */
public class InMemoryDeliveryRepository implements DeliveryRepository {

  private final Map<DeliveryId, Delivery> deliveries = new LinkedHashMap<>();

  @Override
  public Delivery save(Delivery delivery) {
    deliveries.put(delivery.id(), delivery);
    return delivery;
  }

  @Override
  public Optional<Delivery> findById(DeliveryId id) {
    return Optional.ofNullable(deliveries.get(id));
  }

  @Override
  public Optional<Delivery> findByDistributionPlanId(DistributionPlanId distributionPlanId) {
    return deliveries.values().stream()
        .filter(delivery -> delivery.distributionPlanId().equals(distributionPlanId))
        .findFirst();
  }

  @Override
  public List<Delivery> findByZoneId(ZoneId zoneId, OrganizationId organizationId) {
    return deliveries.values().stream()
        .filter(delivery -> delivery.zoneId().equals(zoneId))
        .filter(delivery -> delivery.belongsTo(organizationId))
        .toList()
        .reversed();
  }

  @Override
  public List<Delivery> findCompleted(
      OrganizationId organizationId, ZoneId zoneId, LocalDateTime from, LocalDateTime to) {
    return deliveries.values().stream()
        .filter(delivery -> delivery.belongsTo(organizationId))
        .filter(delivery -> zoneId == null || delivery.zoneId().equals(zoneId))
        .filter(delivery -> delivery.registration().isPresent())
        .filter(
            delivery -> {
              LocalDateTime at = delivery.registration().orElseThrow().deliveredAt();
              return (from == null || !at.isBefore(from)) && (to == null || at.isBefore(to));
            })
        .sorted(
            Comparator.comparing(delivery -> delivery.registration().orElseThrow().deliveredAt()))
        .toList();
  }

  @Override
  public List<Delivery> findPendingAnchoring() {
    return deliveries.values().stream().filter(Delivery::awaitsAnchoring).toList();
  }
}
