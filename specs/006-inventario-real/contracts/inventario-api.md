# Contrato — Uso de la API de inventario

**Feature**: `006-inventario-real` · **Fecha**: 2026-10-08

Formas de `docs/openapi.json` (backend `d1857ad`). Códigos de `InventoryExceptionHandler` y de
`CommonExceptionHandler` del backend en ese commit.

| Acción | Petición | Cuerpo | Respuesta correcta | Efecto en la caché |
|---|---|---|---|---|
| Listar | `GET /materiales` | — | `200 [MaterialDto]` | reemplaza `material` |
| Ver uno | `GET /materiales/{id}` | — | `200 MaterialDto` | upsert |
| Alta | `POST /materiales` | `CreateMaterialRequest` | `201 MaterialDto` | upsert |
| Editar | `PUT /materiales/{id}` | `UpdateMaterialRequest` (con las `fotos` existentes) | `200 MaterialDto` | upsert |
| Ajustar | `PATCH /materiales/{id}/cantidad` | `{cantidadDisponible, motivo}` | `200 MaterialDto` | upsert |
| Borrar | `DELETE /materiales/{id}` | — | `204` | delete |
| Historial | `GET /materiales/{id}/historial` | — | `200 [HistorialMaterialDto]` | reemplaza el historial de ese material |
| Categorías | `GET /categorias` | — | `200 [CategoriaDto]` | reemplaza `category` |
| Crear categoría | `POST /categorias` | `{nombre, descripcion?}` | `201 CategoriaDto` | upsert |
| Renombrar | `PUT /categorias/{id}` | `{nombre, descripcion?}` | `200 CategoriaDto` | upsert y refresco de materiales |
| Borrar categoría | `DELETE /categorias/{id}` | — | `204` | delete; solo si no tiene materiales (FR-013) |

Errores: tabla de research D5. `404 MATERIAL_NOT_FOUND` en cualquier ruta de un material lo
borra de la caché. Sin red no se toca la caché.

Las rutas antiguas `/inventory/materials*` y sus DTOs se retiran (FR-017).
