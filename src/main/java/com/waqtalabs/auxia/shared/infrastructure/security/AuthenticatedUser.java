package com.waqtalabs.auxia.shared.infrastructure.security;

import java.util.UUID;

/**
 * User of the current request, read from its JWT. Controllers of every bounded context receive it
 * with {@code @AuthenticationPrincipal}, for example to know the organization of the user.
 *
 * @param role name of the role, such as {@code AUTHORITY}
 */
public record AuthenticatedUser(UUID userId, UUID organizationId, String role) {}
