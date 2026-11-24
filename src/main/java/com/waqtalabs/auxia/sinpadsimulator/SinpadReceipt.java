package com.waqtalabs.auxia.sinpadsimulator;

import java.time.LocalDateTime;

/**
 * What SINPAD answers when it receives the reports of an emergency.
 *
 * @param codigoRegistro registration number, the same every time the emergency is sent again
 */
public record SinpadReceipt(
    String codigoRegistro,
    String codigoExterno,
    String estado,
    int reportesRecibidos,
    LocalDateTime recibidoEn) {}
