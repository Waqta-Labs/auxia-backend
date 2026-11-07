package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCitizenReportRepository extends JpaRepository<CitizenReportJpaEntity, UUID> {

  Optional<CitizenReportJpaEntity> findByFieldReportId(UUID fieldReportId);
}
