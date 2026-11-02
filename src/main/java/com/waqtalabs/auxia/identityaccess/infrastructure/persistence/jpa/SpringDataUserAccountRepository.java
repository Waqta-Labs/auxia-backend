package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserAccountRepository extends JpaRepository<UserAccountJpaEntity, UUID> {

  Optional<UserAccountJpaEntity> findByEmail(String email);

  boolean existsByEmail(String email);

  List<UserAccountJpaEntity> findByOrganizationIdOrderByEmailAsc(UUID organizationId);
}
