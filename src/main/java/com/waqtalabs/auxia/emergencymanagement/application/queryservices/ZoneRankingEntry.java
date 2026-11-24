package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * One zone of the ranking.
 *
 * @param needs every need mentioned in the processed field reports of the zone, in the order of
 *     {@link Need}
 * @param accessConditions how many SOS of the zone marked each access condition, in the order of
 *     {@link AccessCondition}; conditions that no SOS marked are left out
 */
public record ZoneRankingEntry(
    Zone zone, Set<Need> needs, Map<AccessCondition, Integer> accessConditions) {

  public ZoneRankingEntry {
    needs = needs.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(needs));
    accessConditions =
        accessConditions.isEmpty()
            ? Map.of()
            : Collections.unmodifiableMap(new EnumMap<>(accessConditions));
  }

  /** A zone that no SOS reached. */
  public ZoneRankingEntry(Zone zone, Set<Need> needs) {
    this(zone, needs, Map.of());
  }
}
