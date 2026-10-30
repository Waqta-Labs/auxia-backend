package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import java.util.Set;

/**
 * Corrects or completes the indicators of a structured report (US-03). An indicator that is
 * {@code null} keeps its current value; {@code needs} replaces the needs when it is not null.
 */
public record CorrectFieldReportCommand(
    FieldReportId fieldReportId,
    AuthorityId authorityId,
    DamageLevel damageLevel,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople,
    Set<Need> needs,
    String reason) {}
