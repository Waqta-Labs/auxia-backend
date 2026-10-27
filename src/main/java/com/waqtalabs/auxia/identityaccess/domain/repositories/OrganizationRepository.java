package com.waqtalabs.auxia.identityaccess.domain.repositories;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import java.util.Optional;

public interface OrganizationRepository {

  Organization save(Organization organization);

  Optional<Organization> findById(OrganizationId id);

  boolean existsByTaxId(TaxId taxId);
}
