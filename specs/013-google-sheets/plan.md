# Implementation Plan: Google Drive (hojas de fichajes y recibos) y apellidos

**Branch**: `invoice-ml-feature` | **Date**: 2026-10-18 | **Spec**: [spec.md](spec.md)

## Summary

La app añade los apellidos en personal y en el registro de la propietaria, y una
pantalla «Google Drive», solo para `ADMIN`, desde el menú de cuenta. En ella se
configuran las dos hojas, se exportan los fichajes de un mes, se resincronizan los
recibos y se ve el estado de la copia.

Todo habla con las rutas de la spec 012 del backend: `/api/sheets/**`, `lastName`,
`lastNamePending`. El entorno Demo las imita. La app no tiene credenciales de
Google.

## Technical Context

**Language/Version**: Kotlin Multiplatform (Android + iOS), Compose Multiplatform

**Primary Dependencies**: Koin, Ktor, kotlinx.serialization, kotlinx.datetime, el
design system propio, Turbine y ktor-client-mock en test. Sin dependencias nuevas.

**Storage**: ninguno nuevo. Sin Room para esta feature (ver Complexity Tracking).

**Testing**: kotlin.test en commonTest (dominio, data con MockEngine,
ViewModels con Turbine), tests de `core/demo`, Kover con el trinquete vigente.

**Target Platform**: Android e iOS (paridad, principio XII)

**Project Type**: app móvil KMP

**Performance Goals**: el resultado de la exportación en menos de 1 minuto. Se
consulta la tarea cada 2 s, como mucho 30 veces; después se indica que queda en
cola.

**Constraints**: textos en español en composeResources; sin credenciales;
contrato fijado con `scripts/sync-openapi.sh`.

**Scale/Scope**: un módulo de feature nuevo (`feature/sheets`) y cambios en
`feature/staff`, `feature/auth`, `core` y `core/demo`.

## Constitution Check

| Principio | Cómo se cumple | Estado |
|---|---|---|
| I Arquitectura por módulos | `feature/sheets/{domain,data,presentation}` como `feature/staff`; `presentation → domain ← data`; dominio Kotlin puro | ✅ |
| II commonMain | Todo en commonMain: copiar al portapapeles con `LocalClipboard` y abrir enlaces con `LocalUriHandler` de Compose; sin `expect/actual` nuevos | ✅ |
| III Offline-first | Matiz justificado, como `feature/staff` (009 D3) y facturación: configuración y exportación son operaciones del servidor, solo `ADMIN` y sin valor sin conexión. El ViewModel pasa por casos de uso y repositorio, nunca por una llamada HTTP directa | ✅ con matiz |
| IV Errores explícitos | `Result` con errores tipados: `SheetsError.NotAccessible(field, reason)`, `SameSpreadsheet`, `NotConfigured`, `Unavailable`, `Unreachable`, `TaskNotFound` y red; los mensajes salen de `toUiText()` | ✅ |
| V Secretos por entorno | No hay secretos nuevos; la app nunca ve credenciales de Google (FR-009) | ✅ |
| VI Contrato | Se fija `docs/openapi.json` al commit del backend que publica la spec 012, antes de escribir los DTOs; nada se inventa | ✅ |
| VII Almacenamiento seguro | Sin cambios | ✅ |
| VIII Tests | Tests por capa antes de implementar; el trinquete de Kover no baja | ✅ |
| IX Design system en español | Componentes `App*`, strings en composeResources, `contentDescription` en los iconos | ✅ |
| X Documentada | Esta spec, plan y tareas; README de la app | ✅ |
| XI Build | Módulos nuevos con los convention plugins; sus `build.gradle.kts` solo declaran dependencias | ✅ |
| XII Paridad | Se prueba en Demo en Android (compilación y tests) y en iOS (simulador propio) | ✅ |

## Project Structure

```text
specs/013-google-sheets/{spec.md, plan.md, tasks.md, checklists/requirements.md}

feature/sheets/
├── domain/   SheetsSettings, SheetTask (kind, status), SheetsStatus, SheetsError,
│             SheetsRepository, casos de uso (observe/save settings, exportShiftMonth,
│             resyncInvoiceMonth, awaitTask, status, retry, retryAllFailed)
├── data/     DTOs del contrato, SheetsRemoteDataSource (Ktor), SheetsRepositoryImpl,
│             mapeo de errores {code, spreadsheet, reason}
└── presentation/
              GoogleDriveScreen + GoogleDriveViewModel (configuración, exportar,
              resincronizar, estado), componentes, strings, navegación
              (SheetsGraphRoutes, sheetsGraph)

feature/staff/      domain (StaffMember.lastName/lastNamePending, StaffEdit/Onboarding.lastName),
                    data (DTOs), presentation (campos «Apellidos», nombre completo,
                    aviso «Faltan los apellidos»)
feature/auth/       registro de la propietaria con «Apellidos»
core/data/          SignUpRequestDto.lastName
core/demo/          DemoSheetsRoutes (settings, export, resync, tasks, status, retry),
                    lastName en DemoStaffRoutes, DemoAuthRoutes y DemoState
composeApp/         entrada «Google Drive» en AccountScreen (solo ADMIN), ruta y
                    módulos Koin
settings.gradle.kts include de los tres módulos
```

## Complexity Tracking

| Desviación | Por qué | Alternativa descartada |
|---|---|---|
| Sin Room en `feature/sheets` (III) | La configuración y el estado viven en el servidor y solo los usa el `ADMIN` en línea. Cachearlos mostraría un estado falso de una cola que cambia sola. Mismo criterio que `feature/staff` (009 D3) | Caché en Room: añade una base y migraciones sin ningún caso de uso sin conexión |
