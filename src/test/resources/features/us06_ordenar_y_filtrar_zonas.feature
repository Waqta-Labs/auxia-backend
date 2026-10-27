# language: es
Característica: US-06 Ordenar y filtrar zonas según score de urgencia
  Como autoridad responsable de atender desastres,
  quiero ordenar y filtrar las zonas registradas según su score de urgencia,
  para enfocar la atención en las más críticas primero.

  Antecedentes:
    Dado una emergencia activa "Huaico Chosica 2026" con estas zonas:
      | zona               | habitantes | daño     |
      | Quebrada Pedregal  | 600        | MODERATE |
      | Sector Alto Perú   | 600        | CRITICAL |
      | Nicolás de Piérola | 600        | LOW      |

  Escenario: Ordenamiento de zonas por score de urgencia
    Cuando la autoridad solicita el listado ordenado de zonas
    Entonces las zonas se presentan en este orden: "Sector Alto Perú, Quebrada Pedregal, Nicolás de Piérola"

  Escenario: Filtrado de zonas según rango de score
    Cuando la autoridad filtra las zonas con un score mínimo de 30
    Entonces solo se muestran las zonas "Sector Alto Perú, Quebrada Pedregal"

  Escenario: Filtrado de zonas según tipo de necesidad
    Dado que un reporte de la zona "Quebrada Pedregal" menciona la necesidad "WATER"
    Cuando la autoridad filtra las zonas con la necesidad "WATER"
    Entonces solo se muestran las zonas "Quebrada Pedregal"
