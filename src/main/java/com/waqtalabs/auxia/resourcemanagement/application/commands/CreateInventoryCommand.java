package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;

public record CreateInventoryCommand(OrganizationId organizationId, String name) {}
