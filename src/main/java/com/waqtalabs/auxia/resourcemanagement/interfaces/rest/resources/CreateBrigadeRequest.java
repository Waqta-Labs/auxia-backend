package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The organization of the brigade is the one of the signed-in administrator. */
public record CreateBrigadeRequest(
    @Schema(example = "Brigada Chosica 1") @NotBlank @Size(max = 150) String name,
    @Schema(description = "Base of the brigade") @NotNull @Valid LocationResource baseLocation) {}
