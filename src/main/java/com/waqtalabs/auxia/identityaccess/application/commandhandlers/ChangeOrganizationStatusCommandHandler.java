package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.ChangeOrganizationStatusCommand;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Suspends or reactivates an organization, and with it the access of all its users. */
@Service
public class ChangeOrganizationStatusCommandHandler {

  private final OrganizationRepository organizationRepository;

  public ChangeOrganizationStatusCommandHandler(OrganizationRepository organizationRepository) {
    this.organizationRepository = organizationRepository;
  }

  @Transactional
  public Organization handle(ChangeOrganizationStatusCommand command) {
    Organization organization =
        organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
    if (command.status() == OrganizationStatus.SUSPENDED) {
      organization.suspend();
    } else {
      organization.reactivate();
    }
    return organizationRepository.save(organization);
  }
}
