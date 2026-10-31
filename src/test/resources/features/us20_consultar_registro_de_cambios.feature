# language: es
Característica: US-20 Consultar el registro de cambios sobre una zona
  Como autoridad responsable de atender desastres,
  quiero consultar el historial de decisiones tomadas sobre una zona,
  para reconstruir el proceso seguido ante una revisión posterior.

  Antecedentes:
    Dado una autoridad que sigue las zonas "Sector Alto Perú" y "Quebrada Pedregal"
    Y la autoridad prioriza a mano la zona "Sector Alto Perú" porque "Riesgo inminente de un nuevo huaico"
    Y la autoridad le quita la prioridad manual a la zona "Sector Alto Perú"

  Escenario: Consulta cronológica del historial de decisiones
    Cuando se consulta el registro de cambios de la zona "Sector Alto Perú"
    Entonces el registro de cambios lista en orden "ZONE_REGISTERED, MANUAL_PRIORITY_SET, MANUAL_PRIORITY_REMOVED"
    Y cada decisión muestra a la autoridad que la tomó

  Escenario: Visibilidad del historial tras un cambio de turno
    Cuando la autoridad registra el cambio de turno con otra autoridad
    Y la nueva autoridad consulta el registro de cambios de la zona "Sector Alto Perú"
    Entonces el registro de cambios lista en orden "ZONE_REGISTERED, MANUAL_PRIORITY_SET, MANUAL_PRIORITY_REMOVED, SHIFT_HANDED_OVER"
    Y las decisiones del turno anterior muestran a la autoridad anterior
