package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataDistributionPlanRepository
    extends JpaRepository<DistributionPlanJpaEntity, UUID> {

  List<DistributionPlanJpaEntity> findByZoneIdAndOrganizationIdOrderByCreatedAtDesc(
      UUID zoneId, UUID organizationId);

  List<DistributionPlanJpaEntity> findByEmergencyIdAndOrganizationIdOrderByCreatedAtDesc(
      UUID emergencyId, UUID organizationId);

  boolean existsByEmergencyIdAndOrganizationIdAndStatus(
      UUID emergencyId, UUID organizationId, DistributionPlanStatus status);
}
