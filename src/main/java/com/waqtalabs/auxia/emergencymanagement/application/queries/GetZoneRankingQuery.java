package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;

/**
 * Zones of an emergency from the most to the least urgent, with optional filters (US-06). A
 * {@code null} filter is not applied.
 *
 * @param minScore lowest score to include; zones without a score are left out
 * @param maxScore highest score to include; zones without a score are left out
 * @param need only the zones whose processed field reports mention this need
 */
public record GetZoneRankingQuery(
    EmergencyId emergencyId, Double minScore, Double maxScore, Need need) {

  /** Every zone of the emergency, without filters. */
  public static GetZoneRankingQuery all(EmergencyId emergencyId) {
    return new GetZoneRankingQuery(emergencyId, null, null, null);
  }

  public boolean filtersByScore() {
    return minScore != null || maxScore != null;
  }
}
