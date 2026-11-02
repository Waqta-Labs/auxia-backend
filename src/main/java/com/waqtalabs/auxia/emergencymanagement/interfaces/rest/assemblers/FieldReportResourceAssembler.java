package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.FieldReportCorrectionResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.FieldReportResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterEmergencyFieldReportRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterFieldReportRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.StructuredFieldDataResource;
import java.util.UUID;

public final class FieldReportResourceAssembler {

  private FieldReportResourceAssembler() {}

  public static RegisterFieldReportCommand toCommand(
      UUID zoneId, RegisterFieldReportRequest request) {
    return RegisterFieldReportCommand.forZone(
        new ZoneId(zoneId), request.text(), request.source());
  }

  public static RegisterFieldReportCommand toCommand(
      UUID emergencyId, RegisterEmergencyFieldReportRequest request) {
    return new RegisterFieldReportCommand(
        new EmergencyId(emergencyId),
        request.zoneId() == null ? null : new ZoneId(request.zoneId()),
        request.text(),
        request.source());
  }

  public static FieldReportResource toResource(FieldReport report) {
    return new FieldReportResource(
        report.id().value(),
        report.emergencyId().value(),
        report.zoneId().map(ZoneId::value).orElse(null),
        report.rawText(),
        report.source(),
        report.reportedAt(),
        report.isPendingZoneAssignment(),
        report.isProcessed(),
        report.structuredData().map(FieldReportResourceAssembler::toResource).orElse(null),
        report
            .correction()
            .map(
                correction ->
                    new FieldReportCorrectionResource(
                        toResource(report.originalData().orElseThrow()),
                        correction.correctedBy().value(),
                        correction.correctedAt(),
                        correction.reason()))
            .orElse(null));
  }

  private static StructuredFieldDataResource toResource(StructuredFieldData data) {
    return new StructuredFieldDataResource(
        data.damageLevel(),
        data.familiesAffected(),
        data.peopleAffected(),
        data.children(),
        data.elderlyPeople(),
        data.disabledPeople(),
        data.needs(),
        data.missingVariables());
  }
}
