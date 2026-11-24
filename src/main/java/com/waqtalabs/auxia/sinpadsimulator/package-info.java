/**
 * Simulator of SINPAD, the national system of INDECI for emergency reports. SINPAD has no public
 * API, so the integration of AuxIA is validated against this simulator, which takes a contract
 * like the layout of the SINPAD export (US-33). It is not a bounded context of AuxIA but an
 * external system that runs in the same process; Emergency Management reaches it only by HTTP,
 * so pointing {@code SINPAD_URL} to a real service needs no change in the code.
 */
package com.waqtalabs.auxia.sinpadsimulator;
