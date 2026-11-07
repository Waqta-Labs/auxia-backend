package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaCitizenReportRepository implements CitizenReportRepository {

  private final SpringDataCitizenReportRepository repository;

  JpaCitizenReportRepository(SpringDataCitizenReportRepository repository) {
    this.repository = repository;
  }

  @Override
  public CitizenReport save(CitizenReport citizenReport) {
    return CitizenReportJpaMapper.toDomain(
        repository.save(CitizenReportJpaMapper.toEntity(citizenReport)));
  }

  @Override
  public Optional<CitizenReport> findById(CitizenReportId id) {
    return repository.findById(id.value()).map(CitizenReportJpaMapper::toDomain);
  }
}
