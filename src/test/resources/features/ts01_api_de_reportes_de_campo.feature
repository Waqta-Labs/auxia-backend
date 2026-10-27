# language: es
Característica: TS-01 API para el registro y consulta de reportes de campo
  Como developer,
  quiero exponer un endpoint RESTful para crear y consultar reportes de campo estructurados,
  para que el frontend y otros servicios puedan integrarse con el módulo de NLP.

  Antecedentes:
    Dado una emergencia activa "Huaico Chosica 2026" con la zona "Sector Alto Perú"

  Escenario: Creación exitosa de un reporte
    Cuando se envía una solicitud POST con el texto de un reporte válido a la zona "Sector Alto Perú"
    Entonces el endpoint responde con código 201 y el reporte registrado
    Y el reporte aparece al consultar los reportes de la zona "Sector Alto Perú"

  Escenario: Solicitud sin campo de texto del reporte
    Cuando se envía una solicitud POST sin el campo de texto del reporte a la zona "Sector Alto Perú"
    Entonces el endpoint responde con código 400 indicando el campo faltante "text"
