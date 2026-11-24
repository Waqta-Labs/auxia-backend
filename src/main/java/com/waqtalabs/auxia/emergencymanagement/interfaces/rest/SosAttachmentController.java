package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportAttachmentsQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SosAttachmentQueryService;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.SosAttachmentQueryService.SosAttachmentFile;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosAttachmentId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.SosAttachmentResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The photo and audio a citizen sent with an SOS, for whoever reviews its field report. They are
 * never public (TS-C02).
 */
@RestController
@RequestMapping("/api/v1/field-reports/{fieldReportId}/attachments")
@Tag(name = "Field reports")
public class SosAttachmentController {

  private final SosAttachmentQueryService attachments;

  public SosAttachmentController(SosAttachmentQueryService attachments) {
    this.attachments = attachments;
  }

  @GetMapping
  @Operation(
      summary = "Photo and audio of the SOS of a field report",
      description = "Empty when the report does not come from the SOS of a citizen.")
  @ApiResponse(responseCode = "200", description = "Attachments, the photo first")
  @ApiResponse(responseCode = "404", description = "The report does not exist")
  public List<SosAttachmentResource> list(@PathVariable UUID fieldReportId) {
    return attachments
        .attachments(new GetFieldReportAttachmentsQuery(new FieldReportId(fieldReportId), null))
        .stream()
        .map(SosAttachmentResource::from)
        .toList();
  }

  @GetMapping("/{attachmentId}")
  @Operation(summary = "Download the photo or the audio of the SOS of a field report")
  @ApiResponse(responseCode = "200", description = "The file, with its media type")
  @ApiResponse(responseCode = "404", description = "The report has no such attachment")
  public ResponseEntity<byte[]> download(
      @PathVariable UUID fieldReportId, @PathVariable UUID attachmentId) {
    SosAttachmentFile file =
        attachments.file(
            new GetFieldReportAttachmentsQuery(
                new FieldReportId(fieldReportId), new SosAttachmentId(attachmentId)));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.attachment().contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(attachmentId.toString()).build().toString())
        .body(file.content());
  }
}
