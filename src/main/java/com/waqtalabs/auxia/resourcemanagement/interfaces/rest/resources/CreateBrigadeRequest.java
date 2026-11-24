package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The organization of the brigade is the one of the signed-in administrator. */
public record CreateBrigadeRequest(
    @Schema(example = "Brigada Chosica 1") @NotBlank @Size(max = 150) String name,
    @Schema(description = "Base of the brigade") @NotNull @Valid LocationResource baseLocation,
    @Schema(
            description =
                "Optional: code a brigadist writes to register in the mobile app (A_02), unique"
                    + " in the platform. Generated when it is missing.",
            example = "BRG-ALFA-07")
        @Pattern(regexp = "[A-Za-z0-9-]{3,30}")
        String code) {}
