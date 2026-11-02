package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

@Schema(
    description =
        "Indicators extracted by the NLP (US-02). An indicator that could not be identified is"
            + " null and is listed in missingVariables.")
public record StructuredFieldDataResource(
    DamageLevel damageLevel,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople,
    Set<Need> needs,
    @Schema(example = "[\"disabledPeople\"]") List<String> missingVariables) {}
