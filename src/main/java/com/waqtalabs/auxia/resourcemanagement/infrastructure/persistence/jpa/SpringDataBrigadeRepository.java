package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBrigadeRepository extends JpaRepository<BrigadeJpaEntity, UUID> {

  List<BrigadeJpaEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

  Optional<BrigadeJpaEntity> findByAssignmentsDistributionPlanId(UUID distributionPlanId);
}
