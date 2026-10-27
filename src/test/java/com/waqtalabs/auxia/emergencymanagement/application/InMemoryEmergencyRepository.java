package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository, in registration order. */
public class InMemoryEmergencyRepository implements EmergencyRepository {

  private final Map<EmergencyId, Emergency> emergencies = new LinkedHashMap<>();

  @Override
  public Emergency save(Emergency emergency) {
    emergencies.put(emergency.id(), emergency);
    return emergency;
  }

  @Override
  public Optional<Emergency> findById(EmergencyId id) {
    return Optional.ofNullable(emergencies.get(id));
  }

  @Override
  public List<Emergency> findActive() {
    return emergencies.values().stream().filter(Emergency::isActive).toList();
  }
}
