package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
    @Schema(example = "rosa.quispe@coer.gob.pe") @NotBlank @Email @Size(max = 254) String email,
    @Schema(example = "Rosa") @NotBlank @Size(max = 100) String firstName,
    @Schema(example = "Quispe") @NotBlank @Size(max = 100) String lastName,
    @Schema(example = "AUTHORITY") @NotNull Role role,
    @Schema(description = "8 to 72 characters", example = "Chosica-2026")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {}
