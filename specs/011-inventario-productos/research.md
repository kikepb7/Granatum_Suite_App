# Phase 0 — Investigación: productos del inventario

> **Revisado el 2026-10-17**: el backend implementó su feature 010 (`specs/010-inventory-materials-refactor`)
> con el mismo modelo. D1–D3 se sustituyen por el contrato nuevo; la primera versión (mapear el
> producto sobre el material antiguo y subir la imagen directamente a Supabase desde la app) queda
> descartada.

## D1 — El producto es el material del backend 010, campo a campo

| Producto (app) | API de materiales (backend 010) |
|---|---|
| Categoría (nombre) | `categoria` |
| Descripción | `descripcion` (no en blanco, ≤ 500) |
| Alto, ancho | `dimensiones.alto`, `dimensiones.ancho` (cm, > 0, 2 decimales) |
| Cantidad | `cantidad` (≥ 0) |
| Localización | `localizacion.almacen` (A/B/C) y `localizacion.seccion` (1–5) |
| Imagen | `imagenUrl` (URL firmada temporal, o null) |
| Alta, modificación | `fechaAlta`, `fechaUltimaModificacion` |

El alta lleva `cantidad`; la edición (`PUT`) no. La caché de Room pasa a la versión 3 y se
reconstruye desde el servidor (es solo de lectura).

## D2 — Cantidad con historial

Solo cambia con `PATCH /materiales/{id}/cantidad` `{cantidad, motivo}`, que la registra en el
historial. El servidor exige motivo no vacío: si la persona no escribe ninguno, la app envía «Ajuste
de cantidad».

## D3 — La imagen la guarda el servidor

`PUT /materiales/{id}/imagen` (multipart, parte `imagen`) y `DELETE /materiales/{id}/imagen`. El
servidor decide el tipo por los bytes (JPEG/PNG/WebP, 415 `IMAGEN_NO_VALIDA`), limita a 10 MB (413
`IMAGEN_DEMASIADO_GRANDE`) y responde 503 `ALMACEN_IMAGENES_NO_DISPONIBLE` sin storage. El bucket
es privado y la respuesta trae una URL firmada que caduca (1 h por defecto):

- La app no guarda credenciales de storage ni habla con él más que para descargar esas URL.
- Como la imagen se asocia a un material existente, el formulario guarda primero el producto y
  luego la imagen. Si la imagen falla, el producto ya está guardado: el formulario sigue abierto sobre
  él y volver a guardar solo reintenta lo que falta (no duplica el alta).
- Las imágenes se cargan con un cliente aparte (`ImageHttpClient`, sin token ni API key), con el
  motor conmutable para que el Demo sirva las suyas. La caché de Coil usa la URL sin el token como
  clave, para no volver a descargar la misma imagen cada vez que cambia la firma.

## D4 — Filtros

Búsqueda + selector de almacén en la propia lista (lo más usado, un toque) + panel con sección,
categoría, stock y orden, que enseña cuántos productos coinciden antes de aplicar. Filtros activos
como pastillas que se quitan. Componentes nuevos del design system: control segmentado, pastilla
quitable, botón de filtros con contador y contador numérico; del inventario: plano de almacenes.
