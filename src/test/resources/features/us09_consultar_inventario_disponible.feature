# language: es
Característica: US-09 Consultar el inventario disponible
  Como autoridad responsable de atender desastres,
  quiero consultar en cualquier momento el inventario disponible por tipo de recurso,
  para decidir cuánto se puede distribuir sin exceder el stock real.

  Antecedentes:
    Dado un inventario con estos recursos:
      | recurso          | disponible |
      | Agua 2.5 L       | 100        |
      | Kit de alimentos | 40         |

  Escenario: Consulta del inventario actualizado
    Cuando la autoridad consulta el inventario
    Entonces se muestra la cantidad disponible de cada recurso:
      | recurso          | disponible |
      | Agua 2.5 L       | 100        |
      | Kit de alimentos | 40         |

  Escenario: Reflejo de recursos reservados como no disponibles
    Dado que un plan de distribución reservó 60 unidades de "Agua 2.5 L"
    Cuando la autoridad consulta el inventario
    Entonces "Agua 2.5 L" muestra 60 reservadas y 40 disponibles
