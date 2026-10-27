package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;

public record RemoveStaffMemberCommand(
    BrigadeId brigadeId, OrganizationId organizationId, StaffMemberId memberId) {}
