package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataShiftHandoverRepository extends JpaRepository<ShiftHandoverJpaEntity, UUID> {}
