package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@Schema(example = "AUDITOR") @NotNull Role role) {}
