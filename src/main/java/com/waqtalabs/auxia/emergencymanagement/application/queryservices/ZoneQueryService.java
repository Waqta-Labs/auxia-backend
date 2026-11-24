package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZonesInChargeQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ZoneQueryService {

  /** Manually prioritized zones first (US-07), then by score; zones without score last. */
  private static final Comparator<ZoneRankingEntry> BY_URGENCY =
      Comparator.comparing((ZoneRankingEntry entry) -> !entry.zone().isPrioritizedManually())
          .thenComparing(
              (ZoneRankingEntry entry) ->
                  entry.zone().urgencyScore().map(UrgencyScore::value).orElse(null),
              Comparator.nullsLast(Comparator.reverseOrder()));

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final CitizenReportRepository citizenReportRepository;

  public ZoneQueryService(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      CitizenReportRepository citizenReportRepository) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.citizenReportRepository = citizenReportRepository;
  }

  public Zone handle(GetZoneByIdQuery query) {
    return zoneRepository
        .findById(query.zoneId())
        .orElseThrow(() -> new ZoneNotFoundException(query.zoneId()));
  }

  /**
   * Zones of active emergencies the authority follows, with their last status (US-23, scenario
   * 2): the ones it registered and the ones it received at a shift change. In registration order.
   */
  public List<Zone> handle(GetZonesInChargeQuery query) {
    return zoneRepository.findByResponsibleAuthority(query.authorityId()).stream()
        .filter(
            zone ->
                emergencyRepository
                    .findById(zone.emergencyId())
                    .map(Emergency::isActive)
                    .orElse(false))
        .toList();
  }

  /**
   * Zones of the emergency from the most to the least urgent (US-06). Zones without a score go
   * last, in registration order. Each zone carries the needs of its field reports and how many
   * SOS of the zone marked each access condition, such as a blocked road.
   */
  public List<ZoneRankingEntry> handle(GetZoneRankingQuery query) {
    if (emergencyRepository.findById(query.emergencyId()).isEmpty()) {
      throw new EmergencyNotFoundException(query.emergencyId());
    }
    List<FieldReport> reports = fieldReportRepository.findByEmergencyId(query.emergencyId());
    Map<ZoneId, Set<Need>> needs = needsByZone(reports);
    Map<ZoneId, Map<AccessCondition, Integer>> access = accessConditionsByZone(reports);
    return zoneRepository.findByEmergencyId(query.emergencyId()).stream()
        .map(
            zone ->
                new ZoneRankingEntry(
                    zone,
                    needs.getOrDefault(zone.id(), Set.of()),
                    access.getOrDefault(zone.id(), Map.of())))
        .filter(entry -> matches(entry, query))
        .sorted(BY_URGENCY)
        .toList();
  }

  private static Map<ZoneId, Set<Need>> needsByZone(List<FieldReport> reports) {
    Map<ZoneId, Set<Need>> needs = new HashMap<>();
    for (FieldReport report : reports) {
      if (report.zoneId().isEmpty() || report.structuredData().isEmpty()) {
        continue;
      }
      needs
          .computeIfAbsent(report.zoneId().get(), zoneId -> EnumSet.noneOf(Need.class))
          .addAll(report.structuredData().get().needs());
    }
    return needs;
  }

  /** How many SOS of each zone marked each access condition. */
  private Map<ZoneId, Map<AccessCondition, Integer>> accessConditionsByZone(
      List<FieldReport> reports) {
    Map<FieldReportId, ZoneId> zoneOfReport = new HashMap<>();
    for (FieldReport report : reports) {
      report.zoneId().ifPresent(zoneId -> zoneOfReport.put(report.id(), zoneId));
    }
    Map<ZoneId, Map<AccessCondition, Integer>> access = new HashMap<>();
    for (CitizenReport sos : citizenReportRepository.findByFieldReportIds(zoneOfReport.keySet())) {
      ZoneId zoneId = sos.fieldReportId().map(zoneOfReport::get).orElse(null);
      if (zoneId == null || sos.accessConditions().isEmpty()) {
        continue;
      }
      Map<AccessCondition, Integer> counts =
          access.computeIfAbsent(zoneId, id -> new EnumMap<>(AccessCondition.class));
      sos.accessConditions().forEach(condition -> counts.merge(condition, 1, Integer::sum));
    }
    return access;
  }

  private static boolean matches(ZoneRankingEntry entry, GetZoneRankingQuery query) {
    if (query.need() != null && !entry.needs().contains(query.need())) {
      return false;
    }
    if (!query.filtersByScore()) {
      return true;
    }
    return entry
        .zone()
        .urgencyScore()
        .map(UrgencyScore::value)
        .filter(score -> query.minScore() == null || score >= query.minScore())
        .filter(score -> query.maxScore() == null || score <= query.maxScore())
        .isPresent();
  }
}
