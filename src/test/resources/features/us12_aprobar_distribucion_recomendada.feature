# language: es
Característica: US-12 Aprobar una distribución recomendada
  Como autoridad responsable de atender desastres,
  quiero aprobar una distribución recomendada por el sistema,
  para autorizar su ejecución bajo mi responsabilidad.

  Antecedentes:
    Dado una recomendación generada para la zona "Sector Alto Perú"

  Escenario: Aprobación exitosa de una distribución recomendada
    Cuando la autoridad aprueba la recomendación
    Entonces la distribución queda habilitada para su ejecución
    Y se registra qué autoridad la aprobó

  Escenario: Intento de aprobar una distribución ya aprobada
    Dado que la autoridad aprobó la recomendación
    Cuando la autoridad aprueba la recomendación
    Entonces el sistema indica que ya se encuentra aprobada
