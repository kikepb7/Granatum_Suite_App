# Phase 1 — Modelo de datos

**Feature**: `005-fichaje-real` · **Fecha**: 2026-10-08

## Base de datos local, versión 3

### `shift` (nueva)

| Campo | Tipo | Regla |
|---|---|---|
| `localId` | String, PK | UUID generado en el dispositivo |
| `employeeId` | String | De la sesión; nunca nulo |
| `serverId` | String? | `id` del `FichajeDto`; nulo hasta que el servidor registra o adopta la entrada |
| `startedAtEpochMillis` | Long | `occurredAt` de su `CLOCK_IN` |

### `clock_event` (cambios)

| Campo | Tipo | Regla |
|---|---|---|
| `id` | String, PK | También es el `clientEventId`. Se genera una vez y no cambia nunca |
| `type` | String | `CLOCK_IN`, `BREAK_START`, `BREAK_END`, `CLOCK_OUT` |
| `clientTimestampEpochMillis` | Long | El `occurredAt`. Se formatea siempre igual (research D5) |
| `employeeId` | String | Ya no admite nulos: las filas sin dueño se borran al migrar |
| `shiftLocalId` | String? | **Nuevo.** Jornada local a la que pertenece |
| `breakType` | String? | **Nuevo.** `COMIDA`, `DESCANSO` u `OTRO`; solo en `BREAK_START` |
| `syncState` | String | `PENDING`, `SYNCING`, `SYNCED` o `REJECTED` (desaparece `FAILED`) |
| `rejectionCode` | String? | **Nuevo.** El `code` del servidor, o `JORNADA_NO_REGISTRADA` |
| `retryCount`, `lastSyncAttemptEpochMillis`, `serverTimestampEpochMillis` | — | Sin cambios |

### `server_shift` (nueva, caché)

| Campo | Tipo | Origen |
|---|---|---|
| `id` | String, PK | `FichajeDto.id` |
| `employeeId` | String | `FichajeDto.empleadoId` |
| `entradaEpochMillis` | Long | `entrada` |
| `salidaEpochMillis` | Long? | `salida` |
| `estado` | String | `EN_CURSO`, `CERRADO` o `INCOMPLETO` |
| `minutosTrabajados` | Int? | `minutosTrabajados` |
| `fueIncompleto` | Boolean | `fueIncompleto` |
| `pausasJson` | String | `pausas` serializadas: `id`, `tipo`, `inicio`, `fin` |
| `corregido`, `reconstruido` | Boolean | Del resumen mensual, por fecha (research D6) |
| `fetchedAtEpochMillis` | Long | Momento de la descarga |

### Migración 2 → 3

Manual, con el orden de research D8: borrar las filas sin dueño, crear tablas y columnas, agrupar
los eventos en jornadas y pasar `FAILED` a `PENDING`.

## Dominio (`feature/clockin/domain`)

### `BreakType`

`COMIDA`, `DESCANSO`, `OTRO`. Los mismos nombres que en el contrato.

### `ShiftModel`

| Campo | Regla |
|---|---|
| `key` | `serverId` si existe; si no, `localId` |
| `serverId` | nulo si el servidor aún no la conoce |
| `start`, `end` | `end` nulo si está en curso |
| `breaks: List<BreakModel>` | `tipo`, `inicio`, `fin?`, `pending: Boolean` |
| `state` | `IN_PROGRESS`, `CLOSED` o `INCOMPLETE` |
| `workedMinutes: Int?` | Del servidor si consta; si no, estimación local |
| `isWorkedMinutesProvisional` | `true` mientras haya eventos sin enviar o el servidor no la conozca |
| `flags` | `incomplete`, `corrected`, `reconstructed` (FR-017) |
| `pendingEvents`, `rejectedEvents` | Para pintar su estado de envío |

### `ClockRejection`

| Código del servidor | Caso de dominio | Texto (presentation) |
|---|---|---|
| `DESVIACION_RELOJ`, con `occurredAt` de más de 72 h | `TooOld` | «Este fichaje tiene más de 72 horas y ya no se puede registrar. Pide una corrección de la jornada.» |
| `DESVIACION_RELOJ`, en otro caso | `ClockSkew` | «La hora de este dispositivo no es correcta. Ajústala para que tus fichajes se registren.» |
| `EMPLEADO_INACTIVO` | `Inactive` | «Tu cuenta ya no está activa.» |
| `PAUSA_*`, `FICHAJE_NO_EN_CURSO` | `InvalidTransition` | «El registro ya no admitía este fichaje; quizá se fichó desde otro dispositivo.» |
| `JORNADA_NO_REGISTRADA` | `ShiftNotRegistered` | «La entrada de esta jornada no se pudo registrar.» |
| cualquier otro | `Unexpected` | «No se ha podido registrar este fichaje.» |

### `CorrectionModel`

`id`, `shiftServerId`, `estado` (`PENDIENTE`, `APROBADA`, `RECHAZADA`), `motivo`,
`valoresPropuestos` (entrada, salida, pausas), `motivoResolucion?`, `creadaEn`, `resueltaEn?`.

### `CorrectionDraft` y su validación (FR-020)

| Regla | Error |
|---|---|
| `motivo` no vacío | `MISSING_REASON` |
| `salida > entrada` | `EXIT_BEFORE_ENTRY` |
| cada pausa con `fin > inicio` | `BREAK_ENDS_BEFORE_START` |
| cada pausa dentro de `[entrada, salida]` | `BREAK_OUTSIDE_SHIFT` |
| las pausas no se solapan | `BREAKS_OVERLAP` |

## Transiciones permitidas (FR-004)

| Estado | Entrada | Iniciar pausa | Fin de pausa | Salida |
|---|---|---|---|---|
| Sin jornada | ✅ | — | — | — |
| En curso, sin pausa | — | ✅ | — | ✅ |
| En pausa | — | — | ✅ | — (cerrar la pausa antes) |
