# language: es
Característica: US-10 Recibir alerta de inventario bajo
  Como autoridad responsable de atender desastres,
  quiero recibir una alerta cuando el inventario de un recurso esté por agotarse,
  para gestionar su reposición a tiempo.

  Antecedentes:
    Dado un inventario con el recurso "Agua 2.5 L" de stock mínimo 50 y 80 unidades disponibles

  Escenario: Generación de alerta por inventario bajo
    Cuando la cantidad disponible de "Agua 2.5 L" baja a 30
    Entonces se genera una alerta abierta para "Agua 2.5 L" con 30 unidades disponibles

  Escenario: Resolución automática de la alerta al reponer inventario
    Dado que la cantidad disponible de "Agua 2.5 L" bajó a 30
    Cuando se reponen 40 unidades de "Agua 2.5 L"
    Entonces la alerta de "Agua 2.5 L" queda resuelta
