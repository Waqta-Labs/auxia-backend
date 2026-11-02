package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataZoneRepository extends JpaRepository<ZoneJpaEntity, UUID> {

  List<ZoneJpaEntity> findByEmergencyIdOrderByCreatedAtAsc(UUID emergencyId);

  List<ZoneJpaEntity> findByResponsibleAuthorityIdOrderByCreatedAtAsc(UUID authorityId);
}
