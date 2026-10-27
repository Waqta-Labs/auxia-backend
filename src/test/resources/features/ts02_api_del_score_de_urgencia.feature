# language: es
Característica: TS-02 API para el cálculo del score de urgencia
  Como developer,
  quiero exponer un endpoint RESTful que calcule y devuelva el score de urgencia de una zona junto con su explicación,
  para integrarlo con los módulos de priorización y visualización.

  Escenario: Cálculo exitoso del score de una zona existente
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú" de 600 habitantes
    Cuando se envía una solicitud GET del score de la zona "Sector Alto Perú"
    Entonces el endpoint responde con código 200, el score calculado y sus variables explicativas

  Escenario: Solicitud sobre una zona inexistente
    Cuando se envía una solicitud GET del score de una zona inexistente
    Entonces el endpoint responde con código 404
