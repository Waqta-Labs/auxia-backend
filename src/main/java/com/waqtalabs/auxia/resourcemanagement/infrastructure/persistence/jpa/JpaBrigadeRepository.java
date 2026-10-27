package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Members and assignments are loaded lazily, so every method runs in a transaction while the
 * mapper reads them.
 */
@Repository
class JpaBrigadeRepository implements BrigadeRepository {

  private final SpringDataBrigadeRepository repository;

  JpaBrigadeRepository(SpringDataBrigadeRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public Brigade save(Brigade brigade) {
    BrigadeJpaEntity entity =
        repository.findById(brigade.id().value()).orElseGet(BrigadeJpaEntity::new);
    BrigadeJpaMapper.copy(brigade, entity);
    return BrigadeJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Brigade> findById(BrigadeId id) {
    return repository.findById(id.value()).map(BrigadeJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Brigade> findByOrganizationId(OrganizationId organizationId) {
    return repository.findByOrganizationIdOrderByNameAsc(organizationId.value()).stream()
        .map(BrigadeJpaMapper::toDomain)
        .toList();
  }
}
