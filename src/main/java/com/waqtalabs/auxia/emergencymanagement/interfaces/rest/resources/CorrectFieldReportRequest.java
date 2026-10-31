package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;

/** Only the indicators to correct or complete; the others keep their current value. */
public record CorrectFieldReportRequest(
    @Schema(example = "CRITICAL") DamageLevel damageLevel,
    @PositiveOrZero Integer familiesAffected,
    @Schema(example = "180") @PositiveOrZero Integer peopleAffected,
    @PositiveOrZero Integer children,
    @PositiveOrZero Integer elderlyPeople,
    @PositiveOrZero Integer disabledPeople,
    @Schema(description = "Replaces the needs when present") Set<Need> needs,
    @Schema(example = "Conteo en campo de la brigada") @NotBlank String reason) {}
