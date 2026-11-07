package com.waqtalabs.auxia.identityaccess.domain.repositories;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import java.util.List;
import java.util.Optional;

public interface BrigadeRegistrationRepository {

  BrigadeRegistration save(BrigadeRegistration registration);

  Optional<BrigadeRegistration> findById(BrigadeRegistrationId id);

  /** Requests of the organization in a status, from the oldest. */
  List<BrigadeRegistration> findByOrganizationIdAndStatus(
      OrganizationId organizationId, RegistrationStatus status);
}
