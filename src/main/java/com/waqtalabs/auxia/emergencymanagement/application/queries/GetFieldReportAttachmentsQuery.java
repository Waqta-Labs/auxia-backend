package com.waqtalabs.auxia.emergencymanagement.application.queries;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;

/**
 * The photo and audio a citizen sent with the SOS behind a field report.
 *
 * @param attachmentId one of them with its content, or {@code null} for the list
 */
public record GetFieldReportAttachmentsQuery(
    FieldReportId fieldReportId, SosAttachmentId attachmentId) {}
