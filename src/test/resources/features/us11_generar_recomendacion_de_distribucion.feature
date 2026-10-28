# language: es
Característica: US-11 Generar una recomendación de distribución
  Como autoridad responsable de atender desastres,
  quiero que el sistema recomiende una distribución de recursos entre las zonas priorizadas,
  para tomar decisiones más rápidas sin exceder el inventario disponible.

  Escenario: Generación de una recomendación dentro del inventario disponible
    Dado un inventario con estos recursos disponibles:
      | recurso          | categoría | disponible |
      | Agua 2.5 L       | WATER     | 500        |
      | Kit de alimentos | FOOD      | 300        |
    Y estas zonas priorizadas:
      | zona             | daño     | necesidades |
      | Sector Alto Perú | CRITICAL | WATER, FOOD |
    Cuando la autoridad solicita una recomendación
    Entonces el sistema genera una propuesta que no excede el inventario disponible

  Escenario: Indicación de zonas desatendidas por inventario insuficiente
    Dado un inventario con estos recursos disponibles:
      | recurso    | categoría | disponible |
      | Agua 2.5 L | WATER     | 500        |
    Y estas zonas priorizadas:
      | zona              | daño     | necesidades |
      | Sector Alto Perú  | CRITICAL | WATER       |
      | Quebrada Pedregal | MODERATE | WATER       |
    Cuando la autoridad solicita una recomendación
    Entonces el sistema indica que la zona "Quebrada Pedregal" quedaría totalmente desatendida
