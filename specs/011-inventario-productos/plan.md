# Implementation Plan: Productos del inventario y filtros rediseñados

**Branch**: `inventory-refactor-feature` · **Spec**: [spec.md](spec.md) · **Research**: [research.md](research.md)

## Summary

The inventory's material becomes a **product** (category as name, description, dimensions in cm,
one quantity with history, one image, warehouse/section location, dates) without touching the
backend: the app maps it onto the existing material contract (D1, D2). Images go straight to a
Supabase-compatible object storage through a separate HTTP client (D3). The list gets a new filtering
experience built from new design-system controls.

## Technical Context

Kotlin 2.2.20 · Compose Multiplatform 1.9 · Ktor 3.2.3 · Room KMP 2.7.2 · Koin 4.1 · Coil 3.3.0.
No new libraries. No Room migration: the cached material entity already holds every field.

## Constitution Check

- Contract (VI): unchanged backend; the mapping is documented in research D1 and tested.
- Offline-first reads, online-only writes: kept from spec 006.
- Secrets: storage keys come from `local.properties` / Gradle properties, never committed; the store
  build only carries PRO's. The backend's token never reaches the storage (separate client).
- Quality gates: tests for domain (location parsing, filters, validation), data (mapping, quantity
  sequence, upload) and every ViewModel; coverage ratchet must not drop.

## Design

| Layer | Change |
|---|---|
| Domain | `ProductModel`, `ProductDraft`, `StorageLocation` (`A-3`), `Dimensions`, `ProductFilter` + `filterBy`, `ProductImageStorage`, validation |
| Data | `OfflineFirstInventoryRepository` mapping (D1), `setQuantity` PUT/PATCH order (D2), `SupabaseProductImageStorage` (D3), `StorageHttpClient` in core/data |
| Config | BuildKonfig `STORAGE_URL_PRE/PRO`, `STORAGE_KEY_PRE/PRO`, `STORAGE_BUCKET`; `ActiveEnvironment.storage` |
| Design system | `AppIcons`, `AppSegmentedControl`, `AppFilterButton`, `AppRemovablePill`, `AppQuantityStepper` |
| Presentation | `ProductCard`, `ProductImage`, `LocationPicker`, `InventoryFiltersSheet`; list, detail and form rewritten; routes renamed `Product*` |
| App | Coil's singleton image loader uses `StorageHttpClient` |
| Demo | In-memory storage routes; seeds with `A-2`-style locations |

## Risks

- The storage key is a client key (anon): anyone with the app can upload to the bucket. Acceptable
  for an internal app; the proper fix is backend-issued signed upload URLs (future backend work).
- Replaced images stay in the bucket (the app has no delete privilege).
