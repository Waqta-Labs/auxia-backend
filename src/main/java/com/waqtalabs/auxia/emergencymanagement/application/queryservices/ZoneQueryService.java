package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneRankingQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.Comparator;
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

  /** Highest score first; zones without a score last. The sort is stable for ties. */
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

  public ZoneQueryService(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
  }

  public Zone handle(GetZoneByIdQuery query) {
    return zoneRepository
        .findById(query.zoneId())
        .orElseThrow(() -> new ZoneNotFoundException(query.zoneId()));
  }

  /**
   * Zones of the emergency from the most to the least urgent (US-06). Zones without a score go
   * last, in registration order.
   */
  public List<ZoneRankingEntry> handle(GetZoneRankingQuery query) {
    if (emergencyRepository.findById(query.emergencyId()).isEmpty()) {
      throw new EmergencyNotFoundException(query.emergencyId());
    }
    Map<ZoneId, Set<Need>> needs = needsByZone(query);
    return zoneRepository.findByEmergencyId(query.emergencyId()).stream()
        .map(zone -> new ZoneRankingEntry(zone, needs.getOrDefault(zone.id(), Set.of())))
        .filter(entry -> matches(entry, query))
        .sorted(BY_URGENCY)
        .toList();
  }

  private Map<ZoneId, Set<Need>> needsByZone(GetZoneRankingQuery query) {
    Map<ZoneId, Set<Need>> needs = new HashMap<>();
    for (FieldReport report : fieldReportRepository.findByEmergencyId(query.emergencyId())) {
      if (report.zoneId().isEmpty() || report.structuredData().isEmpty()) {
        continue;
      }
      needs
          .computeIfAbsent(report.zoneId().get(), zoneId -> EnumSet.noneOf(Need.class))
          .addAll(report.structuredData().get().needs());
    }
    return needs;
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
