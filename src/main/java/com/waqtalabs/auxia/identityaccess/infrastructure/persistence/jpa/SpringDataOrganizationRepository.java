package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataOrganizationRepository extends JpaRepository<OrganizationJpaEntity, UUID> {

  boolean existsByTaxId(String taxId);
}
