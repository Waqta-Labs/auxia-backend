package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.ZonePublicStatusRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaZonePublicStatusRepository implements ZonePublicStatusRepository {

  private final SpringDataZonePublicStatusRepository repository;

  JpaZonePublicStatusRepository(SpringDataZonePublicStatusRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public ZonePublicStatus save(ZonePublicStatus status) {
    ZonePublicStatusJpaEntity entity =
        repository
            .findById(status.zoneId().value())
            .orElseGet(ZonePublicStatusJpaEntity::new);
    entity.zoneId = status.zoneId().value();
    entity.emergencyId = status.emergencyId().value();
    entity.zoneName = status.zoneName();
    entity.stage = status.stage();
    entity.currentDistributionPlanId =
        status.currentDistributionPlanId().map(DistributionPlanId::value).orElse(null);
    entity.currentDeliveryId = status.currentDeliveryId().map(DeliveryId::value).orElse(null);
    entity.deliveriesCompleted = status.deliveriesCompleted();
    entity.lastDeliveryAt = status.lastDeliveryAt().orElse(null);
    entity.lastDeliveryVerified = status.lastDeliveryVerified();
    return toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<ZonePublicStatus> findByZoneId(ZoneId zoneId) {
    return repository.findById(zoneId.value()).map(JpaZonePublicStatusRepository::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ZonePublicStatus> findByEmergencyId(EmergencyId emergencyId) {
    return repository.findByEmergencyIdOrderByZoneNameAsc(emergencyId.value()).stream()
        .map(JpaZonePublicStatusRepository::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ZonePublicStatus> searchByDistrictAndName(String district, String name) {
    List<ZonePublicStatusJpaEntity> found =
        name == null
            ? repository.searchByDistrict(district)
            : repository.searchByDistrictAndName(district, name);
    return found.stream().map(JpaZonePublicStatusRepository::toDomain).toList();
  }

  private static ZonePublicStatus toDomain(ZonePublicStatusJpaEntity entity) {
    return ZonePublicStatus.reconstitute(
        new ZoneId(entity.zoneId),
        new EmergencyId(entity.emergencyId),
        entity.zoneName,
        entity.stage,
        entity.currentDistributionPlanId == null
            ? null
            : new DistributionPlanId(entity.currentDistributionPlanId),
        entity.currentDeliveryId == null ? null : new DeliveryId(entity.currentDeliveryId),
        entity.deliveriesCompleted,
        entity.lastDeliveryAt,
        entity.lastDeliveryVerified);
  }
}
