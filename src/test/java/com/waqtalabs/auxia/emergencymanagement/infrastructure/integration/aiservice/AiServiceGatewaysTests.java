package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * The gateways speak the contract of the AI Service README: same requests, same responses. The
 * client comes from AiServiceConfiguration, so its base URL and API key are also checked.
 */
class AiServiceGatewaysTests {

  private static final String API_KEY = "test-api-key-0123456789";
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-03-14T14:30:00Z"), ZoneOffset.ofHours(-5));

  private final RestClient.Builder builder =
      new AiServiceConfiguration()
          .aiServiceRestClient(
              new AiServiceProperties(
                  URI.create("http://ai-service"),
                  API_KEY,
                  Duration.ofSeconds(1),
                  Duration.ofSeconds(1)))
          .mutate();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final RestClient client = builder.build();
  private final RestFieldReportStructuringGateway structuring =
      new RestFieldReportStructuringGateway(client);
  private final RestUrgencyScoringGateway scoring = new RestUrgencyScoringGateway(client, CLOCK);

  @Test
  void structuringSendsTheTextAndTranslatesTheIndicators() {
    server
        .expect(requestTo("http://ai-service/api/v1/field-reports/structure"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(AiServiceConfiguration.API_KEY_HEADER, API_KEY))
        .andExpect(content().json("{\"text\": \"Derrumbe de viviendas\"}", JsonCompareMode.STRICT))
        .andRespond(
            withSuccess(
                """
                {
                  "indicators": {
                    "damageLevel": "SEVERE",
                    "familiesAffected": 35,
                    "peopleAffected": 140,
                    "children": 40,
                    "elderlyPeople": 15,
                    "disabledPeople": null,
                    "needs": ["WATER", "FOOD", "SHELTER"]
                  },
                  "missingVariables": ["disabledPeople"],
                  "modelVersion": "rules-v1"
                }
                """,
                MediaType.APPLICATION_JSON));

    StructuredFieldData data = structuring.structure("Derrumbe de viviendas");

    server.verify();
    assertThat(data)
        .isEqualTo(
            new StructuredFieldData(
                DamageLevel.SEVERE,
                35,
                140,
                40,
                15,
                null,
                Set.of(Need.WATER, Need.FOOD, Need.SHELTER),
                List.of("disabledPeople")));
  }

  @Test
  void scoringSendsTheIndicatorsWithTheZonePopulationAndTranslatesTheScore() {
    server
        .expect(requestTo("http://ai-service/api/v1/urgency-scores"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(AiServiceConfiguration.API_KEY_HEADER, API_KEY))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "indicators": {
                        "damageLevel": "SEVERE",
                        "familiesAffected": 35,
                        "peopleAffected": 140,
                        "children": 40,
                        "elderlyPeople": 15,
                        "disabledPeople": 0,
                        "needs": ["WATER", "FOOD", "SHELTER"]
                      },
                      "zonePopulation": 600
                    }
                    """,
                    JsonCompareMode.STRICT))
        .andRespond(
            withSuccess(
                """
                {
                  "score": 54.9,
                  "priorityLevel": "HIGH",
                  "topFeatures": [
                    { "feature": "damageLevel", "contribution": 30.0 },
                    { "feature": "needs", "contribution": 11.3 },
                    { "feature": "vulnerablePopulation", "contribution": 7.9 }
                  ],
                  "modelVersion": "rules-v1"
                }
                """,
                MediaType.APPLICATION_JSON));

    UrgencyScore score =
        scoring.calculateScore(
            new UrgencyIndicators(
                DamageLevel.SEVERE,
                600,
                35,
                140,
                40,
                15,
                0,
                Set.of(Need.SHELTER, Need.WATER, Need.FOOD)));

    server.verify();
    assertThat(score)
        .isEqualTo(
            new UrgencyScore(
                54.9,
                PriorityLevel.HIGH,
                List.of(
                    new ScoreContribution("damageLevel", 30.0),
                    new ScoreContribution("needs", 11.3),
                    new ScoreContribution("vulnerablePopulation", 7.9)),
                "rules-v1",
                LocalDateTime.of(2026, 3, 14, 9, 30)));
  }

  @Test
  void errorStatusMakesTheServiceUnavailable() {
    server
        .expect(requestTo("http://ai-service/api/v1/field-reports/structure"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> structuring.structure("Derrumbe"))
        .isInstanceOf(ExternalServiceUnavailableException.class)
        .hasMessage("AI Service is not available");
  }

  @Test
  void rejectedRequestMakesTheServiceUnavailable() {
    server
        .expect(requestTo("http://ai-service/api/v1/urgency-scores"))
        .andRespond(withBadRequest());

    assertThatThrownBy(() -> scoring.calculateScore(indicators()))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  @Test
  void responseOutsideTheContractMakesTheServiceUnavailable() {
    server
        .expect(requestTo("http://ai-service/api/v1/urgency-scores"))
        .andRespond(withSuccess("{\"priorityLevel\": \"UNKNOWN\"}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> scoring.calculateScore(indicators()))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  @Test
  void serviceThatDoesNotAnswerIsUnavailable() {
    RestClient unreachable =
        new AiServiceConfiguration()
            .aiServiceRestClient(
                new AiServiceProperties(
                    URI.create("http://localhost:1"),
                    API_KEY,
                    Duration.ofSeconds(1),
                    Duration.ofSeconds(1)));

    assertThatThrownBy(() -> new RestFieldReportStructuringGateway(unreachable).structure("x"))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  private static UrgencyIndicators indicators() {
    return new UrgencyIndicators(DamageLevel.LOW, 100, 0, 10, 0, 0, 0, Set.of());
  }
}
