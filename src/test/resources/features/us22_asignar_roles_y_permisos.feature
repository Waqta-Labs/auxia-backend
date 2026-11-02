# language: es
Característica: US-22 Asignar roles y permisos según responsabilidad
  Como administrador de la plataforma,
  quiero asignar roles y permisos a cada autoridad según su nivel de responsabilidad,
  para que cada usuario solo ejecute las acciones correspondientes a su función.

  Antecedentes:
    Dado una organización con un usuario de rol "FIELD_BRIGADE"

  Escenario: Restricción de acciones fuera del rol asignado
    Dado que el usuario inició sesión con su contraseña
    Cuando el usuario intenta registrar una emergencia
    Entonces el sistema deniega la acción con código 403

  Escenario: Actualización de permisos tras modificar el rol
    Dado que el administrador cambia el rol del usuario a "AUTHORITY"
    Cuando el usuario inicia sesión con su contraseña
    Entonces se concede el acceso con el rol "AUTHORITY"
    Y el usuario puede registrar una emergencia
