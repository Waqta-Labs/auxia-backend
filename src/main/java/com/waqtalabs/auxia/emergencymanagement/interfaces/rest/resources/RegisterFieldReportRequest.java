package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Field report of a known zone, which comes in the path. */
public record RegisterFieldReportRequest(
    @Schema(
            description = "Free text of the report, in Spanish",
            example =
                "Derrumbe de viviendas. Hay 35 familias afectadas, unas 140 personas, entre ellas"
                    + " 40 niños. Urge agua potable, alimentos y carpas.")
        @NotBlank
        @Size(max = 5000)
        String text,
    @Schema(example = "AUTHORITY") @NotNull ReportSource source) {}
