# Implementation Plan: Fichaje real contra el backend

**Branch**: `fichaje-feature` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-fichaje-real/spec.md`

## Summary

Los fichajes pasan de enviarse como eventos sueltos a una ruta que no existe, a registrarse como
jornadas en `/api/fichajes`. El dispositivo sigue guardando cada acción al instante y la agrupa en
una jornada local; un motor de envío las manda en orden, espera el identificador de la jornada
antes de enviar lo que depende de ella, adopta la jornada del servidor si ya existe y separa los
fallos temporales de los rechazos definitivos.

El estado del día y el historial combinan lo que consta en el servidor con lo pendiente. Pedir
una corrección tiene por fin pantalla. Ver [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 17, Gradle 8.14.3

**Primary Dependencies**: Ktor 3.2.3, Room 2.7.2, Koin 4.1.0, Compose Multiplatform,
kotlinx-datetime 0.7.1, WorkManager (Android, ya en el proyecto). Sin dependencias nuevas.

**Storage**: Room, base de `feature/clockin` de la versión 2 a la 3 (tablas `shift` y
`server_shift`, columnas nuevas en `clock_event`).

**Testing**: `commonTest` con `MockEngine` y DAOs en memoria, en JVM y en el simulador de iOS;
quickstart contra el backend local.

**Target Platform**: Android e iOS.

**Project Type**: aplicación móvil KMP.

**Performance Goals**: el estado del día cambia en menos de 0,5 s al pulsar (SC-006); una cola
pendiente se vacía en menos de 1 minuto con conexión (SC-002).

**Constraints**: offline-first; idempotencia byte a byte (research D5); tolerancia del servidor
de +5 minutos y −72 horas; el servidor es la fuente de verdad.

**Scale/Scope**: 4 módulos de `feature/clockin`, 2 pantallas reescritas (estado del día e
historial), 2 nuevas (detalle de jornada y corrección), 1 migración.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluado contra la constitución **v1.1.2**.

| Principio | Estado |
|---|---|
| **I** Arquitectura por módulos | ✅ todo dentro de `feature/clockin/{domain,database,data,presentation}` |
| **II** commonMain | ✅ solo WorkManager y conectividad siguen en código de plataforma, como ya estaban |
| **III** Offline-first | ✅ es el centro de la feature: cada fichaje se guarda primero en local |
| **IV** Errores explícitos | ✅ `ClockRejection` y los errores de corrección en el dominio, texto solo en presentation |
| **V** Secretos | ✅ nada nuevo |
| **VI** El contrato manda | ✅ rutas, cuerpos y códigos de `docs/openapi.json` y del contrato de jornada en `d1857ad` ([contracts/fichaje-api.md](./contracts/fichaje-api.md)) |
| **VII** Credenciales | ✅ no aplica |
| **VIII** Tests y CI | ✅ el motor de envío es la pieza más probada (D12); el trinquete sube |
| **IX** UI en español y accesible | ✅ pantallas nuevas y reescritas con textos en recursos; se migran los textos en código que quedan en `feature/clockin/presentation` |
| **X** Documentada | ✅ |
| **XI** Build | ✅ sin plugins ni dependencias nuevas |
| **XII** Paridad | ⚠️ con matiz: en iOS no hay envío con la app cerrada. Se envía al abrir la app o al recuperar la conexión. Limitación de la plataforma, prevista en FR-007 |

**Resultado: PASA.** El matiz de XII está documentado y es aceptable: un fichaje nunca se pierde;
solo se envía más tarde.

## Project Structure

```text
specs/005-fichaje-real/
├── plan.md · research.md · data-model.md · quickstart.md
├── contracts/fichaje-api.md
├── checklists/requirements.md
└── tasks.md

feature/clockin/
├── domain/        # BreakType, ShiftModel, ClockRejection, CorrectionModel/Draft + validación,
│                  # repositorio y casos de uso adaptados
├── database/      # versión 3: ShiftEntity, ServerShiftEntity, columnas nuevas en
│                  # ClockEventEntity, migración manual 2→3, DAOs
├── data/          # DTOs del contrato, ShiftSyncEngine (sustituye a ClockEventSyncManager),
│                  # adopción, caché del servidor, repositorio combinado, correcciones
└── presentation/  # estado del día con hoja de tipo de pausa, historial por mes, detalle de
                   # jornada, pantalla de corrección, textos en composeResources
```

**Structure Decision**: todo vive dentro de la feature existente. `ClockEventSyncManager` se
sustituye por `ShiftSyncEngine` con la misma forma de arranque: conectividad, sondeo y llamada
tras cada fichaje. Así `ClockSyncWorker` y el arranque en Koin apenas cambian.

## Fases

1. **Dominio y base de datos**: modelos, versión 3, migración y su agrupación con test.
2. **Motor de envío**: DTOs, serialización estable, envío en orden, adopción, clasificación de
   errores y cascada. Todo con tests primero.
3. **Lectura**: caché del servidor, combinación con lo local, estado del día e historial.
4. **Pantallas**: tipo de pausa, historial por mes, detalle, rechazos con su texto, migración de
   los textos de `feature/clockin/presentation` a recursos.
5. **Correcciones**: validación, envío y estado.
6. **Verificación**: quickstart contra el backend local, trinquete y README.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Una serialización no estable produce `409 CLIENT_EVENT_ID_REUTILIZADO` al reintentar | Test byte a byte (D5); `occurredAt` en ms con formato fijo |
| La migración manual pierde fichajes con dueño | Agrupación como función pura con tests; escenario 10 del quickstart con una base real de la versión 2 |
| Las fechas civiles en `Europe/Madrid` descuadran el historial en el cambio de hora | Usar `TimeZone.of("Europe/Madrid")` para construir `desde`/`hasta`, igual que el servidor |
| Adopción en carrera (ninguna jornada en curso visible todavía) | Reintentar en la siguiente pasada (D3) |
| Mucho cambio en `feature/clockin` a la vez | Fases con tests verdes al final de cada una y commits por fase |

## Complexity Tracking

Sin desviaciones.
