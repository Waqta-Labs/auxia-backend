package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AttachToCitizenReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.AttachToCitizenReportCommandHandler.Attachment;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RegisterCitizenReportCommandHandler.Registration;
import com.waqtalabs.auxia.emergencymanagement.application.commands.AttachToCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetCitizenReportProgressQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.CitizenReportQueryService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers.CitizenReportResourceAssembler;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.CitizenReportStatusResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterCitizenReportRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.SosAttachmentResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * SOS of citizens from the mobile app, without signing in. The app keeps every SOS in a queue
 * until the API confirms it, and asks for its progress with the identifier it generated.
 */
@RestController
@RequestMapping("/api/v1/public/field-reports")
@SecurityRequirements
@Tag(name = "Citizen SOS", description = "SOS of citizens from the mobile app, without signing in")
public class CitizenReportController {

  private final RegisterCitizenReportCommandHandler registerCitizenReport;
  private final AttachToCitizenReportCommandHandler attachToCitizenReport;
  private final CitizenReportQueryService citizenReportQueries;

  public CitizenReportController(
      RegisterCitizenReportCommandHandler registerCitizenReport,
      AttachToCitizenReportCommandHandler attachToCitizenReport,
      CitizenReportQueryService citizenReportQueries) {
    this.registerCitizenReport = registerCitizenReport;
    this.attachToCitizenReport = attachToCitizenReport;
    this.citizenReportQueries = citizenReportQueries;
  }

  @PostMapping
  @Operation(
      summary = "Send an SOS",
      description =
          "Registers the SOS as a field report of source CITIZEN, pending review, in the active"
              + " emergency and the zone nearest to its location. Sending the same clientReportId"
              + " again returns the stored SOS. Each address can send 10 new SOS every 10"
              + " minutes; retries do not count.")
  @ApiResponse(responseCode = "201", description = "SOS stored")
  @ApiResponse(responseCode = "200", description = "The SOS was already stored")
  @ApiResponse(responseCode = "400", description = "Invalid SOS")
  @ApiResponse(responseCode = "429", description = "Too many SOS; see the Retry-After header")
  public ResponseEntity<CitizenReportStatusResource> send(
      @Valid @RequestBody RegisterCitizenReportRequest request, HttpServletRequest http) {
    RegisterCitizenReportCommand command =
        CitizenReportResourceAssembler.toCommand(request, ClientAddress.of(http));
    Registration registration;
    try {
      registration = registerCitizenReport.handle(command);
    } catch (DataIntegrityViolationException sameSosStoredMeanwhile) {
      // Two copies of the SOS arrived at the same time and the other one was stored first.
      registration = registerCitizenReport.handle(command);
    }
    CitizenReportStatusResource status = statusOf(registration.report().id());
    if (!registration.created()) {
      return ResponseEntity.ok(status);
    }
    return ResponseEntity.created(
            URI.create("/api/v1/public/field-reports/" + registration.report().id().value()))
        .body(status);
  }

  @GetMapping("/{clientReportId}")
  @Operation(
      summary = "Progress of an SOS",
      description =
          "RECEIVED while it has no zone or its zone has no score yet, PRIORITIZED once its zone"
              + " has a score or a manual priority, and BRIGADE_ON_THE_WAY once a brigade took an"
              + " approved plan of its zone, with the score of the zone and the ETA of the"
              + " brigade.")
  @ApiResponse(responseCode = "200", description = "SOS found")
  @ApiResponse(responseCode = "404", description = "No SOS has that identifier")
  public CitizenReportStatusResource status(@PathVariable UUID clientReportId) {
    return statusOf(new CitizenReportId(clientReportId));
  }

  @PutMapping(
      path = "/{clientReportId}/attachments/{attachmentId}",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Attach the photo or the audio of an SOS",
      description =
          "The phone generates attachmentId, so sending it again returns the stored file. An SOS"
              + " has one photo (JPEG, PNG, WEBP or HEIC, up to 10 MB) and one audio (M4A, AAC,"
              + " MP3, OGG or WAV, up to 5 MB). Only the authority sees them.")
  @ApiResponse(responseCode = "201", description = "File stored")
  @ApiResponse(responseCode = "200", description = "The file was already stored")
  @ApiResponse(responseCode = "404", description = "The SOS has not arrived yet")
  @ApiResponse(responseCode = "422", description = "File not admitted, or the SOS has one")
  @ApiResponse(responseCode = "429", description = "Too many requests; see Retry-After")
  public ResponseEntity<SosAttachmentResource> attach(
      @PathVariable UUID clientReportId,
      @PathVariable UUID attachmentId,
      @RequestPart("file") MultipartFile file,
      HttpServletRequest http)
      throws IOException {
    var command =
        new AttachToCitizenReportCommand(
            new CitizenReportId(clientReportId),
            new SosAttachmentId(attachmentId),
            file.getContentType(),
            file.getBytes(),
            ClientAddress.of(http));
    Attachment attachment;
    try {
      attachment = attachToCitizenReport.handle(command);
    } catch (DataIntegrityViolationException sameFileStoredMeanwhile) {
      attachment = attachToCitizenReport.handle(command);
    }
    var body = SosAttachmentResource.from(attachment.attachment());
    return attachment.created()
        ? ResponseEntity.status(HttpStatus.CREATED).body(body)
        : ResponseEntity.ok(body);
  }

  private CitizenReportStatusResource statusOf(CitizenReportId id) {
    return CitizenReportResourceAssembler.toResource(
        citizenReportQueries.handle(new GetCitizenReportProgressQuery(id)));
  }
}
