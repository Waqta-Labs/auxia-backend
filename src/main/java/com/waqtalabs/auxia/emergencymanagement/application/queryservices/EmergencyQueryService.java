package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetActiveEmergenciesQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetEmergencyByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmergencyQueryService {

  private final EmergencyRepository emergencyRepository;

  public EmergencyQueryService(EmergencyRepository emergencyRepository) {
    this.emergencyRepository = emergencyRepository;
  }

  public Emergency handle(GetEmergencyByIdQuery query) {
    return emergencyRepository
        .findById(query.emergencyId())
        .orElseThrow(() -> new EmergencyNotFoundException(query.emergencyId()));
  }

  public List<Emergency> handle(GetActiveEmergenciesQuery query) {
    return emergencyRepository.findActive();
  }
}
