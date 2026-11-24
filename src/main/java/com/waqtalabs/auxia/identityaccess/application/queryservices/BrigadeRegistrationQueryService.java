package com.waqtalabs.auxia.identityaccess.application.queryservices;

import com.waqtalabs.auxia.identityaccess.application.ports.IdCardStorage;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.BrigadeRegistrationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.repositories.BrigadeRegistrationRepository;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The requests of brigadists: for the phone that sent one, with its secret, and for the
 * administrators of the organization that reviews them.
 */
@Service
@Transactional(readOnly = true)
public class BrigadeRegistrationQueryService {

  private final BrigadeRegistrationRepository registrationRepository;
  private final IdCardStorage idCardStorage;
  private final PasswordHashingService hashing;

  public BrigadeRegistrationQueryService(
      BrigadeRegistrationRepository registrationRepository,
      IdCardStorage idCardStorage,
      PasswordHashingService hashing) {
    this.registrationRepository = registrationRepository;
    this.idCardStorage = idCardStorage;
    this.hashing = hashing;
  }

  /** The photo of a card and its media type. */
  public record IdCardFile(String contentType, byte[] content) {}

  /**
   * The request as the phone that sent it sees it.
   *
   * @throws BrigadeRegistrationNotFoundException if it does not exist or the secret is not its
   *     own, so nobody else learns whether it exists
   */
  public BrigadeRegistration ofDevice(BrigadeRegistrationId id, String deviceSecret) {
    return registrationRepository
        .findById(id)
        .filter(
            found -> deviceSecret != null && hashing.matches(deviceSecret, found.deviceSecret()))
        .orElseThrow(() -> new BrigadeRegistrationNotFoundException(id));
  }

  /** Requests of the organization in a status, from the oldest. */
  public List<BrigadeRegistration> ofOrganization(
      OrganizationId organizationId, RegistrationStatus status) {
    return registrationRepository.findByOrganizationIdAndStatus(organizationId, status);
  }

  /**
   * @throws BrigadeRegistrationNotFoundException if it is not a request of the organization or it
   *     has no photo of the card
   */
  public IdCardFile idCard(BrigadeRegistrationId id, OrganizationId organizationId) {
    return registrationRepository
        .findById(id)
        .filter(found -> found.belongsTo(organizationId))
        .flatMap(BrigadeRegistration::idCard)
        .flatMap(
            card ->
                idCardStorage
                    .read(card.storageUrl())
                    .map(content -> new IdCardFile(card.contentType(), content)))
        .orElseThrow(() -> new BrigadeRegistrationNotFoundException(id));
  }
}
