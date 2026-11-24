package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import com.waqtalabs.auxia.identityaccess.domain.exceptions.RegistrationAlreadyDecidedException;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Request of a brigadist from the mobile app (A_02): name and phone (A_01), the organization
 * they belong to, the code of their brigade and the photo of their institutional card. An
 * ORGANIZATION_ADMIN of the organization of that brigade approves or rejects it once.
 *
 * <p>A_02 asks for no e-mail nor password. The phone generates a secret, kept here as a hash; on
 * approval it becomes the credential of a FIELD_BRIGADE account whose e-mail is internal to the
 * platform and never receives mail.
 */
public final class BrigadeRegistration {

  private final BrigadeRegistrationId id;
  private final String fullName;
  private final String phone;
  private final String organizationName;
  private final String brigadeCode;
  private final UUID brigadeId;
  private final OrganizationId organizationId;
  private final PasswordHash deviceSecret;
  private final IdCard idCard;
  private final LocalDateTime submittedAt;
  private RegistrationStatus status;
  private String rejectionReason;
  private UserId decidedBy;
  private LocalDateTime decidedAt;
  private UserId userId;

  /** Photo of the institutional card in the storage. */
  public record IdCard(String storageUrl, String contentType) {}

  private BrigadeRegistration(
      BrigadeRegistrationId id,
      String fullName,
      String phone,
      String organizationName,
      String brigadeCode,
      UUID brigadeId,
      OrganizationId organizationId,
      PasswordHash deviceSecret,
      IdCard idCard,
      LocalDateTime submittedAt,
      RegistrationStatus status,
      String rejectionReason,
      UserId decidedBy,
      LocalDateTime decidedAt,
      UserId userId) {
    this.id = Objects.requireNonNull(id, "id");
    this.fullName = requireText(fullName, "fullName");
    this.phone = requireText(phone, "phone");
    this.organizationName = requireText(organizationName, "organizationName");
    this.brigadeCode = requireText(brigadeCode, "brigadeCode");
    this.brigadeId = Objects.requireNonNull(brigadeId, "brigadeId");
    this.organizationId = Objects.requireNonNull(organizationId, "organizationId");
    this.deviceSecret = Objects.requireNonNull(deviceSecret, "deviceSecret");
    this.idCard = idCard;
    this.submittedAt = Objects.requireNonNull(submittedAt, "submittedAt");
    this.status = Objects.requireNonNull(status, "status");
    this.rejectionReason = rejectionReason;
    this.decidedBy = decidedBy;
    this.decidedAt = decidedAt;
    this.userId = userId;
  }

  /**
   * Receives the request of a brigadist, pending approval.
   *
   * @param idCard {@code null} when the brigadist did not scan their card
   */
  public static BrigadeRegistration submit(
      BrigadeRegistrationId id,
      String fullName,
      String phone,
      String organizationName,
      String brigadeCode,
      UUID brigadeId,
      OrganizationId organizationId,
      PasswordHash deviceSecret,
      IdCard idCard,
      LocalDateTime submittedAt) {
    return new BrigadeRegistration(
        id,
        fullName,
        phone,
        organizationName,
        brigadeCode,
        brigadeId,
        organizationId,
        deviceSecret,
        idCard,
        submittedAt,
        RegistrationStatus.PENDING,
        null,
        null,
        null,
        null);
  }

  /** Rebuilds a request that is already stored. */
  public static BrigadeRegistration reconstitute(
      BrigadeRegistrationId id,
      String fullName,
      String phone,
      String organizationName,
      String brigadeCode,
      UUID brigadeId,
      OrganizationId organizationId,
      PasswordHash deviceSecret,
      IdCard idCard,
      LocalDateTime submittedAt,
      RegistrationStatus status,
      String rejectionReason,
      UserId decidedBy,
      LocalDateTime decidedAt,
      UserId userId) {
    return new BrigadeRegistration(
        id,
        fullName,
        phone,
        organizationName,
        brigadeCode,
        brigadeId,
        organizationId,
        deviceSecret,
        idCard,
        submittedAt,
        status,
        rejectionReason,
        decidedBy,
        decidedAt,
        userId);
  }

  /**
   * Approves the request with the FIELD_BRIGADE account created for it.
   *
   * @throws RegistrationAlreadyDecidedException if it was already approved or rejected
   */
  public void approve(UserId account, UserId approvedBy, LocalDateTime at) {
    requirePending();
    status = RegistrationStatus.APPROVED;
    userId = Objects.requireNonNull(account, "account");
    decide(approvedBy, at);
  }

  /**
   * Rejects the request with a reason that the brigadist sees.
   *
   * @throws RegistrationAlreadyDecidedException if it was already approved or rejected
   */
  public void reject(String reason, UserId rejectedBy, LocalDateTime at) {
    requirePending();
    status = RegistrationStatus.REJECTED;
    rejectionReason = requireText(reason, "reason");
    decide(rejectedBy, at);
  }

  /**
   * E-mail of the account of the request. It ends in {@code .invalid}, a domain reserved so that
   * no mail is ever delivered (RFC 2606): the brigadist signs in with the secret of the phone.
   */
  public EmailAddress accountEmail() {
    return new EmailAddress("brigadist-" + id.value() + "@device.auxia.invalid");
  }

  /** Name of the account: the first word, and the rest as last name. */
  public PersonName accountName() {
    String[] words = fullName.split("\\s+", 2);
    return new PersonName(words[0], words.length > 1 ? words[1] : words[0]);
  }

  public boolean belongsTo(OrganizationId organization) {
    return organizationId.equals(organization);
  }

  public BrigadeRegistrationId id() {
    return id;
  }

  public String fullName() {
    return fullName;
  }

  public String phone() {
    return phone;
  }

  public String organizationName() {
    return organizationName;
  }

  public String brigadeCode() {
    return brigadeCode;
  }

  public UUID brigadeId() {
    return brigadeId;
  }

  public OrganizationId organizationId() {
    return organizationId;
  }

  public PasswordHash deviceSecret() {
    return deviceSecret;
  }

  public Optional<IdCard> idCard() {
    return Optional.ofNullable(idCard);
  }

  public LocalDateTime submittedAt() {
    return submittedAt;
  }

  public RegistrationStatus status() {
    return status;
  }

  public Optional<String> rejectionReason() {
    return Optional.ofNullable(rejectionReason);
  }

  public Optional<UserId> decidedBy() {
    return Optional.ofNullable(decidedBy);
  }

  public Optional<LocalDateTime> decidedAt() {
    return Optional.ofNullable(decidedAt);
  }

  /** The FIELD_BRIGADE account, once approved. */
  public Optional<UserId> userId() {
    return Optional.ofNullable(userId);
  }

  private void requirePending() {
    if (status != RegistrationStatus.PENDING) {
      throw new RegistrationAlreadyDecidedException(id);
    }
  }

  private void decide(UserId by, LocalDateTime at) {
    decidedBy = Objects.requireNonNull(by, "by");
    decidedAt = Objects.requireNonNull(at, "at");
  }

  private static String requireText(String value, String field) {
    Objects.requireNonNull(value, field);
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return value.strip();
  }
}
