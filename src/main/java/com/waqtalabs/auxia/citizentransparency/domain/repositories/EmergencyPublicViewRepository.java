package com.waqtalabs.auxia.citizentransparency.domain.repositories;

import com.waqtalabs.auxia.citizentransparency.domain.model.readmodels.EmergencyPublicView;
import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.EmergencyId;
import java.util.List;
import java.util.Optional;

public interface EmergencyPublicViewRepository {

  EmergencyPublicView save(EmergencyPublicView view);

  Optional<EmergencyPublicView> findById(EmergencyId emergencyId);

  /** Every emergency, the most recent first. */
  List<EmergencyPublicView> findAll();
}
