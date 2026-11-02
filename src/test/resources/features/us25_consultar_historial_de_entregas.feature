# language: es
Característica: US-25 Consultar el historial de entregas realizadas en una zona
  Como ciudadano afectado por un desastre,
  quiero consultar el historial de entregas realizadas en mi zona (cuántas se realizaron, cuándo fue la última y si está verificada),
  para confirmar que la ayuda fue distribuida.

  Antecedentes:
    Dado que la zona "Sector Alto Perú" tiene un plan de ayuda aprobado y su brigada en camino

  Escenario: Consulta del historial de entregas de una zona
    Dado que la brigada registró la entrega del "2026-03-15T10:30:00" y quedó verificada en Blockchain
    Cuando un ciudadano consulta el estado de la zona sin iniciar sesión
    Entonces se muestra 1 entrega realizada, la última del "2026-03-15T10:30:00", con el estado "VERIFIED"
    Y no se muestran datos personales ni los recursos entregados

  Escenario: Entrega pendiente de verificación
    Dado que la brigada registró la entrega del "2026-03-15T10:30:00" y todavía no está verificada en Blockchain
    Cuando un ciudadano consulta el estado de la zona sin iniciar sesión
    Entonces la última entrega se muestra con el estado "PENDING_VERIFICATION"
