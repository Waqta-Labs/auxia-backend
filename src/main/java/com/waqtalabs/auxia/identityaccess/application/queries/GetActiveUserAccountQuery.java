package com.waqtalabs.auxia.identityaccess.application.queries;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;

/** The current account of a signed-in user, while it and its organization are active. */
public record GetActiveUserAccountQuery(UserId userId) {}
