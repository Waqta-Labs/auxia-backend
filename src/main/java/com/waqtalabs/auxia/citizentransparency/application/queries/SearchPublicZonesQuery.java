package com.waqtalabs.auxia.citizentransparency.application.queries;

/**
 * Zones of a district.
 *
 * @param name part of the name of the zone; {@code null} for every zone of the district
 */
public record SearchPublicZonesQuery(String district, String name) {}
