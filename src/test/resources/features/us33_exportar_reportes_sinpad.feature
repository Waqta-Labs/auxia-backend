# language: es
Característica: US-33 Exportar reportes en formato compatible con SINPAD
  Como autoridad responsable de atender desastres,
  deseo exportar mis reportes y actas en un formato compatible con SINPAD,
  para no duplicar el registro de la misma información en dos sistemas.

  Antecedentes:
    Dado una emergencia con un reporte de la zona "Sector Alto Perú" con 140 personas y 35 familias afectadas

  Escenario: Exportación exitosa en formato SINPAD
    Cuando la autoridad exporta los reportes de la emergencia en formato SINPAD
    Entonces se genera un archivo CSV con la estructura de campos de SINPAD
    Y el reporte de la zona "Sector Alto Perú" aparece con 140 personas y 35 familias afectadas

  Escenario: Reporte con campos obligatorios incompletos
    Dado otro reporte de la misma zona en el que no se identificaron las familias afectadas
    Cuando la autoridad exporta los reportes de la emergencia en formato SINPAD
    Entonces el sistema indica que ese reporte debe completar "familiesAffected" antes de exportar
