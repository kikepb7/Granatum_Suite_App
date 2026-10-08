# Quickstart: verificar el alta del propietario y el personal

Backend `2ec33d0` en local con `AUTH_CODIGO_ARRANQUE` (24+ caracteres) y base de datos vacía.

| # | Escenario | Esperado |
|---|---|---|
| 1 | Login: buscar registro de empleados | Solo «Soy el propietario: crear la cuenta» |
| 2 | Registro con código incorrecto | «Código no válido o la instalación ya tiene propietario» |
| 3 | Registro con contraseña débil / DNI inválido | Error en el campo |
| 4 | Registro correcto | Entra como ADMIN |
| 5 | Segundo registro con el código correcto | Mismo error que el 2 |
| 6 | Equipo → Dar de alta (EMPLEADO) | Credenciales una vez, copiables |
| 7 | Entrar con ellas en otro móvil | Solo deja cambiar la contraseña |
| 8 | Alta con correo usado / DNI con cuenta | Error claro, nada creado |
| 9 | Editar puesto | Guardado |
| 10 | Desactivar; intentar entrar con esa persona | Confirmación con aviso; no entra |
| 11 | Reactivar | Vuelve a entrar |
| 12 | Restablecer contraseña | Nueva provisional; la sesión del otro móvil se cierra |
| 13 | ENCARGADO | Equipo muestra la jornada, no el personal |
| 14 | Logcat / consola durante el alta | Ni DNI ni contraseña provisional |
