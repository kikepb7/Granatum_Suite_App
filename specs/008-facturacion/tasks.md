# Tasks: Facturación contra el backend

**Input**: Design documents from `/specs/008-facturacion/`
**Tests**: SÍ (research D13).

Rutas bajo `feature/invoicing/`; `domain/`, `data/` y `ui/` son el `commonMain` de cada módulo.

## Phase 1: Setup

- [X] T001 Crear los módulos `feature/invoicing/{domain,data,presentation}` (convenciones `kmp.library` y `cmp.feature`, `commonTest` con coroutines-test, `ktor-client-mock` en data, `coil-compose` en presentation) e incluirlos en `settings.gradle.kts` y en `composeApp`
- [X] T002 Crear `presentation/src/commonMain/composeResources/values/strings.xml` con todos los textos de facturación

## Phase 2: Foundational

- [X] T003 [P] `core/presentation/documentpicker`: `expect fun rememberDocumentPicker(onResult)` con `launch(DocumentSource.CAMERA|GALLERY|FILES)`; Android (`TakePicture` + `FileProvider`, `PickMultipleVisualMedia`, `OpenMultipleDocuments`) e iOS (`UIImagePickerController`, `PHPicker`, `UIDocumentPicker`); conversión a JPEG de HEIC/otros y reducción de imágenes de más de 10 MB (research D7). `NSCameraUsageDescription` en `Info.plist`
- [X] T004 [P] `core/presentation/fileopener`: `rememberFileOpener()` con `open(bytes, name, mime)` y `share(...)`; Android con `FileProvider` y `ACTION_VIEW`/`ACTION_SEND`, iOS con `UIActivityViewController` (research D8)
- [X] T005 [P] Dominio en `domain/model/`: `Money`, `Invoice`, `VatLine`, `Party`, `InvoiceDraft`, `InvoiceSummary`, `Page`, `InvoiceFilter`, `UploadDocument`, `UploadResult`, `InvoiceHistory`, `Quarter`, `Report`, `Company`, `InvoicingError` (`data-model.md`) y su test (`Money`, cuadre, permisos por estado)
- [X] T006 `domain/repository/InvoicingRepository.kt` y `InvoicingUseCases`
- [X] T007 [P] DTOs del contrato en `data/dto/InvoicingDtos.kt` (letra por letra con `docs/openapi.json`, `ResumenFactura` plano y `total` numérico)
- [X] T008 `data/remote/InvoicingRemoteDataSource.kt`: rutas de `contracts/facturacion-api.md`, multipart `ficheros`, bytes del original y de los reportes, errores de research D10
- [X] T009 `data/repository/KtorInvoicingRepository.kt` y `data/di`
- [X] T010 Test de datos en `dataTest/InvoicingRepositoryTest.kt` con `MockEngine`: campos del `PUT` con versión, multipart, mapeo de errores, total numérico, reporte JSON

## Phase 3: US1 + US4 — Subir y encontrar (P1) 🎯 MVP

- [X] T011 [US4] `ui/list/InvoiceListViewModel.kt` + `InvoiceListScreen.kt`: páginas de 50, filtros (estado, tipo, fechas, texto), sondeo de pendientes (research D6), estado sin conexión
- [X] T012 [US1] Subida desde la lista: hoja con cámara, galería y archivos; comprobación previa de tamaño (10 MB/fichero, 50 MB/envío); resumen por fichero con acceso a la duplicada
- [X] T013 [P] [US1] Tests del ViewModel de la lista (filtros, paginación, sondeo, subida)

## Phase 4: US2 + US3 — Revisar, confirmar y descartar (P1/P2)

- [X] T014 [US2] `ui/detail/InvoiceDetailViewModel.kt` + `InvoiceDetailScreen.kt`: todos los campos editables, líneas de IVA, avisos por campo, dudosos, ayuda de cuadre; guardar, confirmar (guarda antes, research D5), descartar con confirmación, reconocer de nuevo con advertencia; conflicto de versión (D4)
- [X] T015 [US2] Original (`ui/detail/InvoiceOriginalScreen.kt`: imagen con Coil, PDF con el visor del sistema) e historial (`ui/history/InvoiceHistoryScreen.kt`)
- [X] T016 [P] [US2] Tests del ViewModel del detalle

## Phase 5: US5 + US6 — Trimestres, reportes y empresa (P2/P3)

- [X] T017 [US5] `ui/quarters/QuartersScreen.kt` + ViewModel: año, cuatro trimestres, cerrar y reabrir con motivo
- [X] T018 [US5] `ui/report/ReportScreen.kt` + ViewModel: periodo, resumen y descarga CSV/PDF
- [X] T019 [US6] `ui/company/CompanyScreen.kt` + ViewModel; aviso de empresa sin configurar en la lista
- [X] T020 [P] [US5] Tests de los ViewModels de trimestres, reporte y empresa

## Phase 6: Navegación y polish

- [X] T021 `UserRole.canManageInvoicing` (solo ADMIN); pestaña Facturación; la cuenta pasa a la barra superior (`LocalAccountAction` en `AppTopBar`) y la barra inferior se oculta con un solo destino (research D11)
- [X] T022 [P] Sin cadenas visibles en el código de `feature/invoicing/presentation` (principio IX)
- [X] T023 Subir el trinquete de cobertura a la cifra medida
- [X] T024 [P] README: sección de facturación
- [X] T025 CI en local completa
- [ ] T026 Quickstart en dispositivo contra el backend local (al final)
