# language: es
Característica: US-17 Consultar el estado de verificación de una entrega
  Como autoridad responsable de atender desastres,
  quiero consultar si el hash de una entrega coincide con lo registrado en Blockchain,
  para confirmar que la evidencia no fue alterada.

  Antecedentes:
    Dado una distribución aprobada para la zona "Sector Alto Perú" con su brigada asignada
    Y que la brigada registró la entrega con una foto

  Escenario: Verificación exitosa de una entrega
    Cuando se verifica la entrega con su código, sin iniciar sesión
    Entonces el sistema indica que el hash actual coincide con el almacenado en Blockchain

  Escenario: Señalización de una entrega no verificable
    Dado que alguien cambia en la base de datos la cantidad entregada a 900
    Cuando se verifica la entrega con su código, sin iniciar sesión
    Entonces el sistema señala la entrega como no verificable
