# language: es
Característica: US-19 Exportar el historial de entregas de una zona o periodo
  Como autoridad responsable de atender desastres,
  quiero exportar el historial de entregas de una zona o periodo determinado,
  para sustentar la ayuda entregada ante una auditoría.

  Antecedentes:
    Dado una entrega de 120 unidades de agua registrada el "2026-03-15" en la zona "Sector Alto Perú"

  Escenario: Exportación exitosa del historial de entregas
    Cuando la autoridad exporta el historial de entregas de la zona del "2026-03-15" al "2026-03-15"
    Entonces se genera un PDF con la entrega, sus recursos y su estado de verificación

  Escenario: Ausencia de resultados para el periodo solicitado
    Cuando la autoridad exporta el historial de entregas de la zona del "2026-04-01" al "2026-04-30"
    Entonces el sistema indica que no hay entregas para el periodo solicitado
