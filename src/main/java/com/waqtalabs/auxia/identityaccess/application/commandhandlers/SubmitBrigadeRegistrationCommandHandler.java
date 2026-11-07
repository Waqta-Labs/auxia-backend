package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.SubmitBrigadeRegistrationCommand;
import com.waqtalabs.auxia.identityaccess.application.ports.BrigadeDirectory;
import com.waqtalabs.auxia.identityaccess.application.ports.BrigadeDirectory.BrigadeReference;
import com.waqtalabs.auxia.identityaccess.application.ports.IdCardStorage;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.InvalidIdCardException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.RegistrationIdInUseException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.UnknownBrigadeCodeException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration.IdCard;
import com.waqtalabs.auxia.identityaccess.domain.repositories.BrigadeRegistrationRepository;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receives the request of a brigadist (A_02) for the brigade of its code, pending approval. The
 * phone sends it again while it is not confirmed: the same request with the same secret is
 * returned as it is.
 */
@Service
public class SubmitBrigadeRegistrationCommandHandler {

  private static final Set<String> CARD_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/heic");
  private static final long MAX_CARD_BYTES = 10L * 1024 * 1024;

  private final BrigadeRegistrationRepository registrationRepository;
  private final BrigadeDirectory brigadeDirectory;
  private final IdCardStorage idCardStorage;
  private final PasswordHashingService hashing;
  private final Clock clock;

  public SubmitBrigadeRegistrationCommandHandler(
      BrigadeRegistrationRepository registrationRepository,
      BrigadeDirectory brigadeDirectory,
      IdCardStorage idCardStorage,
      PasswordHashingService hashing,
      Clock clock) {
    this.registrationRepository = registrationRepository;
    this.brigadeDirectory = brigadeDirectory;
    this.idCardStorage = idCardStorage;
    this.hashing = hashing;
    this.clock = clock;
  }

  /**
   * The request, and whether this call stored it.
   *
   * @param created {@code false} when the phone had already sent it
   */
  public record Submission(BrigadeRegistration registration, boolean created) {}

  /**
   * @throws RegistrationIdInUseException if the id was sent before with another secret
   * @throws UnknownBrigadeCodeException if no brigade has the code
   * @throws InvalidIdCardException if the photo of the card is not an image of up to 10 MB
   */
  @Transactional
  public Submission handle(SubmitBrigadeRegistrationCommand command) {
    Optional<BrigadeRegistration> stored =
        registrationRepository.findById(command.registrationId());
    if (stored.isPresent()) {
      if (!hashing.matches(command.deviceSecret(), stored.get().deviceSecret())) {
        throw new RegistrationIdInUseException(command.registrationId());
      }
      return new Submission(stored.get(), false);
    }
    BrigadeReference brigade =
        brigadeDirectory
            .findByCode(command.brigadeCode())
            .orElseThrow(() -> new UnknownBrigadeCodeException(command.brigadeCode()));
    IdCard idCard = command.idCard() == null ? null : storeIdCard(command);
    BrigadeRegistration registration =
        BrigadeRegistration.submit(
            command.registrationId(),
            command.fullName(),
            command.phone(),
            command.organizationName(),
            command.brigadeCode().strip().toUpperCase(Locale.ROOT),
            brigade.brigadeId(),
            brigade.organizationId(),
            hashing.hash(command.deviceSecret()),
            idCard,
            LocalDateTime.now(clock));
    return new Submission(registrationRepository.save(registration), true);
  }

  private IdCard storeIdCard(SubmitBrigadeRegistrationCommand command) {
    String type =
        command.idCardContentType() == null
            ? ""
            : command.idCardContentType().split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
    if (!CARD_TYPES.contains(type)) {
      throw new InvalidIdCardException("unsupported type " + command.idCardContentType());
    }
    if (command.idCard().length == 0 || command.idCard().length > MAX_CARD_BYTES) {
      throw new InvalidIdCardException("up to 10 MB");
    }
    String url =
        idCardStorage.store(command.registrationId().value().toString(), type, command.idCard());
    return new IdCard(url, type);
  }
}
