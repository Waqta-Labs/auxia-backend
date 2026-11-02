# language: es
Característica: US-14 Justificar el rechazo o modificación de una recomendación
  Como autoridad responsable de atender desastres,
  quiero registrar una justificación cuando rechazo o modifico una recomendación del sistema,
  para sustentar la decisión ante una auditoría posterior.

  Antecedentes:
    Dado una recomendación generada para la zona "Sector Alto Perú"

  Esquema del escenario: Exigencia de justificación al rechazar o modificar una recomendación
    Cuando la autoridad <acción> la recomendación sin justificación
    Entonces el sistema exige el registro de una justificación
    Y la recomendación sigue pendiente de decisión

    Ejemplos:
      | acción   |
      | rechaza  |
      | modifica |

  Escenario: Persistencia de la justificación en el historial de la zona
    Dado que la autoridad rechazó la recomendación con la justificación "La vía de acceso está bloqueada"
    Cuando se consulta el historial de la zona "Sector Alto Perú"
    Entonces la justificación "La vía de acceso está bloqueada" queda asociada al plan rechazado
