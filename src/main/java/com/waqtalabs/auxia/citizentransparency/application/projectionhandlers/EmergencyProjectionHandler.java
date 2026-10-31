package com.waqtalabs.auxia.citizentransparency.application.projectionhandlers;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.ZonePublicStatus;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.EmergencyPublicViewRepository;
import com.waqtalabs.auxia.citizentransparency.domain.repositories.ZonePublicStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Projects what happens in Emergency Management: new emergencies and zones, scores and approved
 * plans. An event that arrives twice changes nothing the second time.
 */
@Service
public class EmergencyProjectionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmergencyProjectionHandler.class);

  private final EmergencyPublicViewRepository emergencyViews;
  private final ZonePublicStatusRepository zoneStatuses;

  public EmergencyProjectionHandler(
      EmergencyPublicViewRepository emergencyViews, ZonePublicStatusRepository zoneStatuses) {
    this.emergencyViews = emergencyViews;
    this.zoneStatuses = zoneStatuses;
  }

  @Transactional
  public void projectEmergency(EmergencyPublicView view) {
    if (emergencyViews.findById(view.emergencyId()).isEmpty()) {
      emergencyViews.save(view);
    }
  }

  @Transactional
  public void projectZone(ZoneId zoneId, EmergencyId emergencyId, String zoneName) {
    if (zoneStatuses.findByZoneId(zoneId).isPresent()) {
      return;
    }
    if (emergencyViews.findById(emergencyId).isEmpty()) {
      LOGGER.warn(
          "Zone {} not projected: emergency {} is not", zoneId.value(), emergencyId.value());
      return;
    }
    zoneStatuses.save(ZonePublicStatus.project(zoneId, emergencyId, zoneName));
  }

  @Transactional
  public void markPrioritized(ZoneId zoneId) {
    zoneStatuses
        .findByZoneId(zoneId)
        .ifPresent(
            status -> {
              status.markPrioritized();
              zoneStatuses.save(status);
            });
  }

  @Transactional
  public void markAidApproved(ZoneId zoneId, DistributionPlanId planId) {
    zoneStatuses
        .findByZoneId(zoneId)
        .ifPresent(
            status -> {
              status.markAidApproved(planId);
              zoneStatuses.save(status);
            });
  }
}
