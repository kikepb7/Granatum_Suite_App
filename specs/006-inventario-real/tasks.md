# Tasks: Inventario real contra el backend

**Input**: Design documents from `/specs/006-inventario-real/`
**Tests**: SÍ (research D8).

Rutas bajo `feature/inventory/`; `domain/`, `db/`, `data/` y `ui/` son el `commonMain` de cada
módulo.

## Phase 1: Setup

- [X] T001 Añadir `commonTest` con `libs.kotlinx.coroutines.test` a `domain`, `data` y `presentation` de `feature/inventory`, más `libs.ktor.client.mock` en `data` y `libs.coil.compose` en `presentation`. Solo bloques `dependencies`
- [X] T002 Crear `presentation/src/commonMain/composeResources/values/strings.xml` con todos los textos del inventario, sustituyendo los que hoy están en el código

## Phase 2: Foundational

- [X] T003 [P] Reescribir el dominio en `domain/model/`: `CategoryModel`, `MaterialCondition`, `SizeUnit`, `MaterialSize`, `MaterialModel`, `MaterialDraft`, `HistoryChangeType`, `MaterialHistoryEntry`, `MaterialFilter` e `InventoryError` (`data-model.md`). Borrar `MaterialCategory`, `MaterialStatus` y `StockMovementModel`
- [X] T004 [P] Validaciones en `domain/validation/InventoryValidation.kt` (borrador, ajuste de cantidad y categoría, con las reglas de `data-model.md`) y su test en `domainTest/InventoryValidationTest.kt`
- [X] T005 [P] Filtrado local en `domain/model/MaterialFilter.kt` (`fun List<MaterialModel>.filter(filter)`) y su test
- [X] T006 Reescribir `domain/repository/MaterialRepository.kt` como `InventoryRepository` (observar materiales, uno, categorías e historial; refrescar; crear, editar, ajustar y borrar materiales; crear, editar y borrar categorías) y los casos de uso de `domain/usecase/`
- [X] T007 Base de datos v2 en `db/`: `CategoryEntity`, `MaterialEntity`, `MaterialHistoryEntity` y sus DAOs, `version = 2`. Borrar `StockMovementEntity`. En `data/di`, `fallbackToDestructiveMigrationFrom(true, 1)` (research D2). Versionar `schemas/.../2.json`
- [X] T008 [P] DTOs del contrato en `data/dto/InventoryDtos.kt`, letra por letra con `docs/openapi.json`. Borrar `MaterialDto.kt` y `SaveMaterialRequestDto.kt` antiguos (FR-017)
- [X] T009 `data/remote/InventoryRemoteDataSource.kt` con las rutas de `contracts/inventario-api.md` y los errores tipados de research D5
- [X] T010 `data/repository/OfflineFirstInventoryRepository.kt`: lectura de la caché, refresco que reemplaza, escrituras que actualizan la caché con la respuesta, `404 MATERIAL_NOT_FOUND` que borra de la caché, y sin red sin tocar nada
- [X] T011 Test del repositorio en `dataTest/InventoryRepositoryTest.kt` con `MockEngine` y DAOs en memoria: los nombres de campo del alta y del ajuste; que la edición conserva `fotos`; el 404 que borra; que sin red no escribe; el refresco que reemplaza

## Phase 3: US1 + US2 — Consultar y ajustar (P1) 🎯 MVP

- [X] T012 [US1] Reescribir `ui/list/MaterialListViewModel.kt` y `MaterialListScreen.kt`: lista con disponibles sobre total, estado, ubicación y categoría; búsqueda; filtros por categoría, estado y agotados; refresco al abrir y manual; aviso de datos sin actualizar
- [X] T013 [US1] Reescribir `ui/detail/MaterialDetailViewModel.kt` y `MaterialDetailScreen.kt`: todos los datos, fotos con Coil, historial con tipo, valor anterior → nuevo, motivo y fecha
- [X] T014 [US2] Hoja de ajuste de cantidad en `ui/detail/AdjustQuantitySheet.kt`: nueva cantidad (0..total) y motivo obligatorio, validación local y error claro sin red
- [X] T015 [P] [US1] Tests de `MaterialListViewModel` y del ajuste en el ViewModel del detalle

## Phase 4: US3 — Alta, edición y borrado (P2)

- [X] T016 [US3] Reescribir `ui/form/MaterialFormViewModel.kt` y `MaterialFormScreen.kt` con todos los campos de FR-008: selector de categoría, de estado y de unidad; cantidad disponible solo en el alta; validación por campo; conservar las fotos en la edición
- [X] T017 [US3] Borrar un material desde el detalle con confirmación (`AppDestructiveConfirmationDialog`)
- [X] T018 [P] [US3] Test de `MaterialFormViewModel`

## Phase 5: US4 — Categorías (P3)

- [X] T019 [US4] Pantalla `ui/category/CategoryListScreen.kt` + ViewModel: lista, crear, renombrar y borrar (deshabilitado con explicación si tiene materiales, FR-013)
- [X] T020 [US4] Rutas en `ui/navigation/InventoryGraphRoutes.kt` (categorías, desde la lista)
- [X] T021 [P] [US4] Test del ViewModel de categorías

## Phase 6: Polish

- [X] T022 [P] Sin cadenas visibles en el código de `feature/inventory/presentation` (principio IX) ni rutas `/inventory/` (FR-017)
- [X] T023 Subir el trinquete de cobertura a la cifra medida
- [X] T024 [P] README: sección de inventario
- [X] T025 CI en local completa
- [ ] T026 Quickstart en dispositivo contra el backend local (al final, junto con la spec 005)
