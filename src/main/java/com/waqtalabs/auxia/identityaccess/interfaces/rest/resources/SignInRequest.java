package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
    @Schema(example = "rosa.quispe@coer.gob.pe") @NotBlank String email,
    @Schema(example = "Chosica-2026") @NotBlank String password) {}
