package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataZoneDecisionRepository extends JpaRepository<ZoneDecisionJpaEntity, UUID> {

  List<ZoneDecisionJpaEntity> findByZoneIdOrderByDecidedAtAscIdAsc(UUID zoneId);
}
