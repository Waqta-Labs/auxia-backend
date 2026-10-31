package com.waqtalabs.auxia.citizentransparency.domain.repositories;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Optional;

public interface ZonePublicStatusRepository {

  ZonePublicStatus save(ZonePublicStatus status);

  Optional<ZonePublicStatus> findByZoneId(ZoneId zoneId);

  /** Zones of the emergency, by name. */
  List<ZonePublicStatus> findByEmergencyId(EmergencyId emergencyId);

  /**
   * Zones of the emergencies of a district, ignoring case, by name.
   *
   * @param name part of the name of the zone, ignoring case; {@code null} for every zone
   */
  List<ZonePublicStatus> searchByDistrictAndName(String district, String name);
}
