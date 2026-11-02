package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaFieldReportRepository implements FieldReportRepository {

  private final SpringDataFieldReportRepository repository;

  JpaFieldReportRepository(SpringDataFieldReportRepository repository) {
    this.repository = repository;
  }

  @Override
  public FieldReport save(FieldReport fieldReport) {
    FieldReportJpaEntity entity =
        repository.findById(fieldReport.id().value()).orElseGet(FieldReportJpaEntity::new);
    FieldReportJpaMapper.copy(fieldReport, entity);
    return FieldReportJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<FieldReport> findById(FieldReportId id) {
    return repository.findById(id.value()).map(FieldReportJpaMapper::toDomain);
  }

  @Override
  public List<FieldReport> findByZoneId(ZoneId zoneId) {
    return toDomain(repository.findByZoneIdOrderByReportedAtDesc(zoneId.value()));
  }

  @Override
  public List<FieldReport> findByEmergencyId(EmergencyId emergencyId) {
    return toDomain(repository.findByEmergencyIdOrderByReportedAtDesc(emergencyId.value()));
  }

  @Override
  public List<FieldReport> findPendingZoneAssignmentByEmergencyId(EmergencyId emergencyId) {
    return toDomain(
        repository.findByEmergencyIdAndZoneIdIsNullOrderByReportedAtDesc(emergencyId.value()));
  }

  private static List<FieldReport> toDomain(List<FieldReportJpaEntity> entities) {
    return entities.stream().map(FieldReportJpaMapper::toDomain).toList();
  }
}
