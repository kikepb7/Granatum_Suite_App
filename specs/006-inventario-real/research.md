# Phase 0 — Investigación: inventario real contra el backend

**Feature**: `006-inventario-real` · **Fecha**: 2026-10-08

Verificado contra `docs/openapi.json` (backend `d1857ad`), el código del módulo de inventario del
backend en ese commit (`features/inventory`) y el código actual de `feature/inventory`.

## D1 — Leer sin conexión, modificar solo con conexión

**Decisión**: materiales y categorías se cachean en Room y se leen de ahí, refrescando al abrir.
Las modificaciones (alta, edición, borrado, ajuste de cantidad y categorías) van directas al
servidor y, sin conexión, fallan con un error claro.

**Justificación**: el inventario es compartido y sus rutas no aceptan clave de idempotencia. Un
alta reintentada tras una respuesta perdida crearía dos materiales, y dos ajustes de cantidad
hechos sin conexión desde dos móviles se pisarían sin que nadie se enterase. El fichaje necesita
ser offline-first porque se ficha donde no hay cobertura; el inventario se gestiona en el
almacén, donde sí la hay. Principio III: offline-first donde aporta, sin inventar consistencia
que el servidor no ofrece.

## D2 — Base de datos local: reconstruir la caché

**Hecho**: la base actual (`AppInventoryDatabase` v1) es una caché de lectura: `MaterialEntity` y
`StockMovementEntity` se rellenan desde el servidor y no guardan nada pendiente de enviar. Además,
nunca recibió datos reales, porque las rutas antiguas no existen.

**Decisión**: versión 2 con `fallbackToDestructiveMigrationFrom(1)`. Las tablas nuevas son
`material`, `category` y `material_history`. Al abrirse se descarga todo de nuevo.

**Por qué no una migración manual**: no hay nada que conservar. Una migración que copiara
categorías fijas a un modelo con categorías del servidor produciría datos falsos.

## D3 — Modelo

Sigue el contrato campo a campo (ver `data-model.md`). Las seis categorías fijas y los cinco
estados inventados desaparecen. El **estado** del servidor (`NUEVO`, `USADO`, `DANADO`, `ROTO`,
`EN_REPARACION`) describe la condición física; las existencias se ven con la cantidad disponible
sobre la total. «Agotado» se deduce de `cantidadDisponible == 0` y se ofrece como filtro local.

## D4 — Filtrado local

El servidor no pagina ni filtra (`GET /api/materiales` sin parámetros). La app filtra en el
dispositivo por texto (nombre, ubicación, proveedor), categoría, estado y «agotados».

## D5 — Errores

| Respuesta | Error de dominio | Reacción |
|---|---|---|
| sin red, timeout | `NoInternet` | «Necesitas conexión para…» (FR-014) |
| `404 MATERIAL_NOT_FOUND` | `MaterialNotFound` | se borra de la caché y se avisa (FR-015) |
| `404 CATEGORIA_NOT_FOUND` | `CategoryNotFound` | se refrescan las categorías y se pide elegir otra |
| `400 INVALID_OPERATION` en `PATCH /cantidad` | `QuantityOutOfRange` | no debería ocurrir con la validación local (FR-006) |
| `400 VALIDACION` | `Invalid` | — |
| `403` | `Forbidden` | — |
| cualquier otro, `5xx` incluido | `Unknown` | — |

**Borrar una categoría en uso** violaría la clave foránea en el servidor, que no captura esa
excepción y respondería `500`. La app lo evita: no ofrece borrar una categoría con materiales en
la caché (FR-013). Queda anotado como carencia del backend.

## D6 — Fotos

`fotos` es una lista de URLs y no hay ruta de subida. La app las muestra con Coil (ya en
`core/designsystem`) y las reenvía tal cual al editar: `UpdateMaterialRequest.fotos` es
obligatorio, y mandar una lista vacía las borraría.

## D7 — Precio

`precioUnitario` es `number` en el contrato (`BigDecimal` en el servidor). Se transporta como
`Double` y se muestra con dos decimales. La entrada en el formulario acepta coma o punto.

## D8 — Tests

En `commonTest`:

- validación del formulario (FR-009);
- validación del ajuste de cantidad;
- filtrado local;
- repositorio con `MockEngine` y DAOs en memoria: los nombres de campo del contrato, la caché, el
  404 que borra de la caché y que sin red no se escribe nada;
- ViewModels de lista, formulario y ajuste.

## Resumen

Ninguna incógnita abierta. Sin dependencias nuevas, salvo declarar `coil-compose` en
`feature/inventory/presentation`, que ya está en el catálogo.
