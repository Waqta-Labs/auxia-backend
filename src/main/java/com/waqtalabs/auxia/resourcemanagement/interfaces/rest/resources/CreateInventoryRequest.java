package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateInventoryRequest(
    @Schema(
            description =
                "Organization that owns the inventory. It will come from the JWT once Identity"
                    + " Access exists.")
        @NotNull
        UUID organizationId,
    @Schema(example = "Almacén Central Lima") @NotBlank @Size(max = 150) String name) {}
