package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.ChangeOrganizationStatusCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.RegisterOrganizationCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commands.ChangeOrganizationStatusCommand;
import com.waqtalabs.auxia.identityaccess.application.queries.GetOrganizationByIdQuery;
import com.waqtalabs.auxia.identityaccess.application.queryservices.OrganizationQueryService;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.assemblers.IdentityResourceAssembler;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.ChangeOrganizationStatusRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.OrganizationResource;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.RegisterOrganizationRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.RegisteredOrganizationResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
@Tag(name = "Organizations", description = "Organizations that use AuxIA")
public class OrganizationController {

  private final RegisterOrganizationCommandHandler registerOrganization;
  private final ChangeOrganizationStatusCommandHandler changeStatus;
  private final OrganizationQueryService organizationQueries;
  private final PlatformKeyGuard platformKey;

  public OrganizationController(
      RegisterOrganizationCommandHandler registerOrganization,
      ChangeOrganizationStatusCommandHandler changeStatus,
      OrganizationQueryService organizationQueries,
      PlatformKeyGuard platformKey) {
    this.registerOrganization = registerOrganization;
    this.changeStatus = changeStatus;
    this.organizationQueries = organizationQueries;
    this.platformKey = platformKey;
  }

  @PostMapping
  @SecurityRequirements
  @Operation(
      summary = "Register an organization with its first administrator",
      description = "Only the platform operator, with the X-Platform-Key header.")
  @ApiResponse(responseCode = "201", description = "Organization and administrator registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "401", description = "Missing or wrong platform key")
  @ApiResponse(responseCode = "409", description = "The RUC or the email is already registered")
  public ResponseEntity<RegisteredOrganizationResource> register(
      @Parameter(description = "Secret of the platform operator")
          @RequestHeader(name = PlatformKeyGuard.HEADER, required = false)
          String key,
      @Valid @RequestBody RegisterOrganizationRequest request) {
    platformKey.check(key);
    var registered = registerOrganization.handle(IdentityResourceAssembler.toCommand(request));
    var resource =
        new RegisteredOrganizationResource(
            IdentityResourceAssembler.toResource(registered.organization()),
            IdentityResourceAssembler.toResource(registered.administrator()));
    return ResponseEntity.created(
            URI.create("/api/v1/organizations/" + registered.organization().id().value()))
        .body(resource);
  }

  @GetMapping("/{organizationId}")
  @Operation(summary = "Get the organization of the user")
  @ApiResponse(responseCode = "200", description = "Organization found")
  @ApiResponse(responseCode = "404", description = "Not found, or not the organization of the user")
  public OrganizationResource findById(
      @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser user) {
    var id = new OrganizationId(organizationId);
    if (!organizationId.equals(user.organizationId())) {
      throw new OrganizationNotFoundException(id);
    }
    return IdentityResourceAssembler.toResource(
        organizationQueries.handle(new GetOrganizationByIdQuery(id)));
  }

  @PatchMapping("/{organizationId}/status")
  @SecurityRequirements
  @Operation(
      summary = "Suspend or reactivate an organization",
      description =
          "Only the platform operator, with the X-Platform-Key header. The users of a suspended"
              + " organization cannot sign in.")
  @ApiResponse(responseCode = "200", description = "Status changed")
  @ApiResponse(responseCode = "401", description = "Missing or wrong platform key")
  @ApiResponse(responseCode = "404", description = "The organization does not exist")
  public OrganizationResource changeStatus(
      @RequestHeader(name = PlatformKeyGuard.HEADER, required = false) String key,
      @PathVariable UUID organizationId,
      @Valid @RequestBody ChangeOrganizationStatusRequest request) {
    platformKey.check(key);
    var command =
        new ChangeOrganizationStatusCommand(new OrganizationId(organizationId), request.status());
    return IdentityResourceAssembler.toResource(changeStatus.handle(command));
  }
}
