package com.waqtalabs.auxia.emergencymanagement.domain.exceptions;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.shared.domain.exceptions.RuleViolationException;
import java.util.List;
import java.util.Map;

/**
 * Some field reports lack a field SINPAD requires, so nothing is exported until they are completed
 * (US-33, scenario 2). The problem detail lists each report with its missing fields.
 */
public class IncompleteSinpadReportsException extends RuleViolationException {

  private final transient List<IncompleteReport> reports;

  public IncompleteSinpadReportsException(List<IncompleteReport> reports) {
    super(reports.size() + " field reports lack fields required by SINPAD", reports.size());
    this.reports = List.copyOf(reports);
  }

  public List<IncompleteReport> reports() {
    return reports;
  }

  @Override
  public Map<String, Object> properties() {
    return Map.of(
        "incompleteReports",
        reports.stream()
            .map(
                report ->
                    Map.of(
                        "fieldReportId", report.fieldReportId().value(),
                        "missingFields", report.missingFields()))
            .toList());
  }

  /**
   * A field report and the SINPAD fields it lacks.
   *
   * @param missingFields such as {@code zone} or {@code peopleAffected}
   */
  public record IncompleteReport(FieldReportId fieldReportId, List<String> missingFields) {

    public IncompleteReport {
      missingFields = List.copyOf(missingFields);
    }
  }
}
