package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorityDirectory;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetZoneHistoryQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneDecisionRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * History of decisions of a zone in chronological order, each with the authority who took it
 * (US-20). It is the same for every authority, so the next responsible after a shift change sees
 * the decisions of the previous shift.
 */
@Service
@Transactional(readOnly = true)
public class ZoneHistoryQueryService {

  private final ZoneRepository zoneRepository;
  private final ZoneDecisionRepository zoneDecisionRepository;
  private final AuthorityDirectory authorities;

  public ZoneHistoryQueryService(
      ZoneRepository zoneRepository,
      ZoneDecisionRepository zoneDecisionRepository,
      AuthorityDirectory authorities) {
    this.zoneRepository = zoneRepository;
    this.zoneDecisionRepository = zoneDecisionRepository;
    this.authorities = authorities;
  }

  public List<ZoneHistoryEntry> handle(GetZoneHistoryQuery query) {
    if (zoneRepository.findById(query.zoneId()).isEmpty()) {
      throw new ZoneNotFoundException(query.zoneId());
    }
    List<ZoneDecision> decisions = zoneDecisionRepository.findByZoneId(query.zoneId());
    Set<AuthorityId> involved = new HashSet<>();
    for (ZoneDecision decision : decisions) {
      involved.add(decision.decidedBy());
      decision.handedOverTo().ifPresent(involved::add);
    }
    Map<AuthorityId, String> names = authorities.fullNames(involved);
    return decisions.stream()
        .map(
            decision ->
                new ZoneHistoryEntry(
                    decision,
                    names.get(decision.decidedBy()),
                    decision.handedOverTo().map(names::get).orElse(null)))
        .toList();
  }
}
