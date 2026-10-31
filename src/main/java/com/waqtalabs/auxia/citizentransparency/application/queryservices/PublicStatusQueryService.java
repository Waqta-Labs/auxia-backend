package com.waqtalabs.auxia.citizentransparency.application.queryservices;

import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicEmergenciesQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicZoneByIdQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.GetPublicZonesByEmergencyIdQuery;
import com.waqtalabs.auxia.citizentransparency.application.queries.SearchPublicZonesQuery;
import com.waqtalabs.auxia.citizentransparency.domain.exceptions.PublicEmergencyNotFoundException;
import com.waqtalabs.auxia.citizentransparency.domain.exceptions.PublicZoneNotFoundException;
import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.EmergencyPublicViewRepository;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.ZonePublicStatusRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The four public queries; none needs a signed-in user. */
@Service
@Transactional(readOnly = true)
public class PublicStatusQueryService {

  private final EmergencyPublicViewRepository emergencyViews;
  private final ZonePublicStatusRepository zoneStatuses;

  public PublicStatusQueryService(
      EmergencyPublicViewRepository emergencyViews, ZonePublicStatusRepository zoneStatuses) {
    this.emergencyViews = emergencyViews;
    this.zoneStatuses = zoneStatuses;
  }

  public List<EmergencyPublicView> handle(GetPublicEmergenciesQuery query) {
    return emergencyViews.findAll();
  }

  public List<ZonePublicStatus> handle(GetPublicZonesByEmergencyIdQuery query) {
    if (emergencyViews.findById(query.emergencyId()).isEmpty()) {
      throw new PublicEmergencyNotFoundException(query.emergencyId());
    }
    return zoneStatuses.findByEmergencyId(query.emergencyId());
  }

  /**
   * @throws PublicZoneNotFoundException if the zone is not registered yet (US-24, scenario 2)
   */
  public ZonePublicStatus handle(GetPublicZoneByIdQuery query) {
    return zoneStatuses
        .findByZoneId(query.zoneId())
        .orElseThrow(() -> new PublicZoneNotFoundException(query.zoneId()));
  }

  public List<ZonePublicStatus> handle(SearchPublicZonesQuery query) {
    String name = query.name() == null || query.name().isBlank() ? null : query.name().strip();
    return zoneStatuses.searchByDistrictAndName(query.district().strip(), name);
  }
}
