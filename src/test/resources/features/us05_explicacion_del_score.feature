# language: es
Característica: US-05 Visualizar la explicación del score de una zona
  Como autoridad responsable de atender desastres,
  quiero conocer qué variables influyen más en el score de una zona,
  para sustentar la decisión de priorización ante terceros.

  Escenario: Visualización del detalle explicativo del score
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú" de 600 habitantes
    Cuando la autoridad solicita el detalle explicativo del score de la zona "Sector Alto Perú"
    Entonces se muestran las variables que más influyeron en el score, de mayor a menor

  Escenario: Inclusión de la explicación en el reporte exportado
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú" de 600 habitantes
    Cuando la autoridad exporta el reporte de la zona "Sector Alto Perú"
    Entonces el documento exportado incluye las variables que más influyeron en el score
