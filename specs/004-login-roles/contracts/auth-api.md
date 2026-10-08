# Contrato — Uso de la API de acceso

**Feature**: `004-login-roles` · **Fecha**: 2026-10-08

Cómo usa la app cada ruta y qué hace con cada respuesta. Las formas vienen de `docs/openapi.json`
(backend `d1857ad`); los códigos de error, de `specs/002-auth/contracts/README.md` del backend en
ese mismo commit. Este documento no añade nada al contrato del servidor: solo fija la reacción de
la app.

Todas las rutas cuelgan de `BASE_URL_HTTP`, que ya termina en `/api`.

## `POST /auth/login`

**Envía**: `{ "email": <recortado>, "password": <tal cual> }`. Antes de enviar se valida en
local: ninguno vacío, correo con formato válido, correo ≤ 254 y contraseña ≤ 128.

| Respuesta | Reacción |
|---|---|
| `200 ParTokensResponse` | Decodificar el token (research D1), construir `Session`, guardarla. Pasar a `PasswordChangeRequired` o a `Active(role)` |
| `200` con token sin `sub` o ilegible | `AuthError.InvalidSession`; no se guarda nada |
| `400 VALIDACION` | `AuthError.Validation` |
| `401 CREDENCIALES_INVALIDAS` | `AuthError.InvalidCredentials`: mensaje único (FR-003) |
| `429 DEMASIADAS_PETICIONES` | `AuthError.TooManyAttempts(Retry-After)` |
| `503 SERVICIO_SATURADO` | `AuthError.ServiceBusy(Retry-After)` |
| Sin respuesta | `AuthError.NoInternet` o `AuthError.Timeout` |
| Cualquier otra | `AuthError.Unknown` |

`Retry-After` se interpreta como segundos. Si falta o no es un entero, el mensaje no da cifra.

## `POST /auth/refresh`

Solo lo invoca el cliente HTTP, nunca una pantalla. Ver research D4.

**Envía**: `{ "refreshToken": <de la sesión> }`, marcado como petición de renovación para que no
se renueve a sí misma.

| Respuesta | Reacción |
|---|---|
| `200 ParTokensResponse` | Reemplazar los tokens y lo que diga el token nuevo; conservar `email` |
| `401 TOKEN_RENOVACION_INVALIDO` | Borrar sesión → `SignedOut(SESSION_REJECTED)` |
| `401 EMPLEADO_INACTIVO` | Borrar sesión → `SignedOut(INACTIVE)` |
| `400`, `429`, `5xx`, sin red, timeout | **Conservar la sesión**; la petición original falla como sin conexión |

## `POST /auth/logout`

**Envía**: `{ "refreshToken": <de la sesión> }`.

| Respuesta | Reacción |
|---|---|
| `204` | — |
| Cualquier otra, o sin red | Se ignora |

**En todos los casos**, después de intentarlo, se borra la sesión local y se vacía la caché de
tokens del cliente HTTP → `SignedOut(null)` (FR-026). El intento tiene un tiempo máximo corto
para que cerrar sesión sin cobertura no se quede esperando.

## `POST /auth/change-password`

**Envía**: `Authorization: Bearer <accessToken>` y
`{ "passwordActual": ..., "passwordNueva": ... }`. Antes de enviar,
`PasswordValidator.validate(passwordNueva)` debe estar vacío.

| Respuesta | Reacción |
|---|---|
| `200 ParTokensResponse` | Guardar la sesión nueva (`mustChangePassword = false`) → `Active(role)` (FR-016) |
| `401 CREDENCIALES_INVALIDAS` | `AuthError.InvalidCredentials`: «la contraseña actual no es correcta»; se conserva la nueva escrita (FR-015) |
| `422 PASSWORD_DEBIL` | `AuthError.WeakPassword(requisitos)` (FR-014) |
| `401 TOKEN_ACCESO_EXPIRADO` | Lo resuelve el cliente HTTP renovando, como en cualquier ruta |
| `429`, `503`, sin red | Igual que en login |

> **Ojo con este `401`**: `CREDENCIALES_INVALIDAS` llega aquí con un token válido. El criterio de
> renovación solo actúa ante `TOKEN_ACCESO_EXPIRADO`, de modo que este `401` nunca dispara una
> renovación ni un cierre de sesión.

## Cualquier ruta protegida

| Respuesta | Reacción |
|---|---|
| `401 TOKEN_ACCESO_EXPIRADO` | Renovar una sola vez (single-flight) y repetir la petición |
| `401 NO_AUTENTICADO` | Borrar sesión → `SignedOut(SESSION_REJECTED)` |
| `403 FORBIDDEN` | Error de la pantalla; no toca la sesión |

## Cabeceras y registro

- Toda petición lleva `x-api-key` (BuildKonfig) y `Content-Type: application/json`, como hoy.
- Ninguna petición a `/auth/*` se registra en el log, ni la petición ni la respuesta. En el resto
  se ocultan `Authorization` y `x-api-key` (FR-030).
