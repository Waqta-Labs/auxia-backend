package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.aiservice;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionRecommendation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PrioritizedZone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneAllocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.services.DistributionOptimizationService;
import com.waqtalabs.auxia.shared.infrastructure.aiservice.AiServiceCalls;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Anti-Corruption Layer to {@code POST /api/v1/distribution-recommendations} of the AI Service.
 * The needs and the already allocated categories of each zone are sent in a fixed order, so the
 * same zones always make the same request.
 */
@Component
class RestDistributionOptimizationGateway implements DistributionOptimizationService {

  static final String ENDPOINT = "/api/v1/distribution-recommendations";

  private final RestClient aiServiceRestClient;

  RestDistributionOptimizationGateway(RestClient aiServiceRestClient) {
    this.aiServiceRestClient = aiServiceRestClient;
  }

  @Override
  public DistributionRecommendation recommend(
      List<PrioritizedZone> zones, List<AvailableResource> inventory) {
    var request =
        new RecommendationRequest(
            zones.stream()
                .map(
                    zone ->
                        new ZoneRequest(
                            zone.zoneId().value(),
                            zone.score(),
                            zone.peopleAffected(),
                            zone.needs().stream().sorted().toList(),
                            zone.alreadyAllocated().entrySet().stream()
                                .filter(allocated -> allocated.getValue() > 0)
                                .sorted(Map.Entry.comparingByKey())
                                .map(
                                    allocated ->
                                        new AllocatedRequest(
                                            allocated.getKey(), allocated.getValue()))
                                .toList()))
                .toList(),
            inventory.stream()
                .map(
                    resource ->
                        new ResourceRequest(
                            resource.resourceId().value(),
                            resource.category(),
                            resource.unit(),
                            resource.available()))
                .toList());
    RecommendationResponse response =
        AiServiceCalls.call(
            ENDPOINT,
            () ->
                aiServiceRestClient
                    .post()
                    .uri(ENDPOINT)
                    .body(request)
                    .retrieve()
                    .body(RecommendationResponse.class));
    return new DistributionRecommendation(
        response.allocations().stream()
            .map(
                allocation ->
                    new ZoneAllocation(
                        new ZoneId(allocation.zoneId()),
                        allocation.items().stream()
                            .map(
                                item ->
                                    new ResourceQuantity(
                                        new ResourceId(item.resourceId()), item.quantity()))
                            .toList(),
                        allocation.coverage()))
            .toList(),
        response.justification(),
        response.modelVersion());
  }

  record RecommendationRequest(List<ZoneRequest> zones, List<ResourceRequest> inventory) {}

  record ZoneRequest(
      UUID zoneId,
      double score,
      int peopleAffected,
      List<Need> needs,
      List<AllocatedRequest> alreadyAllocated) {}

  record AllocatedRequest(Need category, int quantity) {}

  record ResourceRequest(UUID resourceId, String category, String unit, int availableQuantity) {}

  record RecommendationResponse(
      List<AllocationResponse> allocations, String justification, String modelVersion) {}

  record AllocationResponse(UUID zoneId, List<ItemResponse> items, Coverage coverage) {}

  record ItemResponse(UUID resourceId, int quantity) {}
}
