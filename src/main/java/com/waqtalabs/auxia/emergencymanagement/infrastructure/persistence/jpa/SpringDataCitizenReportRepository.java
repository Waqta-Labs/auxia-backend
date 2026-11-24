package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCitizenReportRepository extends JpaRepository<CitizenReportJpaEntity, UUID> {

  Optional<CitizenReportJpaEntity> findByFieldReportId(UUID fieldReportId);

  List<CitizenReportJpaEntity> findByFieldReportIdIn(Collection<UUID> fieldReportIds);
}
