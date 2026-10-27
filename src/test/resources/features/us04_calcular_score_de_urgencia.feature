# language: es
Característica: US-04 Calcular el score de urgencia de una zona
  Como autoridad responsable de atender desastres,
  quiero que el sistema calcule un score de urgencia por zona,
  para identificar rápidamente cuáles requieren atención prioritaria.

  Escenario: Cálculo del score con variables suficientes
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú" de 600 habitantes
    Cuando el sistema calcula el score de urgencia de la zona "Sector Alto Perú"
    Entonces la zona "Sector Alto Perú" tiene un score de urgencia con su fecha de cálculo

  Escenario: Zona sin variables suficientes para el cálculo
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Quebrada Pedregal"
    Cuando el sistema intenta calcular el score de urgencia de la zona "Quebrada Pedregal"
    Entonces la zona "Quebrada Pedregal" se marca como score no disponible
