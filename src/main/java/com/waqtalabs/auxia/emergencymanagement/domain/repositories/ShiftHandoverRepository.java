package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ShiftHandover;

public interface ShiftHandoverRepository {

  ShiftHandover save(ShiftHandover handover);
}
