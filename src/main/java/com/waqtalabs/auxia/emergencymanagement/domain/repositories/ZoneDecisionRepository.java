package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;

public interface ZoneDecisionRepository {

  ZoneDecision save(ZoneDecision decision);

  /** History of the zone, from the oldest decision to the most recent. */
  List<ZoneDecision> findByZoneId(ZoneId zoneId);
}
