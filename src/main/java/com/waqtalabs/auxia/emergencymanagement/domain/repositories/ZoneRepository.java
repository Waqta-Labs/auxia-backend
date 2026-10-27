package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Optional;

public interface ZoneRepository {

  Zone save(Zone zone);

  Optional<Zone> findById(ZoneId id);

  /** Zones of the emergency, from the most to the least urgent. */
  List<Zone> findByEmergencyId(EmergencyId emergencyId);
}
