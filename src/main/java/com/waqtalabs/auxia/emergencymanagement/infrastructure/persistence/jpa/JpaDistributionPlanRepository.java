package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaDistributionPlanRepository implements DistributionPlanRepository {

  private final SpringDataDistributionPlanRepository repository;

  JpaDistributionPlanRepository(SpringDataDistributionPlanRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public DistributionPlan save(DistributionPlan plan) {
    DistributionPlanJpaEntity entity =
        repository.findById(plan.id().value()).orElseGet(DistributionPlanJpaEntity::new);
    DistributionPlanJpaMapper.copy(plan, entity);
    return DistributionPlanJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<DistributionPlan> findById(DistributionPlanId id) {
    return repository.findById(id.value()).map(DistributionPlanJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<DistributionPlan> findByZoneId(ZoneId zoneId, OrganizationId organizationId) {
    return repository
        .findByZoneIdAndOrganizationIdOrderByCreatedAtDesc(zoneId.value(), organizationId.value())
        .stream()
        .map(DistributionPlanJpaMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DistributionPlan> findByEmergencyId(
      EmergencyId emergencyId, OrganizationId organizationId) {
    return repository
        .findByEmergencyIdAndOrganizationIdOrderByCreatedAtDesc(
            emergencyId.value(), organizationId.value())
        .stream()
        .map(DistributionPlanJpaMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasPendingPlans(EmergencyId emergencyId, OrganizationId organizationId) {
    return repository.existsByEmergencyIdAndOrganizationIdAndStatus(
        emergencyId.value(), organizationId.value(), DistributionPlanStatus.PROPOSED);
  }
}
