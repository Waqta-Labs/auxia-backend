package com.waqtalabs.auxia.traceability.interfaces.rest;

import com.waqtalabs.auxia.traceability.application.queries.VerifyDeliveryQuery;
import com.waqtalabs.auxia.traceability.application.queryservices.VerifyDeliveryQueryHandler;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.interfaces.rest.assemblers.DeliveryResourceAssembler;
import com.waqtalabs.auxia.traceability.interfaces.rest.resources.VerificationResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public verification of a delivery (US-17): a citizen can check it too, without signing in. The
 * identifier of the delivery works as its public code (US-26).
 */
@RestController
@Tag(name = "Delivery verification", description = "Integrity against the blockchain (US-17)")
public class DeliveryVerificationController {

  private final VerifyDeliveryQueryHandler verifyDelivery;

  public DeliveryVerificationController(VerifyDeliveryQueryHandler verifyDelivery) {
    this.verifyDelivery = verifyDelivery;
  }

  @GetMapping("/api/v1/deliveries/{deliveryId}/verify")
  @SecurityRequirements
  @Operation(
      summary = "Verify that a delivery was not altered",
      description =
          "Computes the hash again from the quantities stored now and from the evidence files,"
              + " and compares it with the hash in the smart contract. No sign-in needed.")
  @ApiResponse(responseCode = "200", description = "Hashes compared; see `verified`")
  @ApiResponse(responseCode = "404", description = "The code is not of a registered delivery")
  @ApiResponse(responseCode = "503", description = "The blockchain or the storage did not answer")
  public VerificationResource verify(@PathVariable UUID deliveryId) {
    return DeliveryResourceAssembler.toResource(
        verifyDelivery.handle(new VerifyDeliveryQuery(new DeliveryId(deliveryId))));
  }
}
