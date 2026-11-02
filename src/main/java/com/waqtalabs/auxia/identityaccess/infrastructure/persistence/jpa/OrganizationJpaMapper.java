package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;

final class OrganizationJpaMapper {

  private OrganizationJpaMapper() {}

  static Organization toDomain(OrganizationJpaEntity entity) {
    return Organization.reconstitute(
        new OrganizationId(entity.id),
        entity.name,
        entity.type,
        new TaxId(entity.taxId),
        entity.status);
  }

  static void copy(Organization organization, OrganizationJpaEntity entity) {
    entity.id = organization.id().value();
    entity.name = organization.name();
    entity.type = organization.type();
    entity.taxId = organization.taxId().value();
    entity.status = organization.status();
  }
}
