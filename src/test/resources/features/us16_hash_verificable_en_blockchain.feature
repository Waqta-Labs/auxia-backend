# language: es
Característica: US-16 Generar un hash verificable de la entrega en Blockchain
  Como autoridad responsable de atender desastres,
  quiero que cada entrega registrada genere un hash verificable en Blockchain,
  para garantizar que la evidencia no pueda alterarse posteriormente sin detectarse.

  Antecedentes:
    Dado una distribución aprobada para la zona "Sector Alto Perú" con su brigada asignada

  Escenario: Generación del hash verificable al registrar la entrega
    Cuando la brigada registra la entrega con una foto
    Entonces se genera un hash de la entrega y de su evidencia
    Y el hash queda almacenado en Blockchain con la transacción que lo registró

  Escenario: Detección de alteración de la evidencia original
    Dado que la brigada registró la entrega con una foto
    Cuando alguien reemplaza la foto en el almacenamiento de evidencias
    Entonces el hash recalculado no coincide con el almacenado en Blockchain
