package com.waqtalabs.auxia.resourcemanagement.domain.services;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import java.util.List;

/** Chooses which brigade delivers the plan of a zone. The AI Service implements it. */
public interface StaffAssignmentService {

  /**
   * The most suitable brigade for the zone, one of the candidates.
   *
   * @param candidates available brigades with a leader; never empty
   */
  BrigadeId suggestBrigade(List<Brigade> candidates, Location zoneLocation);
}
