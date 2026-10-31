package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneDecisionRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaZoneDecisionRepository implements ZoneDecisionRepository {

  private final SpringDataZoneDecisionRepository repository;

  JpaZoneDecisionRepository(SpringDataZoneDecisionRepository repository) {
    this.repository = repository;
  }

  @Override
  public ZoneDecision save(ZoneDecision decision) {
    ZoneDecisionJpaEntity entity = new ZoneDecisionJpaEntity();
    entity.id = decision.id().value();
    entity.zoneId = decision.zoneId().value();
    entity.type = decision.type();
    entity.decidedBy = decision.decidedBy().value();
    entity.decidedAt = decision.decidedAt();
    entity.detail = decision.detail().orElse(null);
    entity.subjectId = decision.subjectId().orElse(null);
    entity.handedOverTo = decision.handedOverTo().map(AuthorityId::value).orElse(null);
    return toDomain(repository.save(entity));
  }

  /** Decisions taken at the same moment keep a stable order. */
  @Override
  public List<ZoneDecision> findByZoneId(ZoneId zoneId) {
    return repository.findByZoneIdOrderByDecidedAtAscIdAsc(zoneId.value()).stream()
        .map(JpaZoneDecisionRepository::toDomain)
        .toList();
  }

  private static ZoneDecision toDomain(ZoneDecisionJpaEntity entity) {
    return ZoneDecision.reconstitute(
        new ZoneDecisionId(entity.id),
        new ZoneId(entity.zoneId),
        entity.type,
        new AuthorityId(entity.decidedBy),
        entity.decidedAt,
        entity.detail,
        entity.subjectId,
        entity.handedOverTo == null ? null : new AuthorityId(entity.handedOverTo));
  }
}
