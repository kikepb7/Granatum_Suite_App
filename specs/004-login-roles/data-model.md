# Phase 1 — Modelo de datos

**Feature**: `004-login-roles` · **Fecha**: 2026-10-08

## Session (`core/domain`)

Lo que la app conserva entre aperturas. Sustituye a `AuthInfoModel` y `UserModel`.

| Campo | Tipo | Origen | Regla |
|---|---|---|---|
| `accessToken` | String | `ParTokensResponse.accessToken` | No vacío |
| `refreshToken` | String | `ParTokensResponse.refreshToken` | No vacío; rota en cada renovación |
| `employeeId` | String | claim `sub` del token | No vacío; si falta, login fallido (FR-011) |
| `role` | `UserRole` | claim `role` | Desconocido → `DESCONOCIDO` (FR-010) |
| `mustChangePassword` | Boolean | `requiereCambioPassword` **o** claim `pwd_change` | Gana el valor más restrictivo |
| `email` | String | lo que la persona tecleó, recortado y en minúsculas | Solo para mostrar (FR-025) |

**No se guarda** `expiresIn` (FR-023, research D2).

**Persistencia**: JSON en el almacén seguro bajo `granatum.session.secure.v2`. Al arrancar se
borra `granatum.session.secure.v1`. Una entrada `v2` ilegible se descarta y la app arranca sin
sesión (FR-024).

**Tras renovar**: `accessToken`, `refreshToken`, `role` y `mustChangePassword` se reemplazan con
lo que diga el token nuevo; `email` se conserva.

## UserRole (`core/domain`)

| Valor | `canClockIn` | `canManageInventory` | `canSeeTeam` |
|---|---|---|---|
| `ADMIN` | ✅ | ✅ | ✅ |
| `ENCARGADO` | ✅ | ✅ | ✅ |
| `EMPLEADO` | ✅ | — | — |
| `REPRESENTANTE` | — | — | — |
| `DESCONOCIDO` | — | — | — |

Todos ven el historial. Las tres propiedades sustituyen a `canManageTeam`.

## SessionState (`core/domain`)

| Estado | Qué se ve |
|---|---|
| `Loading` | Pantalla de arranque |
| `SignedOut(reason?)` | Pantalla de inicio de sesión; mensaje si hay `reason` |
| `PasswordChangeRequired` | Solo el cambio de contraseña y cerrar sesión |
| `Active(role)` | Las pestañas del rol |

### Transiciones

| Desde | Evento | Hacia |
|---|---|---|
| `Loading` | No hay sesión, o la guardada es ilegible | `SignedOut(null)` |
| `Loading` | Sesión con `mustChangePassword` | `PasswordChangeRequired` |
| `Loading` | Sesión válida | `Active(role)` |
| `SignedOut` | Login correcto con cambio pendiente | `PasswordChangeRequired` |
| `SignedOut` | Login correcto | `Active(role)` |
| `PasswordChangeRequired` | Cambio correcto | `Active(role)` |
| cualquiera con sesión | Cierre de sesión de la persona | `SignedOut(null)` |
| cualquiera con sesión | Rechazo definitivo del servidor | `SignedOut(reason)` |
| `Active` | Renovación que trae `pwd_change` | `PasswordChangeRequired` |

La falta de red **no** provoca ninguna transición (FR-020).

## SignOutReason (`core/domain`)

| Valor | Cuándo | Mensaje (FR-021) |
|---|---|---|
| `INACTIVE` | `401 EMPLEADO_INACTIVO` al renovar | Tu cuenta ya no está activa. Habla con la administración. |
| `SESSION_REJECTED` | `401 TOKEN_RENOVACION_INVALIDO` o `401 NO_AUTENTICADO` | Tu sesión ha terminado. Vuelve a iniciar sesión. |

`TOKEN_RENOVACION_INVALIDO` también cubre el caso de que se cambiara la contraseña en otro
dispositivo, porque ese cambio revoca las demás sesiones. El mensaje no lo presenta como un
fallo de la app.

## AuthError (`core/domain`)

Ver research D3. Implementa `Error` y viaja en `Result<D, AuthError>`.

| Caso | Datos |
|---|---|
| `InvalidCredentials` | — |
| `TooManyAttempts` | `retryAfterSeconds: Long?` |
| `ServiceBusy` | `retryAfterSeconds: Long?` |
| `WeakPassword` | `requirements: Set<PasswordRequirement>` |
| `Validation` | — |
| `NoInternet` | — |
| `Timeout` | — |
| `InvalidSession` | — |
| `Unknown` | — |

## PasswordRequirement (`core/domain`)

`LONGITUD_MINIMA`, `LONGITUD_MAXIMA`, `FALTA_MAYUSCULA`, `FALTA_MINUSCULA`, `FALTA_DIGITO`,
`FALTA_SIMBOLO`. Son los mismos identificadores del `422 PASSWORD_DEBIL`. Un identificador que
la app no conozca se ignora en la lista y se muestra el mensaje genérico de contraseña débil.

`PasswordValidator.validate(password)` devuelve el conjunto de requisitos incumplidos; vacío
significa válida.

| Requisito | Regla local |
|---|---|
| `LONGITUD_MINIMA` | `length < 8` |
| `LONGITUD_MAXIMA` | `length > 128` |
| `FALTA_MAYUSCULA` | ningún `isUpperCase()` |
| `FALTA_MINUSCULA` | ningún `isLowerCase()` |
| `FALTA_DIGITO` | ningún `isDigit()` |
| `FALTA_SIMBOLO` | ningún carácter que no sea letra, dígito ni espacio en blanco |

## ClockEventEntity (`feature/clockin/database`) — cambio

| Campo nuevo | Tipo | Regla |
|---|---|---|
| `employeeId` | `String?` | El `employeeId` de la sesión al fichar. `null` solo en filas anteriores a esta feature |

- Base de datos versión **1 → 2**, migración automática (añadir una columna anulable).
- Las consultas de pendientes, de hoy, del historial y del contador filtran por
  `employeeId = :currentEmployeeId`. Sin sesión no hay filas visibles.
- Las filas con `employeeId` nulo nunca se envían (research D6).
