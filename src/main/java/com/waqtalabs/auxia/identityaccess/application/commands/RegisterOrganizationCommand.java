package com.waqtalabs.auxia.identityaccess.application.commands;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;

/** Registers an organization together with its first ORGANIZATION_ADMIN. */
public record RegisterOrganizationCommand(
    String name,
    OrganizationType type,
    String taxId,
    String adminEmail,
    String adminFirstName,
    String adminLastName,
    String adminPassword) {}
