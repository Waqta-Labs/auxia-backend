package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneDecisionRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Test double of the history, in chronological order. */
public class InMemoryZoneDecisionRepository implements ZoneDecisionRepository {

  private final List<ZoneDecision> decisions = new ArrayList<>();

  @Override
  public ZoneDecision save(ZoneDecision decision) {
    decisions.add(decision);
    return decision;
  }

  @Override
  public List<ZoneDecision> findByZoneId(ZoneId zoneId) {
    return decisions.stream()
        .filter(decision -> decision.zoneId().equals(zoneId))
        .sorted(Comparator.comparing(ZoneDecision::decidedAt))
        .toList();
  }
}
