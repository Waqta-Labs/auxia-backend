# language: es
Característica: US-02 Extraer automáticamente variables clave de un reporte
  Como autoridad responsable de atender desastres,
  quiero que el sistema extraiga automáticamente variables clave del reporte (daños, población afectada, necesidades),
  para comparar zonas sin leer manualmente cada reporte.

  Antecedentes:
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú"

  Escenario: Extracción exitosa de variables estructuradas
    Dado que el NLP identifica en el reporte "Colapso total de viviendas, 300 damnificados. Urge agua potable." el daño "CRITICAL", 300 personas afectadas y la necesidad "WATER"
    Cuando la autoridad registra el reporte "Colapso total de viviendas, 300 damnificados. Urge agua potable." en la zona "Sector Alto Perú"
    Entonces el reporte queda procesado con el daño "CRITICAL", 300 personas afectadas y la necesidad "WATER"

  Escenario: Reporte con información ambigua o incompleta
    Dado que el NLP no identifica ninguna variable en el reporte "Se reportan problemas en la zona, se pide apoyo."
    Cuando la autoridad registra el reporte "Se reportan problemas en la zona, se pide apoyo." en la zona "Sector Alto Perú"
    Entonces el reporte queda procesado con "damageLevel" y "peopleAffected" marcadas como no disponibles
