package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateBrigadeRequest(
    @Schema(
            description =
                "Organization of the brigade. It will come from the JWT once Identity Access"
                    + " exists.")
        @NotNull
        UUID organizationId,
    @Schema(example = "Brigada Chosica 1") @NotBlank @Size(max = 150) String name,
    @Schema(description = "Base of the brigade") @NotNull @Valid LocationResource baseLocation) {}
