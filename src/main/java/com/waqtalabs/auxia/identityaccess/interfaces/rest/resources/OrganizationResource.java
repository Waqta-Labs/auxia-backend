package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import java.util.UUID;

public record OrganizationResource(
    UUID id, String name, OrganizationType type, String taxId, OrganizationStatus status) {}
