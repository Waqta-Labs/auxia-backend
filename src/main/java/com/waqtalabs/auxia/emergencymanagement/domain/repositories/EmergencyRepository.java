package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import java.util.List;
import java.util.Optional;

public interface EmergencyRepository {

  Emergency save(Emergency emergency);

  Optional<Emergency> findById(EmergencyId id);

  List<Emergency> findActive();
}
