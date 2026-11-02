package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.CompleteBrigadeAssignmentCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.AssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.PlanAssignmentNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Closes the assignment of a delivered plan and makes its brigade available again. */
@Service
public class CompleteBrigadeAssignmentCommandHandler {

  private final BrigadeRepository brigadeRepository;
  private final Clock clock;

  public CompleteBrigadeAssignmentCommandHandler(BrigadeRepository brigadeRepository, Clock clock) {
    this.brigadeRepository = brigadeRepository;
    this.clock = clock;
  }

  /**
   * @throws PlanAssignmentNotFoundException if no brigade was assigned to the plan
   * @throws AssignmentNotFoundException if its assignment is already completed
   */
  @Transactional
  public Brigade handle(CompleteBrigadeAssignmentCommand command) {
    Brigade brigade =
        brigadeRepository
            .findByAssignedPlan(command.distributionPlanId())
            .orElseThrow(() -> new PlanAssignmentNotFoundException(command.distributionPlanId()));
    brigade.completeAssignment(command.distributionPlanId(), LocalDateTime.now(clock));
    return brigadeRepository.save(brigade);
  }
}
