package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Department, province and district of the emergency")
public record AdministrativeLocationResource(
    @Schema(example = "Lima") @NotBlank @Size(max = 100) String department,
    @Schema(example = "Lima") @NotBlank @Size(max = 100) String province,
    @Schema(example = "Lurigancho-Chosica") @NotBlank @Size(max = 100) String district) {}
