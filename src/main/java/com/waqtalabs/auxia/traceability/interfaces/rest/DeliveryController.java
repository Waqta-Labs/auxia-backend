package com.waqtalabs.auxia.traceability.interfaces.rest;

import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import com.waqtalabs.auxia.traceability.application.commandhandlers.RegisterDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commandhandlers.RegisterDeliveryCommandHandler.RegisteredDelivery;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.DeliveredQuantity;
import com.waqtalabs.auxia.traceability.application.commands.RegisterDeliveryCommand.EvidenceFile;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveriesByZoneIdQuery;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryByIdQuery;
import com.waqtalabs.auxia.traceability.application.queries.GetDeliveryReportQuery;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryQueryService;
import com.waqtalabs.auxia.traceability.application.queryservices.DeliveryReportQueryService;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.GeoPoint;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.traceability.interfaces.rest.assemblers.DeliveryResourceAssembler;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.DeliveryResource;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.RegisterDeliveryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Deliveries of the organization of the signed-in user. A delivery opens when a brigade is
 * assigned to an approved plan; the brigade registers it from the field app with its evidence.
 * A delivery of another organization is not found.
 */
@RestController
@RequestMapping("/api/v1/deliveries")
@Tag(
    name = "Deliveries",
    description = "Registration with evidence and its anchoring in the blockchain (US-15, US-16)")
public class DeliveryController {

  private final RegisterDeliveryCommandHandler registerDelivery;
  private final DeliveryQueryService deliveryQueries;
  private final DeliveryReportQueryService deliveryReports;

  public DeliveryController(
      RegisterDeliveryCommandHandler registerDelivery,
      DeliveryQueryService deliveryQueries,
      DeliveryReportQueryService deliveryReports) {
    this.registerDelivery = registerDelivery;
    this.deliveryQueries = deliveryQueries;
    this.deliveryReports = deliveryReports;
  }

  @GetMapping
  @Operation(summary = "List the deliveries of a zone", description = "From the most recent.")
  @ApiResponse(responseCode = "200", description = "Deliveries of the organization")
  public List<DeliveryResource> byZone(
      @RequestParam UUID zoneId, @AuthenticationPrincipal AuthenticatedUser user) {
    return deliveryQueries
        .handle(new GetDeliveriesByZoneIdQuery(new ZoneId(zoneId), organizationOf(user)))
        .stream()
        .map(DeliveryResourceAssembler::toResource)
        .toList();
  }

  @GetMapping("/report")
  @Operation(
      summary = "Export the history of deliveries as PDF",
      description =
          "Deliveries of the organization that happened, of a zone, a period or both, from the"
              + " oldest: date, resources, record hash, transaction and verification status, to"
              + " support the aid in an audit (US-19). Both days of the period are included. The"
              + " document is in the language of Accept-Language.")
  @ApiResponse(responseCode = "200", description = "PDF of the deliveries")
  @ApiResponse(responseCode = "404", description = "No delivery in the zone or period")
  @ApiResponse(responseCode = "422", description = "The period ends before it starts")
  public ResponseEntity<byte[]> exportReport(
      @Parameter(description = "Only the deliveries of this zone") @RequestParam(required = false)
          UUID zoneId,
      @Parameter(description = "First day, such as 2026-03-15")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate from,
      @Parameter(description = "Last day, included")
          @RequestParam(required = false)
          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate to,
      @AuthenticationPrincipal AuthenticatedUser user) {
    byte[] pdf =
        deliveryReports.handle(
            new GetDeliveryReportQuery(
                organizationOf(user),
                zoneId == null ? null : new ZoneId(zoneId),
                from,
                to,
                LocaleContextHolder.getLocale()));
    var disposition = ContentDisposition.attachment().filename("deliveries.pdf").build();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(pdf);
  }

  @GetMapping("/{deliveryId}")
  @Operation(summary = "Get a delivery with its evidence and its anchoring")
  @ApiResponse(responseCode = "200", description = "Delivery found")
  @ApiResponse(responseCode = "404", description = "Not a delivery of the organization")
  public DeliveryResource byId(
      @PathVariable UUID deliveryId, @AuthenticationPrincipal AuthenticatedUser user) {
    return DeliveryResourceAssembler.toResource(
        deliveryQueries.handle(
            new GetDeliveryByIdQuery(new DeliveryId(deliveryId), organizationOf(user))));
  }

  @PreAuthorize("hasAnyRole('FIELD_BRIGADE', 'AUTHORITY')")
  @PostMapping(path = "/{deliveryId}/registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(
      summary = "Register a delivery with its evidence",
      description =
          "A multipart request: the `delivery` part is JSON, and each `evidences` part is a photo"
              + " (JPEG, PNG, WebP or HEIC) or a PDF. The response carries the SHA-256 of the"
              + " record, which has no personal data; its transaction appears once it is sent to"
              + " the blockchain, right after the response. Sending it again returns the delivery"
              + " as it is (200), so the field app can retry after losing the signal.")
  @ApiResponse(responseCode = "201", description = "Delivery registered and its hash computed")
  @ApiResponse(responseCode = "200", description = "The delivery was already registered")
  @ApiResponse(
      responseCode = "400",
      description = "The evidence or the delivery part is missing, or a field is not valid")
  @ApiResponse(responseCode = "404", description = "Not a delivery of the organization")
  @ApiResponse(
      responseCode = "422",
      description =
          "An evidence is empty or of another type, a resource is repeated, not in the plan or"
              + " above its quantity, or the delivery date is in the future")
  @ApiResponse(responseCode = "503", description = "The evidence storage is not available")
  public ResponseEntity<DeliveryResource> register(
      @PathVariable UUID deliveryId,
      @Valid @RequestPart("delivery") RegisterDeliveryRequest request,
      @Parameter(description = "Photos or documents of the delivery; at least one")
          @RequestPart("evidences")
          List<MultipartFile> evidences,
      @AuthenticationPrincipal AuthenticatedUser user) {
    var command =
        new RegisterDeliveryCommand(
            new DeliveryId(deliveryId),
            organizationOf(user),
            new UserId(user.userId()),
            request.deliveredAt(),
            request.items().stream()
                .map(
                    item ->
                        new DeliveredQuantity(new ResourceId(item.resourceId()), item.quantity()))
                .toList(),
            Optional.ofNullable(request.location())
                .map(location -> new GeoPoint(location.latitude(), location.longitude())),
            evidences.stream().map(DeliveryController::toEvidenceFile).toList());
    RegisteredDelivery registered = registerDelivery.handle(command);
    return ResponseEntity.status(registered.registeredNow() ? HttpStatus.CREATED : HttpStatus.OK)
        .body(DeliveryResourceAssembler.toResource(registered.delivery()));
  }

  private static EvidenceFile toEvidenceFile(MultipartFile file) {
    try {
      return new EvidenceFile(file.getContentType(), file.getBytes());
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  private static OrganizationId organizationOf(AuthenticatedUser user) {
    return new OrganizationId(user.organizationId());
  }
}
