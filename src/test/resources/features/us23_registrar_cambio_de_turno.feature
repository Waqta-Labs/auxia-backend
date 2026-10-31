# language: es
Característica: US-23 Registrar el cambio de turno entre responsables
  Como autoridad responsable de atender desastres,
  quiero registrar el cambio de turno con el siguiente responsable,
  para que no se pierda el seguimiento de las zonas en curso.

  Antecedentes:
    Dado una autoridad que sigue las zonas "Sector Alto Perú" y "Quebrada Pedregal"

  Escenario: Asociación de zonas activas al nuevo responsable
    Cuando la autoridad registra el cambio de turno con otra autoridad
    Entonces la nueva autoridad queda como responsable de las zonas "Sector Alto Perú" y "Quebrada Pedregal"
    Y la autoridad anterior ya no tiene zonas a su cargo

  Escenario: Visualización del estado de zonas tras el cambio de turno
    Cuando la autoridad registra el cambio de turno con otra autoridad
    Y la nueva autoridad inicia sesión y consulta las zonas a su cargo
    Entonces ve la zona "Sector Alto Perú" en estado "PRIORITIZED" con el mismo score
    Y ve la zona "Quebrada Pedregal" en estado "REGISTERED" con el mismo score
