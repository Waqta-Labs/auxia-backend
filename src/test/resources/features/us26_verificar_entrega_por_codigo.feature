# language: es
Característica: US-26 Verificar la validez pública de una entrega mediante su código
  Como ciudadano afectado por un desastre,
  quiero verificar la validez de una entrega ingresando su código público,
  para confirmar que corresponde a un registro real y no alterado.

  Escenario: Verificación válida mediante código público
    Dado que la zona "Sector Alto Perú" tiene un plan de ayuda aprobado y su brigada en camino
    Y que la brigada registró la entrega del "2026-03-15T10:30:00" y quedó verificada en Blockchain
    Cuando un ciudadano ingresa el código público de la entrega
    Entonces el sistema indica que la entrega es válida

  Escenario: Código no correspondiente a ninguna entrega
    Cuando un ciudadano ingresa un código que no corresponde a ninguna entrega
    Entonces el sistema indica que el código no es válido
