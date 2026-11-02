package com.waqtalabs.auxia.identityaccess.application.queries;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import java.util.Set;

/** Whether a user may run a critical command now, with any of the given roles. */
public record CheckAccessQuery(UserId userId, Set<Role> roles) {}
