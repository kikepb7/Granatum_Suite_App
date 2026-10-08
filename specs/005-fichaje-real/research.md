# Phase 0 — Investigación: fichaje real contra el backend

**Feature**: `005-fichaje-real` · **Fecha**: 2026-10-08

Verificado contra `docs/openapi.json` (backend `d1857ad`), el contrato de jornada del backend en
ese commit (`specs/001-timetracking/contracts/{README,fichajes,correcciones}.md`) y el código
actual de `feature/clockin`.

## D1 — El modelo del servidor es la jornada, no el evento

**Hecho**: el servidor no tiene una ruta de eventos. Ofrece cuatro operaciones:

- `POST /api/fichajes/entrada` crea la jornada y devuelve su `id`;
- `POST /api/fichajes/{id}/pausa/inicio`, `/pausa/fin` y `/salida` operan sobre ella.

Todas devuelven el `FichajeDto` completo.

**Decisión**: el dispositivo sigue guardando **eventos**, porque son lo que la persona hace y lo
que hay que enviar en orden, pero cada evento pertenece a una **jornada local**:

- `shift`: tabla nueva con `localId`, `employeeId`, `serverId` (nulo hasta que el servidor
  responde a la entrada) y `startedAt`.
- `clock_event`: añade `shiftLocalId`, `breakType` (solo en `BREAK_START`) y `rejectionCode`.
  El estado de envío pasa a `PENDING | SYNCING | SYNCED | REJECTED`.

**Por qué no reutilizar el id de la entrada como id de jornada**: la jornada puede adoptarse del
servidor (D3) sin que la entrada local llegue a crearla. Tener una tabla aparte hace explícito el
paso de «aún no tiene id del servidor» a «ya lo tiene».

## D2 — Algoritmo de envío

Para la persona con sesión (spec 004, FR-028), en orden de `occurredAt`:

1. **`CLOCK_IN`** → `POST /entrada`. Con éxito, se guarda el `id` como `serverId` de la jornada.
2. **`BREAK_START`, `BREAK_END`, `CLOCK_OUT`** → necesitan el `serverId` de su jornada. Si aún no
   lo tiene, se dejan para la siguiente pasada: su entrada todavía no se ha registrado.
3. **Fallo temporal** (sin red, timeout, `5xx`, `429`) → se para la pasada entera. El orden
   importa y lo de detrás no puede adelantarse.
4. **Rechazo definitivo** (D4) → el evento queda `REJECTED` con su código. Si era una `CLOCK_IN`
   que no se pudo adoptar, los demás eventos de esa jornada quedan rechazados en cascada
   (`JORNADA_NO_REGISTRADA`). La pasada **sigue con las demás jornadas** (FR-010).

Disparadores, los mismos que hoy: tras cada fichaje, al recuperar la conexión, un sondeo cada
30 segundos mientras quede algo pendiente y, en Android, WorkManager cada 15 minutos con red. En
iOS no hay envío con la app cerrada: se envía al abrirla o al recuperar la conexión, una
limitación aceptada (FR-007 dice «donde la plataforma lo permita»).

## D3 — Adoptar la jornada abierta del servidor

**Hecho**: `POST /entrada` con otra jornada abierta → `409 FICHAJE_YA_EN_CURSO`. Pasa cuando se
fichó la entrada desde otro dispositivo, o cuando la respuesta de una entrada se perdió y luego se
envió otra con distinto `clientEventId`.

**Decisión**: ante ese código se pide `GET /api/fichajes/empleado/{yo}?desde=hoy-3&hasta=hoy`, se
busca la jornada `EN_CURSO` y se toma su `id` como `serverId` de la jornada local. La entrada
local queda `SYNCED` (adoptada), sin crear nada en el servidor, y los eventos que dependían de
ella continúan (FR-011). Si no se encuentra ninguna jornada en curso, que sería una carrera muy
improbable, se reintenta en la siguiente pasada.

**Una persona con `EMPLEADO`** consulta sus jornadas con `/api/fichajes/empleado/{su id}`: el
listado general `GET /api/fichajes` le devuelve `403`.

## D4 — Fallos temporales y rechazos definitivos

| Respuesta | Clase | Reacción |
|---|---|---|
| Sin red, timeout | temporal | parar la pasada; reintentar |
| `5xx`, `429` | temporal | parar la pasada; reintentar |
| `401 TOKEN_ACCESO_EXPIRADO` | lo resuelve el cliente HTTP (spec 004) | — |
| `409 FICHAJE_YA_EN_CURSO` en entrada | conflicto recuperable | adoptar (D3) |
| `422 DESVIACION_RELOJ` | definitivo | rechazado: «demasiado antiguo» si `occurredAt` < ahora − 72 h; si no, «reloj desajustado» |
| `409 EMPLEADO_INACTIVO` | definitivo | rechazado, y la pasada se para: nada más saldrá |
| `409 PAUSA_YA_ABIERTA`, `PAUSA_NO_ABIERTA`, `PAUSA_ABIERTA_AL_CERRAR`, `FICHAJE_NO_EN_CURSO` | definitivo | rechazado: «transición no válida» |
| `409 CLIENT_EVENT_ID_REUTILIZADO` | definitivo, y es un bug | rechazado y `AppLogger.error` |
| `404 FICHAJE_NOT_FOUND`, `403`, `400` | definitivo | rechazado: «error inesperado» |

El código del servidor se guarda tal cual en `rejectionCode`. La UI lo traduce, y el texto nunca
se guarda en la base de datos.

## D5 — Idempotencia: el cuerpo tiene que ser idéntico al reintentar

**Hecho**: reenviar el mismo `clientEventId` con el mismo cuerpo devuelve la respuesta original.
Con un cuerpo distinto devuelve `409 CLIENT_EVENT_ID_REUTILIZADO`.

**Decisión**:

- el `clientEventId` es el id del evento, generado una sola vez al fichar;
- `occurredAt` se guarda en milisegundos y se formatea siempre igual, ISO-8601 en UTC con
  milisegundos;
- `tipo` sale del evento guardado, nunca de la UI en el momento del envío.

Un test comprueba que dos serializaciones del mismo evento son idénticas byte a byte.

## D6 — Lo que hay en el servidor, guardado en el dispositivo

**Decisión**: una tabla `server_shift` cachea los `FichajeDto` de la persona: `id`, `entrada`,
`salida`, `estado`, `minutosTrabajados`, `fueIncompleto`, y las pausas como JSON. Se refresca:

- al abrir el estado del día y el historial;
- tras cada respuesta de un fichaje, que ya trae el `FichajeDto` completo;
- tras una pasada de envío.

El repositorio **combina**: las jornadas del servidor más las jornadas locales con eventos aún
no enviados. Una jornada local con `serverId` igual al de una del servidor se pinta como la del
servidor, con sus eventos pendientes encima. Sin cobertura, se usa lo último descargado (FR-016).

**Marcas de jornada (FR-017)**: `fueIncompleto` viene en el `FichajeDto`. `corregido` y
`reconstruido` solo vienen en el resumen mensual (`GET …/resumen?anio&mes`, `DiaResumenDto`), que
se pide al abrir el historial de un mes y se cachea igual.

## D7 — Estado del día

Se deriva de la jornada en curso, sea del servidor o local, más los eventos pendientes aplicados
encima en orden. `currentShiftStatus()` se conserva con la misma regla. Ahora también vale para
una jornada abierta en otro dispositivo (FR-018).

## D8 — Migración de la base de datos (versión 2 → 3)

Migración **manual**, porque no se puede derivar de forma automática:

1. Borrar las filas con `employeeId` nulo (FR-023, research D6 de la spec 004).
2. Crear la tabla `shift` y añadir las columnas nuevas a `clock_event`.
3. Para cada persona, recorrer sus eventos por `occurredAt`: cada `CLOCK_IN` abre una jornada
   local nueva y los eventos siguientes se asignan a ella. Los eventos anteriores a la primera
   entrada quedan sin jornada y se rechazan con `JORNADA_NO_REGISTRADA`.
4. `FAILED` pasa a `PENDING`; ya no existe como estado.

La agrupación del paso 3 es una función pura, con test. Se ejecuta en Kotlin justo después de la
migración SQL, porque en SQL sería ilegible.

## D9 — Correcciones

- Solo con conexión (FR-022).
- `POST /api/fichajes/{id}/correcciones` con `motivo` y `valoresPropuestos`: entrada, salida,
  pausas con tipo. Antes de enviar se valida lo mismo que el servidor llama `VALORES_INCOHERENTES`
  (FR-020).
- Las correcciones de una jornada se piden con `GET /api/fichajes/{id}/correcciones` al abrir su
  detalle.
- Solo jornadas `CERRADO` o `INCOMPLETO` (`FICHAJE_NO_FINALIZADO` si no).
- `RequestCorrectionUseCase` y `CorrectionRequestPushDto` existen, pero apuntan a una ruta
  inexistente y no tienen pantalla. Se reescriben.

## D10 — Tipo de pausa

Una hoja inferior con tres opciones (Comida, Descanso, Otro) al pulsar «Iniciar pausa». El tipo
se guarda en el evento `BREAK_START`. En el historial cada pausa muestra su tipo.

## D11 — Sin ubicación

`ubicacion` es opcional en el contrato. No se envía, y no se pide permiso de localización
(FR-024). Añadirla es una feature aparte, con su propia decisión de privacidad.

## D12 — Tests

En `commonTest`:

- **Motor de envío** con `MockEngine` y un DAO en memoria:
  - orden;
  - dependencias de la entrada;
  - adopción ante `FICHAJE_YA_EN_CURSO`;
  - cada fila de la tabla de D4;
  - cascada;
  - que una jornada rechazada no bloquea a las demás;
  - reintentos idempotentes.
- **Serialización estable** (D5).
- **Agrupación de la migración** (D8).
- **Combinación** de servidor y local (D6, D7).
- **Validación de correcciones** (D9).
- **ViewModels**: estado del día, historial, corrección.

## D13 — Verificación contra el backend real

El backend local, como en la spec 004, con dos ajustes por argumento de arranque:

- `--timetracking.reloj.tolerancia-pasado=PT2M`, para probar el rechazo por antigüedad sin
  esperar 72 horas;
- `JWT_EXPIRATION_MINUTES=1`, para que la renovación ocurra durante los envíos.

El rechazo por reloj adelantado se prueba adelantando la hora del emulador más de 5 minutos.

## Resumen

Ninguna incógnita abierta. Sin dependencias nuevas.
