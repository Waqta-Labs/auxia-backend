package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * One zone of the ranking.
 *
 * @param needs every need mentioned in the processed field reports of the zone, in the order of
 *     {@link Need}
 */
public record ZoneRankingEntry(Zone zone, Set<Need> needs) {

  public ZoneRankingEntry {
    needs = needs.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(needs));
  }
}
