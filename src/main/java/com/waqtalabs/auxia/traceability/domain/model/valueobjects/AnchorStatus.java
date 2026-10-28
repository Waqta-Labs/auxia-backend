package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

/**
 * State of the anchoring of the hash of a delivery in the blockchain.
 */
public enum AnchorStatus {
  PENDING,
  ANCHORED,
  FAILED
}
