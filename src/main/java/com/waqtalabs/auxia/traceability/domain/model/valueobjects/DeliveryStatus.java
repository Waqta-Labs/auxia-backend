package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

/**
 * Life cycle of a delivery: opened when a brigade is assigned, registered with its evidence, and
 * certified once its hash is anchored in the blockchain.
 */
public enum DeliveryStatus {
  INITIATED,
  REGISTERED,
  CERTIFIED
}
