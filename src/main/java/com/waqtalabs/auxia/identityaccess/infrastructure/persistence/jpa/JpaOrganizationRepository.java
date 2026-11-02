package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaOrganizationRepository implements OrganizationRepository {

  private final SpringDataOrganizationRepository repository;

  JpaOrganizationRepository(SpringDataOrganizationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Organization save(Organization organization) {
    OrganizationJpaEntity entity =
        repository.findById(organization.id().value()).orElseGet(OrganizationJpaEntity::new);
    OrganizationJpaMapper.copy(organization, entity);
    return OrganizationJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  public Optional<Organization> findById(OrganizationId id) {
    return repository.findById(id.value()).map(OrganizationJpaMapper::toDomain);
  }

  @Override
  public boolean existsByTaxId(TaxId taxId) {
    return repository.existsByTaxId(taxId.value());
  }
}
