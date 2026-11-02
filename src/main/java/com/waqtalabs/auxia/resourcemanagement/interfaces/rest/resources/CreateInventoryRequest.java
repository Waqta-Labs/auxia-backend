package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The organization of the inventory is the one of the signed-in administrator. */
public record CreateInventoryRequest(
    @Schema(example = "Almacén Central Lima") @NotBlank @Size(max = 150) String name) {}
