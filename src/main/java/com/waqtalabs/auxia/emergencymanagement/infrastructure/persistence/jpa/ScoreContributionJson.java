package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

/** One element of the {@code urgency_top_features} JSON array of {@code zones}. */
record ScoreContributionJson(String feature, double contribution) {}
