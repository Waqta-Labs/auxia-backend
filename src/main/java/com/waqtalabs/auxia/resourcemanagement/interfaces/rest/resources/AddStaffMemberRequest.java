package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddStaffMemberRequest(
    @Schema(example = "Rosa Quispe") @NotBlank @Size(max = 150) String fullName,
    @Schema(example = "LEADER") @NotNull StaffRole role,
    @Schema(description = "Optional", example = "987654321") @Size(max = 20) String phone) {}
