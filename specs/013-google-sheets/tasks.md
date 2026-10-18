---
description: "Tareas de la feature 013: Google Drive y apellidos (app)"
---

# Tasks: Google Drive (hojas de fichajes y recibos) y apellidos

**Input**: `specs/013-google-sheets/` y el contrato de la spec 012 del backend.
**Tests**: requeridos; en cada fase van antes que la implementación (principio VIII).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [ ] T001 Fijar el contrato: `scripts/sync-openapi.sh <sha completo>` con el commit del backend
  publicado en `invoice-ml-feature` que incluye la spec 012, para actualizar `docs/openapi.json`
  y `docs/openapi.pin`. Copiar `docs/english-naming.md` del backend si cambió.
- [ ] T002 Crear `feature/sheets/{domain,data,presentation}`:
  - `build.gradle.kts` copiados de `feature/staff`, cambiando las dependencias de proyecto;
  - los tres módulos incluidos en `settings.gradle.kts`;
  - dependencias en `composeApp/build.gradle.kts`.

## Phase 2: User Story 1 - Nombre y apellidos (P1) 🎯 MVP

- [ ] T003 [P] [US1] Tests de `feature/staff`:
  - data: MockEngine con `lastName` y `lastNamePending` en la lista, el detalle, la edición y el
    onboarding (cuerpo enviado con `lastName`);
  - presentation: validación «Escribe los apellidos», nombre completo y aviso de pendientes.
- [ ] T004 [P] [US1] Tests del registro de la propietaria (`feature/auth/presentation`, y
  `core/data` `KtorAuthRepositoryImpl`): pide y envía `lastName`.
- [ ] T005 [US1] Dominio de staff:
  - `StaffMember(lastName: String?, lastNamePending: Boolean)`, más un `fullName`;
  - `StaffEdit.lastName` y `Onboarding.lastName`;
  - `StaffLimits` con un máximo de 150.
- [ ] T006 [US1] Data de staff: DTOs y mappers según `docs/openapi.json` (`EmployeeDto`,
  `UpdateEmployeeRequestDto`, `OnboardingRequestDto`).
- [ ] T007 [US1] Presentación de staff:
  - campo «Apellidos» en el onboarding y la edición;
  - nombre completo en la lista y la ficha;
  - chip «Faltan los apellidos»;
  - strings en composeResources.
- [ ] T008 [US1] Registro de la propietaria: `SignUpRequestDto.lastName` en `core/data`, y el campo
  y la validación en `feature/auth/presentation/signup`.
- [ ] T009 [US1] `core/demo`: `lastName` en `DemoState` (personas sembradas con apellidos, y una
  antigua sin ellos), en `DemoStaffRoutes` (GET, POST, PUT, onboarding, con validación `400`
  sin `lastName`) y en `DemoAuthRoutes` (sign-up). Actualizar sus tests.

## Phase 3: User Story 2 y 3 - Configurar y exportar (P1)

- [ ] T010 [P] [US2] Tests de dominio y data de `feature/sheets` con MockEngine:
  - **settings**: GET (disponible o no), PUT `200`, y los errores:
    - `422 SPREADSHEET_NOT_ACCESSIBLE`, mapeado a `NotAccessible(field, reason)`;
    - `409 SAME_SPREADSHEET`, `503 SHEETS_UNAVAILABLE`, `502 SHEETS_UNREACHABLE`;
  - **exportación**: POST de exportación → `202 taskId`; `409 SHEETS_NOT_CONFIGURED`;
    `400 INVALID_PERIOD`;
  - **tareas**: `GET /tasks/{id}`;
  - **`awaitTask`**: consulta hasta `DONE` o `FAILED`, o se rinde tras N intentos y devuelve
    `StillQueued`.
- [ ] T011 [US2] Implementar el dominio (modelos, `SheetsError`, `SheetsRepository`, casos de uso)
  y la data (DTOs según el contrato fijado, `SheetsRemoteDataSource`, `SheetsRepositoryImpl`,
  mapeo de errores) en `feature/sheets`. Hacer pasar T010.
- [ ] T012 [P] [US2] Tests de `GoogleDriveViewModel` (Turbine):
  - **carga**: no disponible; disponible sin hojas; configurada;
  - **guardar**: con éxito, con error por campo, con hojas iguales;
  - **copiar el correo**: emite un evento de UI;
  - **exportar**: progreso → resultado con filas y apellidos pendientes; en cola;
    `NotConfigured`;
  - **meses**: el selector no ofrece meses futuros.
- [ ] T013 [US2] Implementar `GoogleDriveScreen` y `GoogleDriveViewModel` en
  `feature/sheets/presentation`:
  - secciones de configuración (correo con copiar, instrucciones, dos campos de enlace, guardar,
    abrir hoja) y de exportación (selector de mes, botón «Exportar a Google Drive», resultado);
  - estados, strings y previews;
  - navegación con `SheetsGraphRoutes` y `sheetsGraph`.

  Hacer pasar T012.
- [ ] T014 [US2] `composeApp`:
  - entrada «Google Drive» en `AccountScreen`, solo con `UserRole.ADMIN`, que navega a la
    pantalla;
  - módulos Koin en la inicialización;
  - test de que otros roles no ven la entrada (`AccountStateTest`).
- [ ] T015 [US2] `core/demo`: `DemoSheetsRoutes` imitando `/api/sheets/**` según el contrato:
  - un correo de cuenta de demostración;
  - un enlace que contenga `no-compartida` responde `NOT_SHARED`;
  - la exportación crea una tarea que termina `DONE` en la siguiente consulta, con las filas
    calculadas de los fichajes demo del mes y los apellidos pendientes;
  - registrada en `DemoBackend`, con tests.

## Phase 4: User Story 4 - Estado y reintentos (P2)

- [ ] T016 [P] [US4] Tests:
  - data y ViewModel del estado (última copia, pendientes y fallidas con motivo traducido);
  - reintentar una o todas;
  - resincronizar un mes de recibos.
- [ ] T017 [US4] Implementar la sección de estado y resincronización en `GoogleDriveScreen`, con los
  motivos traducidos: `NOT_SHARED`, `READ_ONLY`, `NOT_FOUND`, transitorio y sin manejador. Hacer
  pasar T016.
- [ ] T018 [US4] `core/demo`: estado con una tarea fallida sembrada («hoja de recibos sin permiso
  de edición»), reintentos y resincronización.

## Phase 5: Polish

- [ ] T019 `./gradlew ktlintCheck testDebugUnitTest :koverXmlReport :koverVerify :composeApp:assembleDebug`
  en verde.
- [ ] T020 Comprobación PRO: `:composeApp:assembleRelease -Pgranatum.build=pro
  -PBASE_URL_HTTP_PRO=https://pro.ci.invalid/api`, sin `com/granatum/core/demo/` en el dex.
- [ ] T021 iOS: `iosSimulatorArm64Test` y `:composeApp:linkDebugFrameworkIosSimulatorArm64`.
  Recorrer el quickstart en Demo en el simulador propio (iPhone 17 Pro) y apagarlo después.
- [ ] T022 README de la app (sección Google Drive) y CHANGELOG si existe; marcar las tareas.

## Dependencies

- T001 bloquea todo lo que toca DTOs.
- US1 es independiente de las fases 3 y 4.
- US2/US3 antes que US4.
- Polish al final.
