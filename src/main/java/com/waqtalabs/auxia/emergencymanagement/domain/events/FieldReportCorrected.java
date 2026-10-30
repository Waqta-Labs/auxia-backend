package com.waqtalabs.auxia.emergencymanagement.domain.events;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDateTime;

/**
 * An authority corrected the indicators of a report. The score of its zone is calculated again.
 *
 * @param zoneId {@code null} while the report is pending zone assignment
 */
public record FieldReportCorrected(
    FieldReportId fieldReportId,
    ZoneId zoneId,
    AuthorityId correctedBy,
    String reason,
    LocalDateTime correctedAt)
    implements DomainEvent {}
