package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataFieldReportRepository extends JpaRepository<FieldReportJpaEntity, UUID> {

  List<FieldReportJpaEntity> findByZoneIdOrderByReportedAtDesc(UUID zoneId);

  List<FieldReportJpaEntity> findByEmergencyIdOrderByReportedAtDesc(UUID emergencyId);

  List<FieldReportJpaEntity> findByEmergencyIdAndZoneIdIsNullOrderByReportedAtDesc(
      UUID emergencyId);
}
