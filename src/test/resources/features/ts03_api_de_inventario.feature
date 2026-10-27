# language: es
Característica: TS-03 API para consulta y actualización de inventario
  Como developer,
  quiero exponer endpoints RESTful para consultar y actualizar el inventario de recursos,
  para que los módulos de distribución puedan validar disponibilidad en tiempo real.

  Antecedentes:
    Dado un inventario con el recurso "Agua 2.5 L" y 100 unidades disponibles

  Escenario: Consulta exitosa del inventario
    Cuando se envía una solicitud GET del inventario
    Entonces el endpoint responde con código 200 y la cantidad disponible de cada recurso

  Escenario: Actualización que excede el inventario disponible
    Cuando se envía una solicitud para reducir "Agua 2.5 L" en 150 unidades
    Entonces el endpoint responde con código 409 y el inventario no se modifica
