package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Items and evidence are loaded lazily, so every method runs in a transaction while the mapper
 * reads them.
 */
@Repository
class JpaDeliveryRepository implements DeliveryRepository {

  private final SpringDataDeliveryRepository repository;

  JpaDeliveryRepository(SpringDataDeliveryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public Delivery save(Delivery delivery) {
    DeliveryJpaEntity entity =
        repository.findById(delivery.id().value()).orElseGet(DeliveryJpaEntity::new);
    DeliveryJpaMapper.copy(delivery, entity);
    return DeliveryJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Delivery> findById(DeliveryId id) {
    return repository.findById(id.value()).map(DeliveryJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Delivery> findByDistributionPlanId(DistributionPlanId distributionPlanId) {
    return repository
        .findByDistributionPlanId(distributionPlanId.value())
        .map(DeliveryJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Delivery> findByZoneId(ZoneId zoneId, OrganizationId organizationId) {
    return repository
        .findByZoneIdAndOrganizationIdOrderByCreatedAtDesc(zoneId.value(), organizationId.value())
        .stream()
        .map(DeliveryJpaMapper::toDomain)
        .toList();
  }

  /** The zone and the period are filtered here: an organization has few deliveries. */
  @Override
  @Transactional(readOnly = true)
  public List<Delivery> findCompleted(
      OrganizationId organizationId, ZoneId zoneId, LocalDateTime from, LocalDateTime to) {
    return repository
        .findByOrganizationIdAndStatusInOrderByDeliveredAtAsc(
            organizationId.value(), EnumSet.of(DeliveryStatus.REGISTERED, DeliveryStatus.CERTIFIED))
        .stream()
        .filter(entity -> zoneId == null || entity.zoneId.equals(zoneId.value()))
        .filter(entity -> from == null || !entity.deliveredAt.isBefore(from))
        .filter(entity -> to == null || entity.deliveredAt.isBefore(to))
        .map(DeliveryJpaMapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Delivery> findPendingAnchoring() {
    return repository.findByStatusOrderByReceivedAtAsc(DeliveryStatus.REGISTERED).stream()
        .map(DeliveryJpaMapper::toDomain)
        .toList();
  }
}
