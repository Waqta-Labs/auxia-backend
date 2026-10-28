# language: es
Característica: US-15 Registrar una entrega realizada con evidencia
  Como autoridad responsable de atender desastres,
  quiero registrar una entrega junto con su evidencia (foto y geolocalización),
  para dejar constancia verificable de lo distribuido.

  Antecedentes:
    Dado una distribución aprobada para la zona "Sector Alto Perú" con su brigada asignada

  Escenario: Registro exitoso de una entrega con evidencia
    Cuando la brigada registra la entrega de 300 unidades de agua con una foto tomada en la latitud "-11.9436" y la longitud "-76.7078"
    Entonces la evidencia queda asociada a la entrega y a la zona "Sector Alto Perú"
    Y la foto conserva la ubicación donde se tomó

  Escenario: Bloqueo del registro de una entrega sin evidencia
    Cuando la brigada intenta registrar la entrega sin evidencia
    Entonces el sistema no permite completar el registro
    Y la entrega sigue pendiente de registro
