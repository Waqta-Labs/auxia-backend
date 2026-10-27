package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZonesByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ZoneQueryService {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;

  public ZoneQueryService(EmergencyRepository emergencyRepository, ZoneRepository zoneRepository) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
  }

  public Zone handle(GetZoneByIdQuery query) {
    return zoneRepository
        .findById(query.zoneId())
        .orElseThrow(() -> new ZoneNotFoundException(query.zoneId()));
  }

  /** Zones of the emergency, from the most to the least urgent. */
  public List<Zone> handle(GetZonesByEmergencyIdQuery query) {
    if (emergencyRepository.findById(query.emergencyId()).isEmpty()) {
      throw new EmergencyNotFoundException(query.emergencyId());
    }
    return zoneRepository.findByEmergencyId(query.emergencyId());
  }
}
