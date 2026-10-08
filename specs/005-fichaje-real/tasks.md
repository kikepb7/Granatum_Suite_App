# Tasks: Fichaje real contra el backend

**Input**: Design documents from `/specs/005-fichaje-real/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/fichaje-api.md, quickstart.md

**Tests**: SÍ (research D12). Cada test antes de su implementación.

Rutas abreviadas, todas bajo `feature/clockin/`:

- `domain/` = `domain/src/commonMain/kotlin/com/granatum/feature/clockin/domain/`
- `db/` = `database/src/commonMain/kotlin/com/granatum/feature/clockin/database/`
- `data/` = `data/src/commonMain/kotlin/com/granatum/feature/clockin/data/`
- `ui/` = `presentation/src/commonMain/kotlin/com/granatum/feature/clockin/presentation/`
- `*Test/` = el `commonTest` equivalente

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [ ] T001 Añadir `commonTest` con `libs.kotlinx.coroutines.test` a `feature/clockin/domain/build.gradle.kts` y `feature/clockin/presentation/build.gradle.kts` (en `data` ya están desde la spec 004). Solo bloques `dependencies`
- [ ] T002 Crear `feature/clockin/presentation/src/commonMain/composeResources/values/strings.xml` con los textos que hoy están en el código de `ui/` (46 cadenas entre `DynamicString` y `Text("…")`, contadas en la spec 004) más los nuevos de esta feature: tipos de pausa, estados de jornada, marcas, rechazos de `data-model.md` y correcciones

---

## Phase 2: Foundational

### Dominio

- [ ] T003 [P] Crear `domain/model/BreakType.kt` (`COMIDA, DESCANSO, OTRO`), `domain/model/ShiftModel.kt` y `BreakModel` con los campos de `data-model.md`
- [ ] T004 [P] Crear `domain/model/ClockRejection.kt` (`TooOld, ClockSkew, Inactive, InvalidTransition, ShiftNotRegistered, Unexpected`) con `fun fromServerCode(code: String, occurredAt: Instant, now: Instant)`. «Más de 72 h» decide entre `TooOld` y `ClockSkew` (research D4)
- [ ] T005 [P] Test de `ClockRejection.fromServerCode` en `domainTest/ClockRejectionTest.kt`: cada fila de la tabla de `data-model.md`, más el límite de 72 h
- [ ] T006 [P] Crear `domain/model/Correction.kt` (`CorrectionModel`, `CorrectionDraft`, `CorrectionValidation`) y un test en `domainTest/CorrectionValidationTest.kt` con las cinco reglas de `data-model.md` (FR-020)
- [ ] T007 Ampliar `SyncState` en `domain/model/ClockEventModel.kt` con `REJECTED`, quitar `FAILED`, y añadir a `ClockEventModel` `breakType` y `rejection`. Reescribir `domain/repository/ClockInRepository.kt`: `observeToday(): Flow<TodayState>`, `observeMonth(year, month): Flow<List<ShiftModel>>`, `refreshMonth(year, month)`, `startBreak(type: BreakType)`, `observeCorrections(shiftServerId)`, `requestCorrection(shiftServerId, draft)`. Ajustar los casos de uso de `domain/usecase/`

### Base de datos

- [ ] T008 [P] Crear `db/entity/ShiftEntity.kt` y `db/entity/ServerShiftEntity.kt`, y añadir a `ClockEventEntity` los campos `shiftLocalId`, `breakType` y `rejectionCode`. `employeeId` deja de admitir nulos (`data-model.md`)
- [ ] T009 [P] Crear `db/dao/ShiftDao.kt` y `db/dao/ServerShiftDao.kt`, y ajustar `ClockEventDao.kt`: pendientes por persona en orden, eventos por jornada, `markRejected(id, code)` y `markSyncedInShift`
- [ ] T010 Test de la agrupación de la migración en `dbTest/LegacyShiftGroupingTest.kt`: cada `CLOCK_IN` abre jornada; los eventos anteriores a la primera entrada quedan huérfanos; varias personas no se mezclan; el orden es por `occurredAt` (research D8)
- [ ] T011 Implementar la agrupación como función pura en `db/migration/LegacyShiftGrouping.kt`, y la `Migration(2, 3)` en `db/migration/Migration2To3.kt`: borrar las filas sin dueño, crear tablas y columnas, pasar `FAILED` a `PENDING`. La agrupación se ejecuta en el callback posterior a la migración. Subir `AppClockInDatabase` a la versión 3, registrar la migración y versionar `schemas/.../3.json`. Hace pasar T010

**Checkpoint**: dominio y base de datos compilan, y T005, T006 y T010 pasan en JVM e iOS.

---

## Phase 3: User Story 1 + 2 — Registrar, con y sin cobertura (P1) 🎯 MVP

### Tests (primero)

- [ ] T012 [P] [US1] Test de serialización estable en `dataTest/FichajeRequestSerializationTest.kt`: el mismo evento serializado dos veces da los mismos bytes; `occurredAt` en UTC con milisegundos; `tipo` solo en `BREAK_START` (research D5)
- [ ] T013 [US2] Test del motor en `dataTest/ShiftSyncEngineTest.kt` con `MockEngine` y DAOs en memoria:
  - (a) jornada completa en orden;
  - (b) la pausa espera al `serverId` de su entrada;
  - (c) un fallo temporal (sin red, `503`, `429`) para la pasada y conserva todo `PENDING`;
  - (d) `409 FICHAJE_YA_EN_CURSO` → adopción por `GET /fichajes/empleado/{id}` y la pausa siguiente va a esa jornada;
  - (e) `422 DESVIACION_RELOJ` → `REJECTED` en cascada en su jornada, y otra jornada sigue enviándose;
  - (f) `409 EMPLEADO_INACTIVO` para todo;
  - (g) un reintento tras una respuesta perdida no duplica (`requestHistory` igual que en el primer intento);
  - (h) solo envía los de la persona con sesión.

### Implementación

- [ ] T014 [P] [US1] Crear los DTOs del contrato en `data/dto/FichajeDtos.kt`: `EntradaRequestDto`, `InicioPausaRequestDto`, `FinPausaRequestDto`, `SalidaRequestDto`, `FichajeDto`, `PausaDto`. Nombres letra por letra con `docs/openapi.json`. Borrar `ClockEventDto.kt` (FR-025). Hace pasar T012
- [ ] T015 [US2] Crear `data/sync/ShiftSyncEngine.kt` con el algoritmo de research D2 y D4, la adopción de D3 y el guardado de cada `FichajeDto` en `server_shift`. Sustituye a `ClockEventSyncManager.kt`, que se borra. Mismos disparadores: `start(scope)` con conectividad y sondeo, y `syncNow()`. Hace pasar T013
- [ ] T016 [US2] Ajustar `data/di/ClockInDataModule.kt`, `androidMain/.../sync/ClockSyncWorker.kt` y `ClockSyncScheduler.kt` para usar `ShiftSyncEngine`
- [ ] T017 [US1] Reescribir las escrituras de `data/datasource/local/OfflineFirstClockInRepositoryImpl.kt`: `clockIn` crea la `ShiftEntity` y su evento; `startBreak(type)`, `endBreak` y `clockOut` añaden eventos a la jornada en curso, sea local o adoptada del servidor; transiciones de FR-004; `syncNow` sin esperar, en segundo plano

**Checkpoint**: escenarios 1, 2 y 3 del quickstart contra el backend real.

---

## Phase 4: User Story 3 — Ver mi jornada real (P2)

- [ ] T018 [P] [US3] Test de combinación en `dataTest/ShiftMergeTest.kt`: servidor solo; local solo; la misma jornada con `serverId` y eventos pendientes encima; minutos del servidor frente a la estimación provisional; jornada abierta solo en el servidor que hay que continuar (FR-018)
- [ ] T019 [US3] Crear `data/merge/ShiftMerger.kt` (función pura), que hace pasar T018
- [ ] T020 [US3] Crear `data/datasource/remote/FichajeRemoteDataSource.kt`: `GET /fichajes/empleado/{id}?desde&hasta` con las fechas construidas en `TimeZone.of("Europe/Madrid")` y `GET …/resumen?anio&mes`. Guarda en `ServerShiftDao` las jornadas y las marcas `corregido` y `reconstruido` (research D6)
- [ ] T021 [US3] Completar la lectura en `OfflineFirstClockInRepositoryImpl.kt`: `observeToday()` y `observeMonth()` con `ShiftMerger` sobre `server_shift` y los eventos locales; `refreshMonth()` que no falla sin red (FR-016)
- [ ] T022 [US3] Reescribir `ui/clockin/ClockInViewModel.kt` y `ClockInScreen.kt` sobre `TodayState`: jornada en curso local o del servidor, pausas con su tipo, eventos pendientes y rechazados, refresco al abrir; textos de `strings.xml`
- [ ] T023 [US3] Reescribir `ui/history/AttendanceHistoryViewModel.kt` y `AttendanceHistoryScreen.kt`: mes en curso con navegación a meses anteriores, cada día con entrada, pausas, salida y minutos (provisional si aplica), y marcas de incompleta, corregida o reconstruida; textos de `strings.xml`
- [ ] T024 [P] [US3] Tests de `ClockInViewModel` y `AttendanceHistoryViewModel` en `ui/…Test` con repositorios falsos

---

## Phase 5: User Story 4 — Rechazos visibles (P2)

- [ ] T025 [US4] Hoja inferior del tipo de pausa en `ui/clockin/BreakTypeSheet.kt` (`COMIDA`, `DESCANSO`, `OTRO`, accesible), conectada a `startBreak(type)` (FR-003)
- [ ] T026 [US4] En el estado del día y en el historial, cada evento rechazado muestra el texto de `ClockRejection` (`ui/mapper/ClockErrorMappers.kt`, reescrito sobre recursos). En `TooOld`, un botón «Pedir corrección» lleva al detalle de esa jornada si tiene `serverId` (FR-012, FR-013)
- [ ] T027 [US4] Aviso persistente en el estado del día cuando hay algún `ClockSkew`: «la hora de este dispositivo no es correcta»
- [ ] T028 [US4] Impedir la salida con una pausa abierta: el botón deshabilitado con su explicación (FR-004)

---

## Phase 6: User Story 5 — Correcciones (P3)

- [ ] T029 [P] [US5] DTOs en `data/dto/CorreccionDtos.kt` (`CorreccionDto`, `CrearCorreccionRequestDto`, `ValoresFichajeDto`, `ValoresPausaDto`) y `data/datasource/remote/CorreccionRemoteDataSource.kt` (`GET` y `POST /fichajes/{id}/correcciones`), con errores tipados (`FICHAJE_NO_FINALIZADO`, `VALORES_INCOHERENTES`, `EMPLEADO_INACTIVO`, sin red). Borrar `CorrectionRequestPushDto` (FR-025)
- [ ] T030 [P] [US5] Test del envío de correcciones en `dataTest/CorreccionRemoteDataSourceTest.kt` con `MockEngine`
- [ ] T031 [US5] Pantalla de detalle de jornada en `ui/shift/ShiftDetailScreen.kt` + ViewModel: jornada completa, sus correcciones con estado y motivo de resolución, y botón «Pedir corrección» solo si está cerrada o incompleta y tiene `serverId` (FR-019, FR-021)
- [ ] T032 [US5] Pantalla de corrección en `ui/correction/CorrectionScreen.kt` + ViewModel: valores iniciales de la jornada, editar entrada, salida y pausas con su tipo, motivo obligatorio, validación local antes de enviar y error claro sin red (FR-020, FR-022)
- [ ] T033 [P] [US5] Test de `CorrectionViewModel` en `ui/…Test`
- [ ] T034 [US5] Rutas del detalle y de la corrección en `ui/navigation/ClockInGraphRoutes.kt`, desde el historial y desde un rechazo `TooOld`

---

## Phase 7: Polish & Verification

- [ ] T035 [P] Comprobar que no queda ninguna cadena visible en el código de `feature/clockin/presentation` (principio IX) ni ninguna referencia a `/attendance/events` o `/attendance/corrections` (FR-025)
- [ ] T036 [P] Sustituir en `feature/clockin` los usos obsoletos de `kotlinx.datetime.Instant` y `Clock` por `kotlin.time`
- [ ] T037 Levantar el backend local en el 8090 con `--timetracking.reloj.tolerancia-pasado=PT2M` y `JWT_EXPIRATION_MINUTES=1`, sin tocar el 8080 ni los contenedores de Squadfy
- [ ] T038 Recorrer los 10 escenarios del quickstart en Android y anotar el resultado
- [ ] T039 Recorrer en el simulador de iOS lo que se pueda sin teclear credenciales, y dejar el resto para la prueba manual, como en la spec 004
- [ ] T040 Subir el trinquete de cobertura a la cifra medida, redondeada hacia abajo
- [ ] T041 [P] README: fichaje sin cobertura, tipos de pausa, correcciones y la limitación de iOS sin envío en segundo plano
- [ ] T042 CI en local completa: `ktlintCheck testDebugUnitTest koverVerify :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test`

---

## Dependencies & Execution Order

- Setup → Foundational → US1+US2 (MVP) → US3 → US4 → US5 → Polish.
- US4 necesita las pantallas de US3; US5 necesita el detalle de jornada (T031), que se abre desde
  el historial de US3.

## Parallel Opportunities

- Foundational: T003–T006 en paralelo; T008 y T009 en paralelo.
- US1+US2: T012 y T014 en paralelo con el diseño de T013.
- US5: T029, T030 y T033 en paralelo.

## Implementation Strategy

MVP = fases 1 a 3: los fichajes llegan al servidor, con y sin cobertura. Es lo que hoy falta y
tiene valor legal; todo lo demás se construye encima.
