package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDeliveryRepository extends JpaRepository<DeliveryJpaEntity, UUID> {

  Optional<DeliveryJpaEntity> findByDistributionPlanId(UUID distributionPlanId);

  List<DeliveryJpaEntity> findByZoneIdAndOrganizationIdOrderByCreatedAtDesc(
      UUID zoneId, UUID organizationId);

  List<DeliveryJpaEntity> findByStatusOrderByReceivedAtAsc(DeliveryStatus status);
}
