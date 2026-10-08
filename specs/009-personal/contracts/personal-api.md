# Contrato usado (backend `2ec33d0`)

| Ruta | Quién | Uso | Errores |
|---|---|---|---|
| `POST /api/auth/registro` `{email, password, nombre, documentoIdentidad, codigoArranque}` → 201 `{estado, mensaje}` | pública | Registro del propietario | 400 `VALIDACION`, 403 `CODIGO_ARRANQUE_INVALIDO`, 409 `EMAIL_YA_REGISTRADO`, 409 `CUENTA_YA_EXISTE`, 422 `DOCUMENTO_INVALIDO`, 422 `PASSWORD_DEBIL`, 429 |
| `POST /api/auth/altas` `{nombre, documentoIdentidad, puesto, tipoContrato, fechaAlta, email, rol}` → 201 `{empleadoId, cuentaId, email, rol, fichaCreada, passwordTemporal}` | ADMIN | Alta en un paso | 400, 409 `EMAIL_YA_REGISTRADO`, 409 `CUENTA_YA_EXISTE`, 422 `DOCUMENTO_INVALIDO` |
| `POST /api/auth/cuentas` `{empleadoId, email, rol}` → 201 `{cuentaId, empleadoId, email, rol, passwordTemporal}` | ADMIN | Dar acceso a una ficha sin cuenta | 404 `EMPLEADO_NO_ENCONTRADO`, 409 `CUENTA_YA_EXISTE`, 409 `EMAIL_YA_REGISTRADO` |
| `POST /api/auth/cuentas/{empleadoId}/restablecer` → 200 (mismo cuerpo) | ADMIN | Restablecer contraseña | 404 `CUENTA_NO_ENCONTRADA` |
| `GET /api/empleados?activo` → `[EmpleadoDto]` | ADMIN | Lista | — |
| `GET /api/empleados/{id}` | ADMIN | Ficha | 404 `EMPLEADO_NOT_FOUND` |
| `PUT /api/empleados/{id}` `{nombre, puesto, tipoContrato, fechaAlta}` | ADMIN | Editar (sin documento) | 404, 422 `VALORES_INCOHERENTES` |
| `PATCH /api/empleados/{id}/activo` `{activo}` | ADMIN | Baja y reactivación | 404 |

`EmpleadoDto`: `id, nombre, documentoIdentidad, puesto, tipoContrato (JORNADA_COMPLETA|PARCIAL|POR_HORAS), fechaAlta, activo`.
