package com.waqtalabs.auxia.sinpadsimulator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The field reports of one emergency, as SINPAD takes them: the fields and values are in Spanish.
 *
 * @param codigoExterno identifier of the emergency in the sender system; sending it again
 *     replaces the reports of the same registration
 */
public record SinpadSubmission(
    @NotBlank String codigoExterno,
    @NotNull @Valid Emergencia emergencia,
    @NotEmpty List<@Valid Reporte> reportes) {

  /** The emergency and its location by department, province and district. */
  public record Emergencia(
      @NotBlank String nombre,
      @NotBlank String tipoFenomeno,
      @NotBlank String departamento,
      @NotBlank String provincia,
      @NotBlank String distrito,
      @NotNull LocalDate fechaInicio) {}

  /** A field report. SINPAD requires the zone, the damage level, the people and the families. */
  public record Reporte(
      @NotBlank String zona,
      @NotNull LocalDateTime fechaReporte,
      @NotBlank String fuente,
      @NotBlank String nivelDano,
      @NotNull @PositiveOrZero Integer personasAfectadas,
      @NotNull @PositiveOrZero Integer familiasAfectadas,
      @PositiveOrZero Integer ninos,
      @PositiveOrZero Integer adultosMayores,
      @PositiveOrZero Integer personasConDiscapacidad,
      List<String> necesidades) {}
}
