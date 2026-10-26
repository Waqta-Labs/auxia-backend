package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaZoneRepository implements ZoneRepository {

  private final SpringDataZoneRepository repository;

  JpaZoneRepository(SpringDataZoneRepository repository) {
    this.repository = repository;
  }

  @Override
  public Zone save(Zone zone) {
    ZoneJpaEntity entity = repository.findById(zone.id().value()).orElseGet(ZoneJpaEntity::new);
    ZoneJpaMapper.copy(zone, entity);
    return ZoneJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Zone> findById(ZoneId id) {
    return repository.findById(id.value()).map(ZoneJpaMapper::toDomain);
  }

  /** Until the urgency score exists, zones keep the order in which they were registered. */
  @Override
  public List<Zone> findByEmergencyId(EmergencyId emergencyId) {
    return repository.findByEmergencyIdOrderByCreatedAtAsc(emergencyId.value()).stream()
        .map(ZoneJpaMapper::toDomain)
        .toList();
  }
}
