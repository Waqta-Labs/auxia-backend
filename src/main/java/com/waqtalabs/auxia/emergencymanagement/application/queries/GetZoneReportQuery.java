package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.Locale;

/**
 * The exported report of a zone.
 *
 * @param locale language of the document, the same one the API answers in
 */
public record GetZoneReportQuery(ZoneId zoneId, Locale locale) {}
