package com.waqtalabs.auxia.resourcemanagement.application.queries;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;

/** Missions of the brigade the signed-in brigadist is a member of (A_07). */
public record GetMyMissionsQuery(UserId userId) {}
