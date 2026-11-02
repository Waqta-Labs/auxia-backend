package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.CorrectFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportCorrected;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotStructuredException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrects the indicators of a report by hand (US-03). The corrected values are used in the next
 * score of its zone, which is calculated again after the commit.
 */
@Service
public class CorrectFieldReportCommandHandler {

  private final FieldReportRepository fieldReportRepository;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public CorrectFieldReportCommandHandler(
      FieldReportRepository fieldReportRepository,
      AuthorizationPort authorization,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.fieldReportRepository = fieldReportRepository;
    this.authorization = authorization;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public FieldReport handle(CorrectFieldReportCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    FieldReport report =
        fieldReportRepository
            .findById(command.fieldReportId())
            .orElseThrow(() -> new FieldReportNotFoundException(command.fieldReportId()));
    StructuredFieldData current =
        report
            .structuredData()
            .orElseThrow(() -> new FieldReportNotStructuredException(report.id()));
    LocalDateTime now = LocalDateTime.now(clock);
    report.correct(corrected(current, command), command.authorityId(), now, command.reason());
    FieldReport saved = fieldReportRepository.save(report);
    eventPublisher.publish(
        new FieldReportCorrected(
            saved.id(),
            saved.zoneId().orElse(null),
            command.authorityId(),
            saved.correction().orElseThrow().reason(),
            now));
    return saved;
  }

  private static <T> T given(T value, T current) {
    return value != null ? value : current;
  }

  /** The current indicators with the given ones on top; a filled indicator is no longer missing. */
  private static StructuredFieldData corrected(
      StructuredFieldData current, CorrectFieldReportCommand command) {
    var data =
        new StructuredFieldData(
            given(command.damageLevel(), current.damageLevel()),
            given(command.familiesAffected(), current.familiesAffected()),
            given(command.peopleAffected(), current.peopleAffected()),
            given(command.children(), current.children()),
            given(command.elderlyPeople(), current.elderlyPeople()),
            given(command.disabledPeople(), current.disabledPeople()),
            given(command.needs(), current.needs()),
            current.missingVariables());
    Map<String, Object> values = new HashMap<>();
    values.put("damageLevel", data.damageLevel());
    values.put("familiesAffected", data.familiesAffected());
    values.put("peopleAffected", data.peopleAffected());
    values.put("children", data.children());
    values.put("elderlyPeople", data.elderlyPeople());
    values.put("disabledPeople", data.disabledPeople());
    values.put("needs", data.needs().isEmpty() ? null : data.needs());
    return new StructuredFieldData(
        data.damageLevel(),
        data.familiesAffected(),
        data.peopleAffected(),
        data.children(),
        data.elderlyPeople(),
        data.disabledPeople(),
        data.needs(),
        current.missingVariables().stream()
            .filter(variable -> values.get(variable) == null)
            .toList());
  }
}
