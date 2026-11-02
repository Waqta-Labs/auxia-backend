package com.waqtalabs.auxia.identityaccess.interfaces.rest.resources;

public record RegisteredOrganizationResource(
    OrganizationResource organization, UserAccountResource administrator) {}
