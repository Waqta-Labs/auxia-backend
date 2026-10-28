# language: es
Característica: US-13 Modificar una distribución recomendada
  Como autoridad responsable de atender desastres,
  quiero modificar las cantidades o zonas de una distribución recomendada antes de aprobarla,
  para ajustar la recomendación a información que el sistema no considera.

  Antecedentes:
    Dado una recomendación generada para la zona "Sector Alto Perú"

  Esquema del escenario: Modificación válida de una cantidad recomendada
    Cuando la autoridad modifica la cantidad de "Agua 2.5 L" a <cantidad>
    Entonces la modificación es <resultado>

    Ejemplos:
      | cantidad | resultado                                      |
      | 200      | aceptada                                       |
      | 501      | rechazada por exceder el inventario disponible |

  Escenario: Conservación de la recomendación original tras aprobar la versión modificada
    Dado que la autoridad modificó la cantidad de "Agua 2.5 L" a 200
    Cuando la autoridad aprueba la recomendación
    Entonces el plan conserva para "Agua 2.5 L" la cantidad recomendada de 500 y la aprobada de 200
