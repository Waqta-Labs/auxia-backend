package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterEmergencyRequest(
    @Schema(example = "Huaico Chosica 2026") @NotBlank @Size(max = 150) String name,
    @Schema(example = "LANDSLIDE") @NotNull EmergencyType type,
    @Schema(example = "Huaico tras lluvias intensas en la quebrada Pedregal")
        @Size(max = 2000)
        String description,
    @NotNull @Valid AdministrativeLocationResource location,
    @Schema(example = "2026-03-14") @NotNull @PastOrPresent LocalDate startDate) {}
