package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.BrigadeTrackingPort;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetCitizenReportProgressQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportSosQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportProgress.Stage;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.CitizenReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportWithoutSosException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.TravelEstimate;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Progress of the SOS of a citizen, from the zone its field report belongs to: received while it
 * has no zone or its zone has no score, prioritized once the zone has a score, and a brigade on
 * the way while a brigade is delivering an approved plan of the zone.
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
      return new CitizenReportProgress(Stage.RECEIVED, Optional.empty(), Optional.empty());
    }
    Optional<Double> score = zone.get().urgencyScore().map(UrgencyScore::value);
    Optional<Geolocation> brigadeBase = baseOfBrigadeOnTheWay(zone.get());
    if (brigadeBase.isPresent()) {
      Optional<Integer> eta =
          zone.get()
              .geolocation()
              .map(place -> TravelEstimate.between(brigadeBase.get(), place).minutes());
      return new CitizenReportProgress(Stage.BRIGADE_ON_THE_WAY, score, eta);
    }
    if (score.isPresent() || zone.get().isPrioritizedManually()) {
      return new CitizenReportProgress(Stage.PRIORITIZED, score, Optional.empty());
    }
    return new CitizenReportProgress(Stage.RECEIVED, Optional.empty(), Optional.empty());
  }

  /**
   * The SOS behind a field report, with what the citizen marked besides the text.
   *
   * @throws FieldReportNotFoundException if the field report does not exist
   * @throws FieldReportWithoutSosException if the field report does not come from an SOS
   */
  public CitizenReport handle(GetFieldReportSosQuery query) {
    if (fieldReportRepository.findById(query.fieldReportId()).isEmpty()) {
      throw new FieldReportNotFoundException(query.fieldReportId());
    }
    return citizenReportRepository
        .findByFieldReportId(query.fieldReportId())
        .orElseThrow(() -> new FieldReportWithoutSosException(query.fieldReportId()));
  }

  /**
   * Base of the brigade delivering the most recent approved plan of the zone. Once the delivery
   * is confirmed the brigade is no longer on the way, and the SOS shows its priority again.
   */
  private Optional<Geolocation> baseOfBrigadeOnTheWay(Zone zone) {
    return distributionPlanRepository.findApprovedByZoneId(zone.id()).stream()
        .map(DistributionPlan::id)
        .map(brigadeTracking::baseOfBrigadeDelivering)
        .flatMap(Optional::stream)
        .findFirst();
  }
}
