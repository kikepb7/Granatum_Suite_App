# Tasks: Productos del inventario y filtros rediseñados

## Phase 1: Foundation

- [X] T001 Product domain model, location, dimensions, filter and validation in feature/inventory/domain/src/commonMain/kotlin/com/granatum/feature/inventory/domain/model/
- [X] T002 [P] Domain tests (location parsing, filters, sort, validation) in feature/inventory/domain/src/commonTest/
- [X] T003 Storage configuration in build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt and core/data/.../environment/ActiveEnvironment.kt
- [X] T004 `StorageHttpClient` in core/data/.../networking/StorageHttpClient.kt
- [X] T005 Repository mapping and quantity sequence in feature/inventory/data/.../repository/OfflineFirstInventoryRepository.kt
- [X] T006 `SupabaseProductImageStorage` in feature/inventory/data/.../storage/
- [X] T007 [P] Data tests (mapping, legacy fields, PUT/PATCH order, upload) in feature/inventory/data/src/commonTest/

## Phase 2: US1 – find products (P1)

- [X] T008 [P] [US1] Design-system controls in core/designsystem/.../components/controls/ and icons
- [X] T009 [US1] `ProductCard`, `ProductImage`, `LocationPicker`, `InventoryFiltersSheet` in feature/inventory/presentation/.../components/
- [X] T010 [US1] List ViewModel and screen in feature/inventory/presentation/.../list/
- [X] T011 [US1] Coil image loader through `StorageHttpClient` in composeApp/.../App.kt

## Phase 3: US2 – create and edit (P1)

- [X] T012 [US2] Form ViewModel and screen with camera/gallery upload and location plan in feature/inventory/presentation/.../form/

## Phase 4: US3 – quantity with history (P2)

- [X] T013 [US3] Detail ViewModel and screen with stepper, reason and history in feature/inventory/presentation/.../detail/

## Phase 5: Polish

- [X] T014 Routes renamed to `Product*`, DI and composeApp navigation
- [X] T015 [P] ViewModel tests in feature/inventory/presentation/src/commonTest/
- [X] T016 [P] Demo storage routes and seeds in core/demo/
- [X] T017 README and local.properties.example
- [ ] T018 Configure the PRE and PRO storage projects (bucket `inventario`, public, insert policy) and verify uploads on Android and iOS
