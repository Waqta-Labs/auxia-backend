package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportSosQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.SosDetailsResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Field reports")
public class FieldReportSosController {

  private final CitizenReportQueryService citizenReports;

  public FieldReportSosController(CitizenReportQueryService citizenReports) {
    this.citizenReports = citizenReports;
  }

  @GetMapping("/api/v1/field-reports/{fieldReportId}/sos")
  @Operation(
      summary = "What the citizen marked in the SOS of a field report",
      description =
          "Vulnerable people, access conditions, category and location that the citizen sent"
              + " besides the text (A_04).")
  @ApiResponse(responseCode = "200", description = "The SOS")
  @ApiResponse(
      responseCode = "404",
      description = "The report does not exist or does not come from an SOS")
  public SosDetailsResource find(@PathVariable UUID fieldReportId) {
    return SosDetailsResource.from(
        citizenReports.handle(new GetFieldReportSosQuery(new FieldReportId(fieldReportId))));
  }
}
