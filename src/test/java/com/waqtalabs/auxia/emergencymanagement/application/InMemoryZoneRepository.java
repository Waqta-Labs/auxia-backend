package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository, in registration order. */
public class InMemoryZoneRepository implements ZoneRepository {

  private final Map<ZoneId, Zone> zones = new LinkedHashMap<>();

  @Override
  public Zone save(Zone zone) {
    zones.put(zone.id(), zone);
    return zone;
  }

  @Override
  public Optional<Zone> findById(ZoneId id) {
    return Optional.ofNullable(zones.get(id));
  }

  @Override
  public List<Zone> findByEmergencyId(EmergencyId emergencyId) {
    return zones.values().stream().filter(zone -> zone.belongsTo(emergencyId)).toList();
  }
}
