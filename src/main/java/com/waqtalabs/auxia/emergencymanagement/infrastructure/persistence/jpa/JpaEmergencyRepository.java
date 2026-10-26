package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaEmergencyRepository implements EmergencyRepository {

  private final SpringDataEmergencyRepository repository;

  JpaEmergencyRepository(SpringDataEmergencyRepository repository) {
    this.repository = repository;
  }

  @Override
  public Emergency save(Emergency emergency) {
    EmergencyJpaEntity entity =
        repository.findById(emergency.id().value()).orElseGet(EmergencyJpaEntity::new);
    EmergencyJpaMapper.copy(emergency, entity);
    return EmergencyJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Emergency> findById(EmergencyId id) {
    return repository.findById(id.value()).map(EmergencyJpaMapper::toDomain);
  }

  @Override
  public List<Emergency> findActive() {
    return repository
        .findByStatusOrderByStartDateDescCreatedAtDesc(EmergencyStatus.ACTIVE)
        .stream()
        .map(EmergencyJpaMapper::toDomain)
        .toList();
  }
}
