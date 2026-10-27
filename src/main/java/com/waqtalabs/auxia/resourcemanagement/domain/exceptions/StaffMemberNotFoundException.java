package com.waqtalabs.auxia.resourcemanagement.domain.exceptions;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffMemberId;
import com.waqtalabs.auxia.shared.domain.exceptions.NotFoundException;

public class StaffMemberNotFoundException extends NotFoundException {

  public StaffMemberNotFoundException(StaffMemberId memberId, BrigadeId brigadeId) {
    super(
        "Member " + memberId.value() + " does not belong to brigade " + brigadeId.value(),
        memberId.value(),
        brigadeId.value());
  }
}
