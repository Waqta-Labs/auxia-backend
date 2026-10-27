package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.ZoneReportRenderer;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneReportQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Exports the report of a zone with the explanation of its urgency score (US-05, scenario 2). */
@Service
@Transactional(readOnly = true)
public class ZoneReportQueryService {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final ZoneReportRenderer renderer;
  private final Clock clock;

  public ZoneReportQueryService(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      ZoneReportRenderer renderer,
      Clock clock) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.renderer = renderer;
    this.clock = clock;
  }

  /** The document of the report, in the language of the query. */
  public byte[] handle(GetZoneReportQuery query) {
    Zone zone =
        zoneRepository
            .findById(query.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(query.zoneId()));
    Emergency emergency =
        emergencyRepository
            .findById(zone.emergencyId())
            .orElseThrow(() -> new EmergencyNotFoundException(zone.emergencyId()));
    var report =
        new ZoneReport(
            emergency,
            zone,
            fieldReportRepository.findByZoneId(zone.id()),
            LocalDateTime.now(clock));
    return renderer.render(report, query.locale());
  }
}
