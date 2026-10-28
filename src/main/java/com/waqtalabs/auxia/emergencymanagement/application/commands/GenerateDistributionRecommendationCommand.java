package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;

/** An authority asks for the distribution of the inventory of its organization (US-11). */
public record GenerateDistributionRecommendationCommand(
    EmergencyId emergencyId, AuthorityId authorityId, OrganizationId organizationId) {}
