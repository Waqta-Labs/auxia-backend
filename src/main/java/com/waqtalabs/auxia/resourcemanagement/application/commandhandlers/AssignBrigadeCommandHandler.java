package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AssignBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.BrigadeAssigned;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DistributionPlanAlreadyAssignedException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import com.waqtalabs.auxia.resourcemanagement.domain.services.StaffAssignmentService;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assigns the most suitable brigade to an approved distribution plan and announces it, so
 * Traceability opens the delivery.
 *
 * <p>The candidates are the available brigades of the organization that have a leader, ordered
 * by name. The AI Service suggests the closest one to the zone; a zone without coordinates gets
 * the first candidate, because there is no distance to compare.
 */
@Service
public class AssignBrigadeCommandHandler {

  private final BrigadeRepository brigadeRepository;
  private final StaffAssignmentService staffAssignment;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public AssignBrigadeCommandHandler(
      BrigadeRepository brigadeRepository,
      StaffAssignmentService staffAssignment,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.brigadeRepository = brigadeRepository;
    this.staffAssignment = staffAssignment;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  /**
   * The assigned brigade, or empty when no brigade of the organization can take the plan now.
   *
   * @throws DistributionPlanAlreadyAssignedException if a brigade already has the plan
   */
  @Transactional
  public Optional<Brigade> handle(AssignBrigadeCommand command) {
    brigadeRepository
        .findByAssignedPlan(command.distributionPlanId())
        .ifPresent(
            brigade -> {
              throw new DistributionPlanAlreadyAssignedException(
                  command.distributionPlanId(), brigade.id());
            });
    List<Brigade> candidates =
        brigadeRepository.findByOrganizationId(command.organizationId()).stream()
            .filter(brigade -> brigade.isAvailable() && brigade.hasLeader())
            .toList();
    if (candidates.isEmpty()) {
      return Optional.empty();
    }
    BrigadeId chosen =
        command
            .zoneLocation()
            .map(location -> staffAssignment.suggestBrigade(candidates, location))
            .orElse(candidates.getFirst().id());
    Brigade brigade =
        candidates.stream()
            .filter(candidate -> candidate.id().equals(chosen))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Suggested brigade is not a candidate"));
    LocalDateTime assignedAt = LocalDateTime.now(clock);
    brigade.assignTo(command.distributionPlanId(), command.zoneId(), assignedAt);
    Brigade saved = brigadeRepository.save(brigade);
    eventPublisher.publish(
        new BrigadeAssigned(
            saved.id(),
            command.distributionPlanId(),
            command.zoneId(),
            command.organizationId(),
            assignedAt));
    return Optional.of(saved);
  }
}
