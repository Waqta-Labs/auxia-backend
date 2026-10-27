package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;

final class EmergencyJpaMapper {

  private EmergencyJpaMapper() {}

  static Emergency toDomain(EmergencyJpaEntity entity) {
    return Emergency.reconstitute(
        new EmergencyId(entity.id),
        entity.name,
        entity.type,
        entity.description,
        new AdministrativeLocation(entity.department, entity.province, entity.district),
        entity.startDate,
        entity.status);
  }

  /** Copies the aggregate into a new or managed entity, keeping its audit columns. */
  static void copy(Emergency emergency, EmergencyJpaEntity entity) {
    entity.id = emergency.id().value();
    entity.name = emergency.name();
    entity.type = emergency.type();
    entity.description = emergency.description().orElse(null);
    entity.department = emergency.location().department();
    entity.province = emergency.location().province();
    entity.district = emergency.location().district();
    entity.status = emergency.status();
    entity.startDate = emergency.startDate();
  }
}
