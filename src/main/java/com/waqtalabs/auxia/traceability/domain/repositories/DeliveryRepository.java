package com.waqtalabs.auxia.traceability.domain.repositories;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository {

  Delivery save(Delivery delivery);

  Optional<Delivery> findById(DeliveryId id);

  Optional<Delivery> findByDistributionPlanId(DistributionPlanId distributionPlanId);

  /** Deliveries of the zone made by the organization, the most recent first. */
  List<Delivery> findByZoneId(ZoneId zoneId, OrganizationId organizationId);

  /**
   * Registered or certified deliveries of the organization, from the oldest. {@code zoneId},
   * {@code from} and {@code to} are optional; {@code to} is excluded.
   */
  List<Delivery> findCompleted(
      OrganizationId organizationId, ZoneId zoneId, LocalDateTime from, LocalDateTime to);

  /** Registered deliveries whose hash is not anchored yet. */
  List<Delivery> findPendingAnchoring();
}
