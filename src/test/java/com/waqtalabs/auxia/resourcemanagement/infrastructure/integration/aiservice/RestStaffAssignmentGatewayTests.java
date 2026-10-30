package com.waqtalabs.auxia.resourcemanagement.infrastructure.integration.aiservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.infrastructure.aiservice.AiServiceConfiguration;
import com.waqtalabs.auxia.shared.infrastructure.aiservice.AiServiceProperties;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** The gateway speaks the brigade suggestion contract of the AI Service README. */
class RestStaffAssignmentGatewayTests {

  private static final String API_KEY = "test-api-key-0123456789";
  private static final String URL =
      "http://ai-service/api/v1/brigade-assignments/suggestions";
  private static final Location ZONE = new Location(-12.0464, -77.0428);

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
  private final RestStaffAssignmentGateway gateway =
      new RestStaffAssignmentGateway(builder.build());

  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final Brigade near =
      Brigade.create(organization, "Brigada Callao", new Location(-12.056, -77.0844));
  private final Brigade far =
      Brigade.create(organization, "Brigada Miraflores", new Location(-12.121, -77.03));

  @Test
  void suggestionSendsTheCandidatesInOrderAndReturnsTheChosenOne() {
    server
        .expect(requestTo(URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(AiServiceConfiguration.API_KEY_HEADER, API_KEY))
        .andExpect(
            content()
                .json(
                    """
                    {
                      "zoneLocation": {"latitude": -12.0464, "longitude": -77.0428},
                      "brigades": [
                        {
                          "brigadeId": "%s",
                          "location": {"latitude": -12.056, "longitude": -77.0844}
                        },
                        {
                          "brigadeId": "%s",
                          "location": {"latitude": -12.121, "longitude": -77.03}
                        }
                      ]
                    }
                    """
                        .formatted(near.id().value(), far.id().value()),
                    JsonCompareMode.STRICT))
        .andRespond(withSuccess(response(near), MediaType.APPLICATION_JSON));

    assertThat(gateway.suggestBrigade(List.of(near, far), ZONE)).isEqualTo(near.id());
    server.verify();
  }

  @Test
  void suggestionOfABrigadeThatIsNotACandidateIsRejected() {
    Brigade stranger = Brigade.create(organization, "Otra", ZONE);
    server
        .expect(requestTo(URL))
        .andRespond(withSuccess(response(stranger), MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> gateway.suggestBrigade(List.of(near, far), ZONE))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  @Test
  void failureOfTheServiceMakesItUnavailable() {
    server.expect(requestTo(URL)).andRespond(withServerError());

    assertThatThrownBy(() -> gateway.suggestBrigade(List.of(near), ZONE))
        .isInstanceOf(ExternalServiceUnavailableException.class);
  }

  private static String response(Brigade brigade) {
    return """
        {"brigadeId": "%s", "distanceKm": 4.6, "modelVersion": "rules-v1"}
        """
        .formatted(brigade.id().value());
  }
}
