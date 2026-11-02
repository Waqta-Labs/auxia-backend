package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeAccountStatusRequest(
    @Schema(description = "ACTIVE to enable or unlock, DISABLED to disable", example = "DISABLED")
        @NotNull
        AccountStatus status) {}
