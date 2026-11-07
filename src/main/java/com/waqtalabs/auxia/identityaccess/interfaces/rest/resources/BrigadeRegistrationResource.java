package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/** Request of a brigadist, as the administrator of the organization reviews it. */
public record BrigadeRegistrationResource(
    UUID id,
    String fullName,
    String phone,
    String organization,
    String brigadeCode,
    UUID brigadeId,
    RegistrationStatus status,
    @Schema(description = "Download it from /id-card") boolean hasIdCard,
    LocalDateTime submittedAt,
    String rejectionReason,
    @Schema(description = "FIELD_BRIGADE account, once approved") UUID userId) {

  public static BrigadeRegistrationResource from(BrigadeRegistration registration) {
    return new BrigadeRegistrationResource(
        registration.id().value(),
        registration.fullName(),
        registration.phone(),
        registration.organizationName(),
        registration.brigadeCode(),
        registration.brigadeId(),
        registration.status(),
        registration.idCard().isPresent(),
        registration.submittedAt(),
        registration.rejectionReason().orElse(null),
        registration.userId().map(UserId::value).orElse(null));
  }

  /** What the phone sees of its own request. */
  public record Status(
      @Schema(example = "PENDING") RegistrationStatus status,
      @Schema(description = "Why it was rejected; null otherwise") String reason) {

    public static Status from(BrigadeRegistration registration) {
      return new Status(registration.status(), registration.rejectionReason().orElse(null));
    }
  }

  /** Why the request is rejected; the brigadist sees it. */
  public record Rejection(
      @Schema(example = "El código de brigada no corresponde a tu organismo")
          @NotBlank
          @Size(max = 500)
          String reason) {}

  /** Sign-in of an approved brigadist with the secret of their phone. */
  public record DeviceSignIn(@NotNull UUID registrationId, @NotBlank String deviceSecret) {}
}
