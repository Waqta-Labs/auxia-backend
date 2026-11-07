package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.BrigadeTrackingPort;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetCitizenReportProgressQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress.Stage;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.CitizenReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress of the SOS of a citizen, from the zone its field report belongs to: received while it
 * has no zone, prioritized once the zone has a score, and a brigade on the way once a brigade
 * took an approved plan of the zone.
 */
@Service
@Transactional(readOnly = true)
public class CitizenReportQueryService {

  private final CitizenReportRepository citizenReportRepository;
  private final FieldReportRepository fieldReportRepository;
  private final ZoneRepository zoneRepository;
  private final DistributionPlanRepository distributionPlanRepository;
  private final BrigadeTrackingPort brigadeTracking;

  public CitizenReportQueryService(
      CitizenReportRepository citizenReportRepository,
      FieldReportRepository fieldReportRepository,
      ZoneRepository zoneRepository,
      DistributionPlanRepository distributionPlanRepository,
      BrigadeTrackingPort brigadeTracking) {
    this.citizenReportRepository = citizenReportRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.zoneRepository = zoneRepository;
    this.distributionPlanRepository = distributionPlanRepository;
    this.brigadeTracking = brigadeTracking;
  }

  /**
   * @throws CitizenReportNotFoundException if no SOS has that identifier
   */
  public CitizenReportProgress handle(GetCitizenReportProgressQuery query) {
    CitizenReport report =
        citizenReportRepository
            .findById(query.citizenReportId())
            .orElseThrow(() -> new CitizenReportNotFoundException(query.citizenReportId()));
    Optional<Zone> zone =
        report
            .fieldReportId()
            .flatMap(fieldReportRepository::findById)
            .flatMap(FieldReport::zoneId)
            .flatMap(zoneRepository::findById);
    if (zone.isEmpty()) {
      return progress(Stage.RECEIVED);
    }
    if (hasBrigadeOnTheWay(zone.get())) {
      return progress(Stage.BRIGADE_ON_THE_WAY);
    }
    if (zone.get().urgencyScore().isPresent() || zone.get().isPrioritizedManually()) {
      return progress(Stage.PRIORITIZED);
    }
    return progress(Stage.RECEIVED);
  }

  private boolean hasBrigadeOnTheWay(Zone zone) {
    return distributionPlanRepository.findApprovedByZoneId(zone.id()).stream()
        .map(DistributionPlan::id)
        .anyMatch(brigadeTracking::hasBrigade);
  }

  /** No arrival time yet: the brigades do not report their position. */
  private static CitizenReportProgress progress(Stage stage) {
    return new CitizenReportProgress(stage, Optional.empty());
  }
}
