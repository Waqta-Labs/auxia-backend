# language: es
Característica: US-03 Corregir manualmente un reporte estructurado
  Como autoridad responsable de atender desastres,
  quiero corregir o completar manualmente las variables extraídas de un reporte,
  para asegurar que la información usada en la priorización sea precisa.

  Antecedentes:
    Dado un reporte estructurado de la zona "Sector Alto Perú" con daño "MODERATE" y 60 personas afectadas

  Escenario: Conservación del valor corregido manualmente
    Cuando la autoridad corrige las personas afectadas a 300 porque "Conteo en campo de la brigada"
    Entonces el reporte usa 300 personas afectadas
    Y el score de la zona se calcula con el valor corregido

  Escenario: Trazabilidad entre valor original y valor corregido
    Cuando la autoridad corrige las personas afectadas a 300 porque "Conteo en campo de la brigada"
    Y se consulta el reporte
    Entonces se distingue el valor original 60 del valor corregido 300, con la autoridad y el motivo
