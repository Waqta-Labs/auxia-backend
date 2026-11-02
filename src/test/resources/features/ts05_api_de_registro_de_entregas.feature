# language: es
Característica: TS-05 API para registrar una entrega y su hash en Blockchain
  Como developer,
  quiero exponer un endpoint RESTful que registre una entrega, genere su hash y lo envíe a Blockchain,
  para que el módulo de trazabilidad pueda verificar la evidencia posteriormente.

  Antecedentes:
    Dado una distribución aprobada para la zona "Sector Alto Perú" con su brigada asignada

  Escenario: Registro de una entrega con evidencia válida
    Cuando se envía la solicitud POST de registro con la entrega y una foto
    Entonces la respuesta tiene el código 201 y el hash generado
    Y la entrega muestra la referencia de la transacción en Blockchain

  Escenario: Registro de una entrega sin evidencia
    Cuando se envía la solicitud POST de registro con la entrega y sin evidencia
    Entonces la respuesta tiene el código 400
    Y la entrega no tiene un hash generado
