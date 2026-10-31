package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataEmergencyPublicViewRepository
    extends JpaRepository<EmergencyPublicViewJpaEntity, UUID> {

  List<EmergencyPublicViewJpaEntity> findAllByOrderByStartDateDescNameAsc();
}
