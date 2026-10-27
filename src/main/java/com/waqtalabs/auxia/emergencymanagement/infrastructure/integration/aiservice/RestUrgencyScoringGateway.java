package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.services.UrgencyScoringService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Anti-Corruption Layer to {@code POST /api/v1/urgency-scores} of the AI Service. The calculation
 * time is the time of the response.
 */
@Component
class RestUrgencyScoringGateway implements UrgencyScoringService {

  static final String ENDPOINT = "/api/v1/urgency-scores";

  private final RestClient aiServiceRestClient;
  private final Clock clock;

  RestUrgencyScoringGateway(RestClient aiServiceRestClient, Clock clock) {
    this.aiServiceRestClient = aiServiceRestClient;
    this.clock = clock;
  }

  @Override
  public UrgencyScore calculateScore(UrgencyIndicators indicators) {
    var request = new ScoreRequest(AiServiceIndicators.from(indicators), indicators.population());
    ScoreResponse response =
        AiServiceCalls.call(
            ENDPOINT,
            () ->
                aiServiceRestClient
                    .post()
                    .uri(ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(ScoreResponse.class));
    return new UrgencyScore(
        response.score(),
        response.priorityLevel(),
        response.topFeatures().stream()
            .map(feature -> new ScoreContribution(feature.feature(), feature.contribution()))
            .toList(),
        response.modelVersion(),
        LocalDateTime.now(clock));
  }

  record ScoreRequest(AiServiceIndicators indicators, int zonePopulation) {}

  record ScoreResponse(
      double score,
      PriorityLevel priorityLevel,
      List<FeatureContribution> topFeatures,
      String modelVersion) {}

  record FeatureContribution(String feature, double contribution) {}
}
