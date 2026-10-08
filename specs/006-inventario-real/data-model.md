# Phase 1 — Modelo de datos

**Feature**: `006-inventario-real` · **Fecha**: 2026-10-08

## Dominio (`feature/inventory/domain`)

| Tipo | Campos |
|---|---|
| `CategoryModel` | `id`, `name`, `description?` |
| `MaterialCondition` | `NUEVO, USADO, DANADO, ROTO, EN_REPARACION` |
| `SizeUnit` | `MM, CM, M, IN` |
| `MaterialSize` | `height`, `width`, `diameter?`, `unit` |
| `MaterialModel` | `id`, `name`, `category: CategoryModel`, `available: Int`, `total: Int`, `size`, `color`, `physicalMaterial`, `condition`, `location`, `unitPrice: Double`, `supplier`, `photos: List<String>`, `createdAt`, `updatedAt` · derivado `isOutOfStock = available == 0` |
| `MaterialDraft` | lo editable de `MaterialModel`; `available` solo en el alta (en la edición se usa el ajuste con motivo) |
| `HistoryChangeType` | `CANTIDAD, ESTADO, UBICACION, PRECIO_UNITARIO, PROVEEDOR, OTRO` |
| `MaterialHistoryEntry` | `id`, `type`, `previousValue?`, `newValue?`, `reason`, `at` |
| `MaterialFilter` | `query`, `categoryId?`, `condition?`, `onlyOutOfStock` |
| `InventoryError` | `NoInternet, MaterialNotFound, CategoryNotFound, QuantityOutOfRange, Invalid, Forbidden, Unknown` |

### Validación del borrador (FR-009)

| Campo | Regla | Error |
|---|---|---|
| `name` | no vacío, ≤ 140 | `NAME_REQUIRED`, `NAME_TOO_LONG` |
| `color`, `physicalMaterial`, `location`, `supplier` | no vacíos | `…_REQUIRED` |
| `available`, `total` | ≥ 0, `available ≤ total` | `NEGATIVE_QUANTITY`, `AVAILABLE_OVER_TOTAL` |
| `unitPrice` | ≥ 0 | `NEGATIVE_PRICE` |
| `height`, `width`, `diameter` | ≥ 0 | `NEGATIVE_SIZE` |
| `category` | elegida | `CATEGORY_REQUIRED` |

### Ajuste de cantidad (FR-005, FR-006)

`0 ≤ nueva ≤ total`, distinta de la actual, y `motivo` no vacío.

### Categoría (FR-012)

`nombre` no vacío, ≤ 100 · `descripcion` ≤ 500.

## Base de datos local, versión 2 (caché)

| Tabla | Contenido |
|---|---|
| `category` | `id` PK, `name`, `description?` |
| `material` | los campos de `MaterialModel`, con `categoryId`, `sizeJson`, `photosJson` y las fechas en ms |
| `material_history` | `id` PK, `materialId`, `type`, `previousValue?`, `newValue?`, `reason`, `atEpochMillis` |

`fallbackToDestructiveMigrationFrom(1)`: la versión 1 solo contenía caché (research D2).
