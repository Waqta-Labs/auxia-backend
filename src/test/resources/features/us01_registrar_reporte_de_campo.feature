# language: es
Característica: US-01 Registrar un reporte de campo en lenguaje natural
  Como autoridad responsable de atender desastres,
  quiero registrar un reporte de campo redactado en lenguaje natural,
  para dejar constancia de la situación de una zona sin completar formularios extensos.

  Antecedentes:
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú"

  Escenario: Registro exitoso de un reporte asociado a una zona
    Cuando la autoridad registra el reporte "Derrumbe de viviendas. Hay 35 familias afectadas." en la zona "Sector Alto Perú"
    Entonces el sistema guarda el reporte y lo asocia a la zona "Sector Alto Perú"

  Escenario: Reporte cuya zona se identifica en el texto
    Cuando la autoridad registra en la emergencia el reporte "Derrumbe de viviendas en el sector Alto Perú." sin indicar la zona
    Entonces el sistema guarda el reporte y lo asocia a la zona "Sector Alto Perú"

  Escenario: Reporte sin zona identificable
    Cuando la autoridad registra en la emergencia el reporte "Hay 35 familias afectadas, urge agua potable." sin indicar la zona
    Entonces se marca el reporte como pendiente de asignación de zona
