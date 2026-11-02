# language: es
Característica: US-18 Excluir datos personales sensibles del registro en Blockchain
  Como autoridad responsable de atender desastres,
  quiero que el sistema impida almacenar datos personales sensibles de la población afectada en Blockchain,
  para proteger su privacidad conforme a las restricciones del proyecto.

  Antecedentes:
    Dado una distribución aprobada para la zona "Sector Alto Perú" con su brigada asignada

  Escenario: Exclusión de datos sensibles del registro en Blockchain
    Cuando la brigada registra la entrega con una foto
    Entonces el hash enviado a Blockchain solo cubre identificadores, cantidades, hashes de la evidencia y la fecha

  Escenario: Exclusión de un campo con información personal detectada
    Cuando la brigada registra la entrega con una foto y el campo "beneficiario" con "Juana Pérez, DNI 45678912"
    Entonces la entrega no guarda "Juana Pérez, DNI 45678912"
    Y el hash enviado a Blockchain solo cubre identificadores, cantidades, hashes de la evidencia y la fecha
