package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataBrigadeRegistrationRepository
    extends JpaRepository<BrigadeRegistrationJpaEntity, UUID> {

  List<BrigadeRegistrationJpaEntity> findByOrganizationIdAndStatusOrderBySubmittedAtAsc(
      UUID organizationId, RegistrationStatus status);
}
