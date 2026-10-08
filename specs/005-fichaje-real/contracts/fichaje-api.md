# Contrato — Uso de la API de jornada

**Feature**: `005-fichaje-real` · **Fecha**: 2026-10-08

Formas de `docs/openapi.json` (backend `d1857ad`); códigos y reglas de
`specs/001-timetracking/contracts/` del backend en ese commit. Este documento fija la reacción de
la app; no añade nada al contrato del servidor.

## Envío de fichajes

Cuerpo común: `{ "clientEventId": <id del evento>, "occurredAt": <ISO-8601 UTC, ms> }`. Sin
`ubicacion` (FR-024).

| Evento | Petición | Extra | Respuesta correcta |
|---|---|---|---|
| `CLOCK_IN` | `POST /fichajes/entrada` | — | `201 FichajeDto` → guardar `id` como `serverId` |
| `BREAK_START` | `POST /fichajes/{serverId}/pausa/inicio` | `"tipo"` | `200 FichajeDto` |
| `BREAK_END` | `POST /fichajes/{serverId}/pausa/fin` | — | `200 FichajeDto` |
| `CLOCK_OUT` | `POST /fichajes/{serverId}/salida` | — | `200 FichajeDto` |

Con cualquier respuesta correcta: el evento pasa a `SYNCED` y el `FichajeDto` se guarda en
`server_shift`. Para los errores, ver la tabla de research D4.

**Reintento**: idéntico byte a byte. Un `200` o `201` sobre un evento ya registrado es la
respuesta original y se trata igual (SC-003).

## Adopción (`409 FICHAJE_YA_EN_CURSO`)

`GET /fichajes/empleado/{employeeId}?desde={hoy−3}&hasta={hoy}`. Las fechas son civiles y el
servidor las interpreta en `Europe/Madrid`. Se toma el `id` de la jornada con `estado = EN_CURSO`
como `serverId` de la jornada local, y la entrada local pasa a `SYNCED`.

## Consulta

| Para | Petición | Uso |
|---|---|---|
| Historial y estado del día | `GET /fichajes/empleado/{employeeId}?desde&hasta` | Jornadas con sus pausas → `server_shift` |
| Marcas corregido y reconstruido | `GET /fichajes/empleado/{employeeId}/resumen?anio&mes` | `DiaResumenDto.corregido` y `.reconstruido` por fecha |

Por defecto, el mes en curso. Al navegar a otro mes, se pide ese mes. Sin cobertura, se usa la
caché.

## Correcciones

| Acción | Petición | Errores que la app distingue |
|---|---|---|
| Ver las de una jornada | `GET /fichajes/{serverId}/correcciones` | — |
| Pedir una | `POST /fichajes/{serverId}/correcciones` con `{motivo, valoresPropuestos: {entrada, salida, pausas: [{tipo, inicio, fin}]}}` | `409 FICHAJE_NO_FINALIZADO`, `422 VALORES_INCOHERENTES`, `409 EMPLEADO_INACTIVO`, sin red |

Las pausas propuestas llevan siempre `fin`: el contrato lo exige en `ValoresPausaDto`. Una jornada
solo se puede corregir si el servidor ya la conoce, es decir, si tiene `serverId`.

## Lo que se retira

`/attendance/events`, `/attendance/corrections` y sus DTOs (`ClockEventPushDto`, `ClockEventDto`,
`CorrectionRequestPushDto`) (FR-025). `/attendance/team-summary` lo usa la pantalla de equipo y se
reescribe en la fase de equipo; aquí no se toca.
