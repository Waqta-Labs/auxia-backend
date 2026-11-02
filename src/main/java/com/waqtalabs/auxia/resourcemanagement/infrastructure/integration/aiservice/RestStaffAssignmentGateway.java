package com.waqtalabs.auxia.resourcemanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.services.StaffAssignmentService;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import com.waqtalabs.auxia.shared.infrastructure.aiservice.AiServiceCalls;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Anti-Corruption Layer to {@code POST /api/v1/brigade-assignments/suggestions} of the AI
 * Service. The brigades are sent in the order received, which decides a tie.
 */
@Component
class RestStaffAssignmentGateway implements StaffAssignmentService {

  static final String ENDPOINT = "/api/v1/brigade-assignments/suggestions";

  private static final Logger LOGGER = LoggerFactory.getLogger(RestStaffAssignmentGateway.class);

  private final RestClient aiServiceRestClient;

  RestStaffAssignmentGateway(RestClient aiServiceRestClient) {
    this.aiServiceRestClient = aiServiceRestClient;
  }

  /**
   * @throws ExternalServiceUnavailableException if the service fails or suggests a brigade that
   *     is not a candidate
   */
  @Override
  public BrigadeId suggestBrigade(List<Brigade> candidates, Location zoneLocation) {
    var request =
        new SuggestionRequest(
            LocationRequest.of(zoneLocation),
            candidates.stream()
                .map(
                    brigade ->
                        new BrigadeRequest(
                            brigade.id().value(), LocationRequest.of(brigade.baseLocation())))
                .toList());
    SuggestionResponse response =
        AiServiceCalls.call(
            ENDPOINT,
            () ->
                aiServiceRestClient
                    .post()
                    .uri(ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(SuggestionResponse.class));
    BrigadeId suggested = new BrigadeId(response.brigadeId());
    if (candidates.stream().noneMatch(brigade -> brigade.id().equals(suggested))) {
      LOGGER.warn("{} suggested brigade {}, which is not a candidate", ENDPOINT, suggested.value());
      throw new ExternalServiceUnavailableException(AiServiceCalls.SERVICE_NAME, null);
    }
    return suggested;
  }

  record SuggestionRequest(LocationRequest zoneLocation, List<BrigadeRequest> brigades) {}

  record BrigadeRequest(UUID brigadeId, LocationRequest location) {}

  record LocationRequest(double latitude, double longitude) {

    static LocationRequest of(Location location) {
      return new LocationRequest(location.latitude(), location.longitude());
    }
  }

  record SuggestionResponse(UUID brigadeId, double distanceKm, String modelVersion) {}
}
