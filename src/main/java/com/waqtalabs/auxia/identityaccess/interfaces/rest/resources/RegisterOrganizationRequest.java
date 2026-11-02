package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterOrganizationRequest(
    @Schema(example = "COER Lima") @NotBlank @Size(max = 150) String name,
    @Schema(example = "GOVERNMENT") @NotNull OrganizationType type,
    @Schema(description = "RUC, 11 digits", example = "20131370998")
        @NotNull
        @Pattern(regexp = "\\d{11}")
        String taxId,
    @NotNull @Valid NewAdministratorRequest administrator) {}
