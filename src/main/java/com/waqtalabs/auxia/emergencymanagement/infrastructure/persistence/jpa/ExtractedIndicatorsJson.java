package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import java.util.List;

/**
 * JSON of the {@code extracted_indicators} column of {@code field_reports}: the indicators of a
 * structured report, with {@code null} for the ones that were not identified.
 */
record ExtractedIndicatorsJson(
    DamageLevel damageLevel,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople,
    List<Need> needs) {}
