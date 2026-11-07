package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.DecideBrigadeRegistrationCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.SubmitBrigadeRegistrationCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.SubmitBrigadeRegistrationCommandHandler.Submission;
import com.waqtalabs.auxia.identityaccess.application.commands.DecideBrigadeRegistrationCommand;
import com.waqtalabs.auxia.identityaccess.application.commands.SubmitBrigadeRegistrationCommand;
import com.waqtalabs.auxia.identityaccess.application.queryservices.BrigadeRegistrationQueryService;
import com.waqtalabs.auxia.identityaccess.application.queryservices.BrigadeRegistrationQueryService.IdCardFile;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.BrigadeRegistrationRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.BrigadeRegistrationResource;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.BrigadeRegistrationResource.Rejection;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.BrigadeRegistrationResource.Status;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Registration of brigadists from the mobile app (A_02): the phone sends the request and asks
 * for its status without signing in; an ORGANIZATION_ADMIN approves or rejects it.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Brigade registrations", description = "Brigadists that register from the app (A_02)")
public class BrigadeRegistrationController {

  private final SubmitBrigadeRegistrationCommandHandler submit;
  private final DecideBrigadeRegistrationCommandHandler decide;
  private final BrigadeRegistrationQueryService registrations;

  public BrigadeRegistrationController(
      SubmitBrigadeRegistrationCommandHandler submit,
      DecideBrigadeRegistrationCommandHandler decide,
      BrigadeRegistrationQueryService registrations) {
    this.submit = submit;
    this.decide = decide;
    this.registrations = registrations;
  }

  @PutMapping(
      path = "/public/brigade-registrations/{registrationId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @SecurityRequirements
  @Operation(
      summary = "Send the request of a brigadist",
      description =
          "Part registration (JSON) and optional part idCard (JPEG, PNG, WEBP or HEIC, up to 10"
              + " MB). The phone generates registrationId and deviceSecret; sending them again"
              + " returns the stored request.")
  @ApiResponse(responseCode = "201", description = "Request received, PENDING")
  @ApiResponse(responseCode = "200", description = "The request was already received")
  @ApiResponse(responseCode = "404", description = "No brigade has the code")
  @ApiResponse(responseCode = "409", description = "The id was sent with another secret")
  @ApiResponse(responseCode = "422", description = "The photo of the card is not admitted")
  public ResponseEntity<Status> submit(
      @PathVariable UUID registrationId,
      @Valid @RequestPart("registration") BrigadeRegistrationRequest request,
      @RequestPart(value = "idCard", required = false) MultipartFile idCard)
      throws IOException {
    var command =
        new SubmitBrigadeRegistrationCommand(
            new BrigadeRegistrationId(registrationId),
            request.fullName(),
            request.phone(),
            request.organization(),
            request.brigadeCode(),
            request.deviceSecret(),
            idCard == null ? null : idCard.getContentType(),
            idCard == null ? null : idCard.getBytes());
    Submission submission;
    try {
      submission = submit.handle(command);
    } catch (DataIntegrityViolationException sameRequestStoredMeanwhile) {
      submission = submit.handle(command);
    }
    return ResponseEntity.status(submission.created() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(Status.from(submission.registration()));
  }

  @GetMapping("/public/brigade-registrations/{registrationId}")
  @SecurityRequirements
  @Operation(
      summary = "Status of the request of a brigadist",
      description = "Only for the phone that sent it, with its secret in X-Device-Secret.")
  @ApiResponse(responseCode = "200", description = "PENDING, APPROVED or REJECTED with its reason")
  @ApiResponse(responseCode = "404", description = "Unknown request or wrong secret")
  public Status status(
      @PathVariable UUID registrationId,
      @RequestHeader(value = "X-Device-Secret", required = false) String deviceSecret) {
    return Status.from(
        registrations.ofDevice(new BrigadeRegistrationId(registrationId), deviceSecret));
  }

  @GetMapping("/brigade-registrations")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "Requests of brigadists of my organization", description = "Oldest first.")
  public List<BrigadeRegistrationResource> list(
      @RequestParam(defaultValue = "PENDING") RegistrationStatus status,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return registrations.ofOrganization(organizationOf(user), status).stream()
        .map(BrigadeRegistrationResource::from)
        .toList();
  }

  @GetMapping("/brigade-registrations/{registrationId}/id-card")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "Photo of the institutional card of a request")
  @ApiResponse(responseCode = "404", description = "Not a request of the organization, or no photo")
  public ResponseEntity<byte[]> idCard(
      @PathVariable UUID registrationId, @AuthenticationPrincipal AuthenticatedUser user) {
    IdCardFile card =
        registrations.idCard(new BrigadeRegistrationId(registrationId), organizationOf(user));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(card.contentType()))
        .body(card.content());
  }

  @PostMapping("/brigade-registrations/{registrationId}/approval")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Approve the request of a brigadist",
      description =
          "Creates their FIELD_BRIGADE account and adds them to the brigade of the code, so the"
              + " app signs them in with POST /api/v1/auth/device-sign-in.")
  @ApiResponse(responseCode = "404", description = "Not a request of the organization")
  @ApiResponse(responseCode = "409", description = "Already approved or rejected")
  public BrigadeRegistrationResource approve(
      @PathVariable UUID registrationId, @AuthenticationPrincipal AuthenticatedUser user) {
    return decide(registrationId, null, user);
  }

  @PostMapping("/brigade-registrations/{registrationId}/rejection")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "Reject the request of a brigadist, with a reason they see")
  @ApiResponse(responseCode = "404", description = "Not a request of the organization")
  @ApiResponse(responseCode = "409", description = "Already approved or rejected")
  public BrigadeRegistrationResource reject(
      @PathVariable UUID registrationId,
      @Valid @RequestBody Rejection rejection,
      @AuthenticationPrincipal AuthenticatedUser user) {
    return decide(registrationId, rejection.reason(), user);
  }

  private BrigadeRegistrationResource decide(
      UUID registrationId, String reason, AuthenticatedUser user) {
    return BrigadeRegistrationResource.from(
        decide.handle(
            new DecideBrigadeRegistrationCommand(
                new BrigadeRegistrationId(registrationId),
                organizationOf(user),
                new UserId(user.userId()),
                reason)));
  }

  private static OrganizationId organizationOf(AuthenticatedUser user) {
    return new OrganizationId(user.organizationId());
  }
}
