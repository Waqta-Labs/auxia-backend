package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.ChangeAccountStatusCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.ChangeUserRoleCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.RegisterUserCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commands.ChangeAccountStatusCommand;
import com.waqtalabs.auxia.identityaccess.application.commands.ChangeUserRoleCommand;
import com.waqtalabs.auxia.identityaccess.application.queries.GetUserAccountByIdQuery;
import com.waqtalabs.auxia.identityaccess.application.queries.GetUserAccountsByOrganizationIdQuery;
import com.waqtalabs.auxia.identityaccess.application.queryservices.UserAccountQueryService;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.assemblers.IdentityResourceAssembler;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.ChangeAccountStatusRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.ChangeRoleRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.RegisterUserRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.UserAccountResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Users of an organization. Except {@code /users/me}, only an ORGANIZATION_ADMIN of the same
 * organization can use these endpoints (US-22); users of other organizations are not found.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Users", description = "User accounts and their roles (US-22)")
public class UserAccountController {

  private final RegisterUserCommandHandler registerUser;
  private final ChangeUserRoleCommandHandler changeRole;
  private final ChangeAccountStatusCommandHandler changeStatus;
  private final UserAccountQueryService userQueries;

  public UserAccountController(
      RegisterUserCommandHandler registerUser,
      ChangeUserRoleCommandHandler changeRole,
      ChangeAccountStatusCommandHandler changeStatus,
      UserAccountQueryService userQueries) {
    this.registerUser = registerUser;
    this.changeRole = changeRole;
    this.changeStatus = changeStatus;
    this.userQueries = userQueries;
  }

  @PostMapping("/organizations/{organizationId}/users")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "Register a user of the organization")
  @ApiResponse(responseCode = "201", description = "User registered")
  @ApiResponse(responseCode = "400", description = "Invalid request")
  @ApiResponse(responseCode = "403", description = "The user is not an ORGANIZATION_ADMIN")
  @ApiResponse(responseCode = "404", description = "Not the organization of the administrator")
  @ApiResponse(responseCode = "409", description = "The email is already registered")
  public ResponseEntity<UserAccountResource> register(
      @PathVariable UUID organizationId,
      @Valid @RequestBody RegisterUserRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    UserAccount account =
        registerUser.handle(
            IdentityResourceAssembler.toCommand(ownOrganization(organizationId, user), request));
    return ResponseEntity.created(URI.create("/api/v1/users/" + account.id().value()))
        .body(IdentityResourceAssembler.toResource(account));
  }

  @GetMapping("/organizations/{organizationId}/users")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(summary = "List the users of the organization", description = "Ordered by email.")
  @ApiResponse(responseCode = "200", description = "Users of the organization")
  @ApiResponse(responseCode = "403", description = "The user is not an ORGANIZATION_ADMIN")
  @ApiResponse(responseCode = "404", description = "Not the organization of the administrator")
  public List<UserAccountResource> findByOrganization(
      @PathVariable UUID organizationId, @AuthenticationPrincipal AuthenticatedUser user) {
    var query = new GetUserAccountsByOrganizationIdQuery(ownOrganization(organizationId, user));
    return userQueries.handle(query).stream().map(IdentityResourceAssembler::toResource).toList();
  }

  @GetMapping("/users/me")
  @Operation(summary = "Get the signed-in user")
  @ApiResponse(responseCode = "200", description = "The user of the token")
  public UserAccountResource me(@AuthenticationPrincipal AuthenticatedUser user) {
    return IdentityResourceAssembler.toResource(
        userQueries.handle(new GetUserAccountByIdQuery(new UserId(user.userId()))));
  }

  @PatchMapping("/users/{userId}/role")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Change the role of a user",
      description = "It applies from the next sign-in. An administrator cannot change their own.")
  @ApiResponse(responseCode = "200", description = "Role changed")
  @ApiResponse(responseCode = "403", description = "The user is not an ORGANIZATION_ADMIN")
  @ApiResponse(responseCode = "404", description = "Not a user of the organization")
  @ApiResponse(responseCode = "422", description = "The administrator changes their own role")
  public UserAccountResource changeRole(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeRoleRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new ChangeUserRoleCommand(
            new UserId(user.userId()),
            new OrganizationId(user.organizationId()),
            new UserId(userId),
            request.role());
    return IdentityResourceAssembler.toResource(changeRole.handle(command));
  }

  @PatchMapping("/users/{userId}/status")
  @PreAuthorize("hasRole('ORGANIZATION_ADMIN')")
  @Operation(
      summary = "Enable, unlock or disable a user",
      description = "ACTIVE enables or unlocks the account; DISABLED disables it.")
  @ApiResponse(responseCode = "200", description = "Status changed")
  @ApiResponse(responseCode = "403", description = "The user is not an ORGANIZATION_ADMIN")
  @ApiResponse(responseCode = "404", description = "Not a user of the organization")
  @ApiResponse(responseCode = "422", description = "LOCKED, or the own account of the admin")
  public UserAccountResource changeStatus(
      @PathVariable UUID userId,
      @Valid @RequestBody ChangeAccountStatusRequest request,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new ChangeAccountStatusCommand(
            new UserId(user.userId()),
            new OrganizationId(user.organizationId()),
            new UserId(userId),
            request.status());
    return IdentityResourceAssembler.toResource(changeStatus.handle(command));
  }

  /** An administrator only manages their own organization; another one is not found. */
  private static OrganizationId ownOrganization(UUID organizationId, AuthenticatedUser user) {
    var id = new OrganizationId(organizationId);
    if (!organizationId.equals(user.organizationId())) {
      throw new OrganizationNotFoundException(id);
    }
    return id;
  }
}
