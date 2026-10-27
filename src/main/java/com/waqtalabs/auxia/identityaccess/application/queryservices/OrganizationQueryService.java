package com.waqtalabs.auxia.identityaccess.application.queryservices;

import com.waqtalabs.auxia.identityaccess.application.queries.GetOrganizationByIdQuery;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrganizationQueryService {

  private final OrganizationRepository organizationRepository;

  public OrganizationQueryService(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  public Organization handle(GetOrganizationByIdQuery query) {
    return organizationRepository
        .findById(query.organizationId())
        .orElseThrow(() -> new OrganizationNotFoundException(query.organizationId()));
  }
}
