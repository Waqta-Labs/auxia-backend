package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;

/** What a citizen sent in the SOS behind a field report, besides its text. */
public record GetFieldReportSosQuery(FieldReportId fieldReportId) {}
