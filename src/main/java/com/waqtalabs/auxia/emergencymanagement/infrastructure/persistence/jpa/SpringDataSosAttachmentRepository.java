package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSosAttachmentRepository extends JpaRepository<SosAttachmentJpaEntity, UUID> {

  List<SosAttachmentJpaEntity> findByCitizenReportIdOrderByTypeDesc(UUID citizenReportId);
}
