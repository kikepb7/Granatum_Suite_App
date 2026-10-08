# Implementation Plan: Inventario real contra el backend

**Branch**: `inventario-feature` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

## Summary

El inventario pasa a `/api/materiales` y `/api/categorias` con el modelo del servidor: categorías
de la empresa, cantidad disponible y total, medidas, estado físico, precio, proveedor, fotos e
historial. Lectura desde una caché local que se refresca al abrir; modificaciones solo con
conexión, porque el inventario es compartido y el servidor no ofrece idempotencia. Ver
[research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20 · **Dependencies**: Ktor 3.2.3, Room 2.7.2, Koin, Compose
Multiplatform, Coil 3.3.0 (ya en el catálogo) · **Storage**: Room, `AppInventoryDatabase` v2
(caché, migración destructiva) · **Testing**: `commonTest` con `MockEngine` · **Platform**:
Android e iOS · **Constraints**: el contrato en `d1857ad`; sin escrituras sin conexión.

## Constitution Check

Evaluado contra la constitución **v1.1.2**.

| Principio | Estado |
|---|---|
| I · II | ✅ todo en `feature/inventory/{domain,database,data,presentation}`, en `commonMain` |
| III Offline-first | ✅ con matiz justificado (D1): la lectura funciona sin conexión; la escritura exige conexión porque el servidor no da idempotencia en inventario |
| IV Errores | ✅ `InventoryError` en el dominio, texto en presentation |
| VI Contrato | ✅ [contracts/inventario-api.md](./contracts/inventario-api.md) |
| VIII Tests | ✅ D8; el trinquete sube |
| IX UI | ✅ textos en `composeResources`; componentes de `core/designsystem` |
| XI Build | ✅ solo `coil-compose` declarado en el módulo, ya en el catálogo |
| XII Paridad | ✅ |

**Resultado: PASA.**

## Project Structure

```text
feature/inventory/
├── domain/        # CategoryModel, MaterialModel/Draft/Size/Condition, historial, filtro,
│                  # InventoryError, validaciones, repositorio y casos de uso
├── database/      # v2: CategoryEntity, MaterialEntity, MaterialHistoryEntity, DAOs
├── data/          # DTOs del contrato, InventoryRemoteDataSource, OfflineFirstInventoryRepository
└── presentation/  # lista con filtros, detalle con fotos e historial, formulario, ajuste,
                   # categorías; textos en composeResources
```

## Fases

1. Dominio, validaciones y sus tests.
2. Base de datos v2 y capa de datos con tests de contrato.
3. Lista, detalle e historial (US1, US2).
4. Formulario de alta y edición, y borrado (US3).
5. Categorías (US4).
6. Trinquete, README y CI en local. La verificación en dispositivo se hará al final, junto con la
   de la spec 005, como pidió el responsable.

## Complexity Tracking

Sin desviaciones.
