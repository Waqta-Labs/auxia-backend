package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicLocation;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.EmergencyPublicViewRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaEmergencyPublicViewRepository implements EmergencyPublicViewRepository {

  private final SpringDataEmergencyPublicViewRepository repository;

  JpaEmergencyPublicViewRepository(SpringDataEmergencyPublicViewRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public EmergencyPublicView save(EmergencyPublicView view) {
    EmergencyPublicViewJpaEntity entity =
        repository
            .findById(view.emergencyId().value())
            .orElseGet(EmergencyPublicViewJpaEntity::new);
    entity.emergencyId = view.emergencyId().value();
    entity.name = view.name();
    entity.type = view.type();
    entity.department = view.location().department();
    entity.province = view.location().province();
    entity.district = view.location().district();
    entity.startDate = view.startDate();
    return toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<EmergencyPublicView> findById(EmergencyId emergencyId) {
    return repository.findById(emergencyId.value()).map(JpaEmergencyPublicViewRepository::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmergencyPublicView> findAll() {
    return repository.findAllByOrderByStartDateDescNameAsc().stream()
        .map(JpaEmergencyPublicViewRepository::toDomain)
        .toList();
  }

  private static EmergencyPublicView toDomain(EmergencyPublicViewJpaEntity entity) {
    return EmergencyPublicView.project(
        new EmergencyId(entity.emergencyId),
        entity.name,
        entity.type,
        new PublicLocation(entity.department, entity.province, entity.district),
        entity.startDate);
  }
}
