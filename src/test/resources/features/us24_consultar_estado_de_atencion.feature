# language: es
Característica: US-24 Consultar el estado de atención de una zona
  Como ciudadano afectado por un desastre,
  quiero consultar el estado de atención de mi zona,
  para saber si ya fue registrada y en qué etapa del proceso se encuentra.

  Escenario: Consulta del estado de atención de una zona registrada
    Dado que la zona "Sector Alto Perú" tiene un plan de ayuda aprobado y su brigada en camino
    Cuando un ciudadano consulta el estado de la zona sin iniciar sesión
    Entonces se muestra la etapa "AID_IN_TRANSIT"
    Y no se muestra el score de urgencia ni información de terceros

  Escenario: Zona no registrada aún
    Cuando un ciudadano consulta el estado de una zona que no está registrada
    Entonces el sistema indica que no existe información disponible
