# Tasks: Productos del inventario y filtros rediseñados

## Phase 1: Foundation

- [X] T001 Product domain model, location, dimensions, filter and validation in feature/inventory/domain/src/commonMain/kotlin/com/granatum/feature/inventory/domain/model/
- [X] T002 [P] Domain tests (location parsing, filters, sort, validation) in feature/inventory/domain/src/commonTest/
- [X] T003 Backend 010 DTOs and Room v3 cache in feature/inventory/data/.../dto/ and feature/inventory/database/
- [X] T004 `ImageHttpClient` in core/data/.../networking/ImageHttpClient.kt
- [X] T005 Repository against backend 010 in feature/inventory/data/.../repository/OfflineFirstInventoryRepository.kt
- [X] T006 Multipart image upload and removal in feature/inventory/data/.../remote/InventoryRemoteDataSource.kt
- [X] T007 [P] Data tests (bodies, reason, multipart, image errors) in feature/inventory/data/src/commonTest/

## Phase 2: US1 – find products (P1)

- [X] T008 [P] [US1] Design-system controls in core/designsystem/.../components/controls/ and icons
- [X] T009 [US1] `ProductCard`, `ProductImage`, `LocationPicker`, `InventoryFiltersSheet` in feature/inventory/presentation/.../components/
- [X] T010 [US1] List ViewModel and screen in feature/inventory/presentation/.../list/
- [X] T011 [US1] Coil image loader through `ImageHttpClient` in composeApp/.../App.kt

## Phase 3: US2 – create and edit (P1)

- [X] T012 [US2] Form ViewModel and screen: save the product, then upload the image; location plan, in feature/inventory/presentation/.../form/

## Phase 4: US3 – quantity with history (P2)

- [X] T013 [US3] Detail ViewModel and screen with stepper, reason and history in feature/inventory/presentation/.../detail/

## Phase 5: Polish

- [X] T014 Routes renamed to `Product*`, DI and composeApp navigation
- [X] T015 [P] ViewModel tests in feature/inventory/presentation/src/commonTest/
- [X] T016 [P] Demo inventory routes of backend 010 with images in core/demo/
- [X] T017 README and local.properties.example
- [ ] T018 Update docs/openapi.json with scripts/sync-openapi.sh once backend 010 is committed
- [ ] T019 Verify on Android and iOS against the local backend with storage configured
