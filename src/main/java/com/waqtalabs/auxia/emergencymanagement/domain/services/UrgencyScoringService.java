package com.waqtalabs.auxia.emergencymanagement.domain.services;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;

/**
 * Calculates the urgency score of a zone (US-04). The domain defines only the interface: the
 * implementation calls the AI Service through an Anti-Corruption Layer (TS-C04).
 */
public interface UrgencyScoringService {

  UrgencyScore calculateScore(UrgencyIndicators indicators);
}
