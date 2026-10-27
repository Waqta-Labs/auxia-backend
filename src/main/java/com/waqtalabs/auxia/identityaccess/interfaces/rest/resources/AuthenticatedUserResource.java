package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record AuthenticatedUserResource(
    @Schema(description = "Send it as Authorization: Bearer <token>") String token,
    @Schema(example = "Bearer") String tokenType,
    Instant expiresAt,
    UUID userId,
    UUID organizationId,
    Role role,
    String fullName) {}
