package com.waqtalabs.auxia.resourcemanagement.domain.repositories;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import java.util.List;
import java.util.Optional;

public interface BrigadeRepository {

  Brigade save(Brigade brigade);

  Optional<Brigade> findById(BrigadeId id);

  /** Brigades of the organization, ordered by name. */
  List<Brigade> findByOrganizationId(OrganizationId organizationId);
}
