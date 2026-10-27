package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** First ORGANIZATION_ADMIN of a new organization. */
public record NewAdministratorRequest(
    @Schema(example = "admin@coer.gob.pe") @NotBlank @Email @Size(max = 254) String email,
    @Schema(example = "Carlos") @NotBlank @Size(max = 100) String firstName,
    @Schema(example = "Huamán") @NotBlank @Size(max = 100) String lastName,
    @Schema(description = "8 to 72 characters", example = "Admin-COER-2026")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {}
