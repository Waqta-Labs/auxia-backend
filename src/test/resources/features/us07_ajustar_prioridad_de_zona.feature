# language: es
Característica: US-07 Ajustar manualmente la prioridad de una zona
  Como autoridad responsable de atender desastres,
  quiero marcar manualmente una zona como prioritaria,
  para atender casos que el score no captura completamente (por ejemplo, riesgo inminente).

  Antecedentes:
    Dado un reporte estructurado de la zona "Sector Alto Perú" con daño "CRITICAL" y 300 personas afectadas
    Y un reporte estructurado de la zona "Quebrada Pedregal" con daño "LOW" y 10 personas afectadas

  Escenario: Registro de una prioridad manual con justificación
    Cuando la autoridad marca como prioritaria la zona "Quebrada Pedregal" porque "Riesgo inminente de un nuevo huaico"
    Entonces la zona "Quebrada Pedregal" registra la prioridad manual con la justificación y la autoridad

  Escenario: Distinción visual entre priorización manual y automática
    Cuando la autoridad marca como prioritaria la zona "Quebrada Pedregal" porque "Riesgo inminente de un nuevo huaico"
    Y se consulta el listado de zonas
    Entonces la zona "Quebrada Pedregal" aparece primero marcada como prioridad manual
    Y la zona "Sector Alto Perú" aparece priorizada solo por su score
