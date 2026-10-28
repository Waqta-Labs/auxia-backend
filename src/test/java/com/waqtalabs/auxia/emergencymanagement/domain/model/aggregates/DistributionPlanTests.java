package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanAlreadyApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotPendingException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DuplicateDistributionResourceException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmptyDistributionPlanException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.JustificationRequiredException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DistributionPlanTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);

  private final ResourceId water = new ResourceId(UUID.randomUUID());
  private final ResourceId food = new ResourceId(UUID.randomUUID());
  private final ResourceId tents = new ResourceId(UUID.randomUUID());
  private final AuthorityId authority = new AuthorityId(UUID.randomUUID());
  private final OrganizationId organization = new OrganizationId(UUID.randomUUID());
  private final DistributionPlan plan = propose(new ResourceQuantity(water, 329));

  @Test
  void proposedPlanKeepsTheRecommendationAndWaitsForADecision() {
    assertThat(plan.status()).isEqualTo(DistributionPlanStatus.PROPOSED);
    assertThat(plan.isPending()).isTrue();
    assertThat(plan.coverage()).isEqualTo(Coverage.PARTIAL);
    assertThat(plan.justification()).isEqualTo("Proporcional al score");
    assertThat(plan.modelVersion()).isEqualTo("rules-v1");
    assertThat(plan.createdAt()).isEqualTo(NOW);
    assertThat(plan.belongsTo(organization)).isTrue();
    assertThat(plan.items())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.recommendedQuantity()).contains(329);
              assertThat(item.quantity()).isEqualTo(329);
            });
    assertThat(plan.distributedQuantities()).containsExactly(new ResourceQuantity(water, 329));
    assertThat(plan.modification()).isEmpty();
    assertThat(plan.decision()).isEmpty();
  }

  @Test
  void zoneWithoutCoverageOrWithoutResourcesGetsNoPlan() {
    var items = List.of(new ResourceQuantity(water, 10));

    assertThatThrownBy(() -> propose(Coverage.NONE, items))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> propose(Coverage.FULL, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> propose(new ResourceQuantity(water, 0)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> propose(new ResourceQuantity(water, 1), new ResourceQuantity(water, 2)))
        .isInstanceOf(DuplicateDistributionResourceException.class);
  }

  @Test
  void approvedPlanRecordsTheAuthorityAndTheDate() {
    plan.approve(authority, NOW.plusHours(1));

    assertThat(plan.status()).isEqualTo(DistributionPlanStatus.APPROVED);
    assertThat(plan.isApproved()).isTrue();
    assertThat(plan.decision()).contains(new PlanDecision(authority, NOW.plusHours(1)));
    assertThat(plan.rejectedReason()).isEmpty();
  }

  @Test
  void approvedPlanCannotBeApprovedAgainNorRejected() {
    plan.approve(authority, NOW);

    assertThatThrownBy(() -> plan.approve(authority, NOW))
        .isInstanceOf(DistributionPlanAlreadyApprovedException.class);
    assertThatThrownBy(() -> plan.reject(authority, "Ya no hace falta", NOW))
        .isInstanceOf(DistributionPlanNotPendingException.class);
    assertThatThrownBy(
            () -> plan.modifyItems(List.of(new ResourceQuantity(water, 1)), authority, "x", NOW))
        .isInstanceOf(DistributionPlanNotPendingException.class);
  }

  @Test
  void rejectedPlanKeepsItsReasonAndCannotBeApproved() {
    plan.reject(authority, "  La vía está bloqueada  ", NOW);

    assertThat(plan.status()).isEqualTo(DistributionPlanStatus.REJECTED);
    assertThat(plan.rejectedReason()).contains("La vía está bloqueada");
    assertThat(plan.decision()).contains(new PlanDecision(authority, NOW));
    assertThatThrownBy(() -> plan.approve(authority, NOW))
        .isInstanceOf(DistributionPlanNotPendingException.class);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "   "})
  void rejectionAndModificationNeedAJustification(String reason) {
    var changes = List.of(new ResourceQuantity(water, 100));

    assertThatThrownBy(() -> plan.reject(authority, reason, NOW))
        .isInstanceOf(JustificationRequiredException.class);
    assertThatThrownBy(() -> plan.modifyItems(changes, authority, reason, NOW))
        .isInstanceOf(JustificationRequiredException.class);
    assertThat(plan.isPending()).isTrue();
    assertThat(plan.distributedQuantities()).containsExactly(new ResourceQuantity(water, 329));
  }

  @Test
  void modificationKeepsTheRecommendedQuantitiesAndWhoChangedThem() {
    DistributionPlan withFood =
        propose(new ResourceQuantity(water, 329), new ResourceQuantity(food, 140));

    withFood.modifyItems(
        List.of(new ResourceQuantity(water, 200), new ResourceQuantity(tents, 10)),
        authority,
        "La zona recibió agua de otra ONG",
        NOW.plusMinutes(30));

    assertThat(withFood.items())
        .extracting(
            DistributionItem::resourceId,
            item -> item.recommendedQuantity().orElse(null),
            DistributionItem::quantity)
        .containsExactlyInAnyOrder(
            tuple(water, 329, 200),
            tuple(food, 140, 140),
            tuple(tents, null, 10));
    assertThat(withFood.modification())
        .hasValueSatisfying(
            modification -> {
              assertThat(modification.modifiedBy()).isEqualTo(authority);
              assertThat(modification.modifiedAt()).isEqualTo(NOW.plusMinutes(30));
              assertThat(modification.reason()).isEqualTo("La zona recibió agua de otra ONG");
            });
    assertThat(withFood.isPending()).isTrue();
  }

  @Test
  void quantityOfZeroTakesAResourceOutWithoutLosingTheRecommendation() {
    DistributionPlan withFood =
        propose(new ResourceQuantity(water, 329), new ResourceQuantity(food, 140));
    withFood.modifyItems(List.of(new ResourceQuantity(tents, 5)), authority, "Carpas", NOW);

    withFood.modifyItems(
        List.of(new ResourceQuantity(food, 0), new ResourceQuantity(tents, 0)),
        authority,
        "Solo agua",
        NOW);

    assertThat(withFood.distributedQuantities()).containsExactly(new ResourceQuantity(water, 329));
    assertThat(withFood.items())
        .extracting(DistributionItem::resourceId)
        .containsExactlyInAnyOrder(water, food);
    assertThat(withFood.modification().orElseThrow().reason()).isEqualTo("Solo agua");
  }

  @Test
  void modificationCannotEmptyThePlanNorRepeatAResourceAndThenChangesNothing() {
    assertThatThrownBy(
            () ->
                plan.modifyItems(
                    List.of(new ResourceQuantity(water, 0)), authority, "Sin recursos", NOW))
        .isInstanceOf(EmptyDistributionPlanException.class);
    assertThatThrownBy(
            () ->
                plan.modifyItems(
                    List.of(new ResourceQuantity(water, 5), new ResourceQuantity(water, 6)),
                    authority,
                    "Repetido",
                    NOW))
        .isInstanceOf(DuplicateDistributionResourceException.class);

    assertThat(plan.distributedQuantities()).containsExactly(new ResourceQuantity(water, 329));
    assertThat(plan.modification()).isEmpty();
  }

  @Test
  void onlyAnApprovedPlanIsExecuted() {
    assertThatThrownBy(plan::markExecuted).isInstanceOf(DistributionPlanNotApprovedException.class);

    plan.approve(authority, NOW);
    plan.markExecuted();

    assertThat(plan.status()).isEqualTo(DistributionPlanStatus.EXECUTED);
    assertThat(plan.decision()).isPresent();
  }

  private DistributionPlan propose(ResourceQuantity... items) {
    return propose(Coverage.PARTIAL, List.of(items));
  }

  private DistributionPlan propose(Coverage coverage, List<ResourceQuantity> items) {
    return DistributionPlan.propose(
        new EmergencyId(UUID.randomUUID()),
        new ZoneId(UUID.randomUUID()),
        organization,
        items,
        coverage,
        "Proporcional al score",
        "rules-v1",
        NOW);
  }
}
