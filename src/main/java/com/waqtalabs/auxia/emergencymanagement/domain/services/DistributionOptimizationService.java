package com.waqtalabs.auxia.emergencymanagement.domain.services;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionRecommendation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PrioritizedZone;
import java.util.List;

/**
 * Recommends how to split the available inventory among the prioritized zones without exceeding
 * it (US-11). The domain defines only the interface: the implementation calls the AI Service
 * through an Anti-Corruption Layer (TS-C04).
 */
public interface DistributionOptimizationService {

  DistributionRecommendation recommend(
      List<PrioritizedZone> zones, List<AvailableResource> inventory);
}
