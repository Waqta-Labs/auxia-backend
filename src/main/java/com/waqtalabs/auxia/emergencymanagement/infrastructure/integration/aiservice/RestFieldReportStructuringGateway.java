package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.emergencymanagement.application.ports.FieldReportStructuringService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Anti-Corruption Layer to {@code POST /api/v1/field-reports/structure} of the AI Service. */
@Component
class RestFieldReportStructuringGateway implements FieldReportStructuringService {

  static final String ENDPOINT = "/api/v1/field-reports/structure";

  private final RestClient aiServiceRestClient;

  RestFieldReportStructuringGateway(RestClient aiServiceRestClient) {
    this.aiServiceRestClient = aiServiceRestClient;
  }

  @Override
  public StructuredFieldData structure(String rawText) {
    StructureResponse response =
        AiServiceCalls.call(
            ENDPOINT,
            () ->
                aiServiceRestClient
                    .post()
                    .uri(ENDPOINT)
                    .body(new StructureRequest(rawText))
                    .retrieve()
                    .body(StructureResponse.class));
    AiServiceIndicators indicators = response.indicators();
    return new StructuredFieldData(
        indicators.damageLevel(),
        indicators.familiesAffected(),
        indicators.peopleAffected(),
        indicators.children(),
        indicators.elderlyPeople(),
        indicators.disabledPeople(),
        indicators.needs() == null ? Set.of() : Set.copyOf(indicators.needs()),
        response.missingVariables());
  }

  record StructureRequest(String text) {}

  record StructureResponse(AiServiceIndicators indicators, List<String> missingVariables) {}
}
