package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.DeviceSignInCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commandhandlers.SignInCommandHandler;
import com.waqtalabs.auxia.identityaccess.application.commands.SignInCommand;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.assemblers.IdentityResourceAssembler;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.AuthenticatedUserResource;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.BrigadeRegistrationResource.DeviceSignIn;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.SignInRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Sign-in with institutional credentials (US-21)")
public class AuthenticationController {

  private final SignInCommandHandler signIn;
  private final DeviceSignInCommandHandler deviceSignIn;

  public AuthenticationController(
      SignInCommandHandler signIn, DeviceSignInCommandHandler deviceSignIn) {
    this.signIn = signIn;
    this.deviceSignIn = deviceSignIn;
  }

  @PostMapping("/sign-in")
  @SecurityRequirements
  @Operation(
      summary = "Sign in",
      description =
          "Returns the access token with the role and the organization of the user. Every"
              + " failure answers 401 with the same message, without saying what was wrong. The"
              + " fifth failed attempt in a row locks the account.")
  @ApiResponse(responseCode = "200", description = "Signed in")
  @ApiResponse(responseCode = "400", description = "Missing email or password")
  @ApiResponse(responseCode = "401", description = "Invalid credentials")
  public AuthenticatedUserResource signIn(@Valid @RequestBody SignInRequest request) {
    return IdentityResourceAssembler.toResource(
        signIn.handle(new SignInCommand(request.email(), request.password())));
  }

  @PostMapping("/device-sign-in")
  @SecurityRequirements
  @Operation(
      summary = "Sign in a brigadist with the secret of their phone",
      description =
          "For a brigadist whose request (A_02) was approved: registrationId and deviceSecret"
              + " instead of e-mail and password. The same token and the same 401 as sign-in.")
  @ApiResponse(responseCode = "200", description = "Signed in")
  @ApiResponse(responseCode = "401", description = "Not approved, or wrong secret")
  public AuthenticatedUserResource deviceSignIn(@Valid @RequestBody DeviceSignIn request) {
    return IdentityResourceAssembler.toResource(
        deviceSignIn.handle(
            new BrigadeRegistrationId(request.registrationId()), request.deviceSecret()));
  }
}
