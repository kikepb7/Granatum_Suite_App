# Implementation Plan: Productos del inventario y filtros rediseñados

**Branch**: `inventory-refactor-feature` · **Spec**: [spec.md](spec.md) · **Research**: [research.md](research.md)

## Summary

The inventory's material becomes a **product** (category as name, description, dimensions in cm,
one quantity with history, one image, warehouse/section location, dates), following the backend's
feature 010 contract field for field (D1, D2). The server stores images itself and hands out signed
URLs (D3). The list gets a new filtering experience built from new design-system controls.

## Technical Context

Kotlin 2.2.20 · Compose Multiplatform 1.9 · Ktor 3.2.3 · Room KMP 2.7.2 · Koin 4.1 · Coil 3.3.0.
No new libraries. Room cache goes to version 3, rebuilt from the server (read-only cache).

## Constitution Check

- Contract (VI): backend 010 (`specs/010-inventory-materials-refactor/contracts/README.md`); the pinned
  `docs/openapi.json` is updated with `scripts/sync-openapi.sh` once the backend commits it.
- Offline-first reads, online-only writes: kept from spec 006.
- Secrets: the app holds no storage credentials; images load from signed URLs with a client that
  carries neither the session token nor the API key.
- Quality gates: tests for domain (location parsing, filters, validation), data (mapping, quantity
  sequence, upload) and every ViewModel; coverage ratchet must not drop.

## Design

| Layer | Change |
|---|---|
| Domain | `ProductModel`, `ProductDraft`, `StorageLocation` (`A-3`), `Dimensions`, `ProductFilter` + `filterBy`, `ProductImageStorage`, validation |
| Data | DTOs of backend 010, `OfflineFirstInventoryRepository`, multipart `PUT …/imagen` and `DELETE …/imagen`, Room v3 |
| Design system | `AppIcons`, `AppSegmentedControl`, `AppFilterButton`, `AppRemovablePill`, `AppQuantityStepper` |
| Presentation | `ProductCard`, `ProductImage` (cache key without token), `LocationPicker`, `InventoryFiltersSheet`; list, detail and form rewritten; routes renamed `Product*` |
| App | Coil's singleton image loader uses `ImageHttpClient` |
| Demo | Inventory routes of backend 010, image upload and signed-URL reads |

## Risks

- Signed URLs expire (1 h by default): a product cached longer shows its category initial until the
  next refresh, which happens on opening the list or the detail.
