package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ShiftHandover;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ShiftHandoverId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ShiftHandoverRepository;
import java.util.ArrayList;
import org.springframework.stereotype.Repository;

@Repository
class JpaShiftHandoverRepository implements ShiftHandoverRepository {

  private final SpringDataShiftHandoverRepository repository;

  JpaShiftHandoverRepository(SpringDataShiftHandoverRepository repository) {
    this.repository = repository;
  }

  @Override
  public ShiftHandover save(ShiftHandover handover) {
    ShiftHandoverJpaEntity entity = new ShiftHandoverJpaEntity();
    entity.id = handover.id().value();
    entity.organizationId = handover.organizationId().value();
    entity.outgoingAuthorityId = handover.outgoing().value();
    entity.incomingAuthorityId = handover.incoming().value();
    entity.handedOverAt = handover.handedOverAt();
    entity.notes = handover.notes().orElse(null);
    entity.zoneIds = new ArrayList<>(handover.zoneIds().stream().map(ZoneId::value).toList());
    ShiftHandoverJpaEntity saved = repository.save(entity);
    return ShiftHandover.reconstitute(
        new ShiftHandoverId(saved.id),
        new OrganizationId(saved.organizationId),
        new AuthorityId(saved.outgoingAuthorityId),
        new AuthorityId(saved.incomingAuthorityId),
        saved.handedOverAt,
        saved.notes,
        saved.zoneIds.stream().map(ZoneId::new).toList());
  }
}
