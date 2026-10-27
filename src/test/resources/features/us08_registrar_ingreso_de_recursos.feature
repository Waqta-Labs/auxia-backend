# language: es
Característica: US-08 Registrar ingreso de recursos al inventario
  Como autoridad responsable de atender desastres,
  quiero registrar el ingreso de recursos donados o adquiridos,
  para mantener actualizado el inventario disponible.

  Antecedentes:
    Dado un inventario con el recurso "Agua 2.5 L" y 100 unidades disponibles

  Escenario: Registro exitoso del ingreso de recursos
    Cuando la autoridad registra el ingreso de 50 unidades de "Agua 2.5 L"
    Entonces la cantidad disponible de "Agua 2.5 L" es 150

  Escenario: Rechazo de un ingreso con cantidad inválida
    Cuando la autoridad registra el ingreso de -20 unidades de "Agua 2.5 L"
    Entonces el ingreso es rechazado
    Y la cantidad disponible de "Agua 2.5 L" sigue siendo 100
