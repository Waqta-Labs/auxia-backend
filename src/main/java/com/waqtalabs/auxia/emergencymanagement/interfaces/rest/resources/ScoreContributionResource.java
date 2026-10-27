package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Score points that one variable added")
public record ScoreContributionResource(
    @Schema(example = "damageLevel") String feature,
    @Schema(example = "30.0") double contribution) {}
