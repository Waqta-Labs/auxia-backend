# language: es
Característica: US-21 Iniciar sesión con credenciales institucionales
  Como autoridad responsable de atender desastres,
  quiero iniciar sesión en la plataforma con mis credenciales institucionales,
  para acceder únicamente a la información que corresponde a mi rol.

  Antecedentes:
    Dado una organización con un usuario de rol "AUTHORITY"

  Escenario: Inicio de sesión exitoso
    Cuando el usuario inicia sesión con su contraseña
    Entonces se concede el acceso con el rol "AUTHORITY"
    Y el usuario puede registrar una emergencia

  Escenario: Denegación de acceso por credenciales inválidas
    Cuando el usuario inicia sesión con una contraseña incorrecta
    Y alguien inicia sesión con un correo que no está registrado
    Entonces los dos accesos se deniegan con el mismo mensaje
