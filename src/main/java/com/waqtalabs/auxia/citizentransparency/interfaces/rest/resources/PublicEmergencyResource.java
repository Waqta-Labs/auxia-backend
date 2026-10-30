package com.waqtalabs.auxia.citizentransparency.interfaces.rest.resources;

import java.time.LocalDate;
import java.util.UUID;

/** What a citizen needs to recognize an emergency. */
public record PublicEmergencyResource(
    UUID id, String name, String type, PublicLocationResource location, LocalDate startDate) {}
