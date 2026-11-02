package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEmergencyRepository extends JpaRepository<EmergencyJpaEntity, UUID> {

  List<EmergencyJpaEntity> findByStatusOrderByStartDateDescCreatedAtDesc(EmergencyStatus status);
}
