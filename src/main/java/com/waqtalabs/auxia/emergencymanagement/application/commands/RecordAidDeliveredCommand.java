package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import java.time.LocalDateTime;

/** A brigade delivered an approved plan in its zone. */
public record RecordAidDeliveredCommand(
    DistributionPlanId distributionPlanId, LocalDateTime deliveredAt) {}
