# Phase 0 — Investigación: productos del inventario

## D1 — El modelo nuevo sobre el API actual (backend `2ec33d0`, sin cambios)

| Producto (app) | API de materiales |
|---|---|
| Categoría (nombre) | `categoria` |
| Descripción | `nombre` (1–140) |
| Alto, ancho | `tamano.alto`, `tamano.ancho`, `unidadMedida = CM` |
| Cantidad | `cantidadDisponible` = `cantidadTotal` |
| Imagen | `fotos[0]` (dirección pública en el storage) |
| Localización | `ubicacion` = `"A-3"` |
| Alta, modificación | `fechaAlta`, `fechaUltimaModificacion` |

Lo que el API exige y el producto ya no tiene (`color`, `materialFisico`, `estado`,
`precioUnitario`, `proveedor`, `diametro`) se **conserva** al editar (sigue en la caché local) y se
rellena con valores neutros al crear («—», `NUEVO`, `0`). Nada se pierde.

## D2 — Cantidad única con historial

El API solo registra historial en `PATCH /materiales/{id}/cantidad`, que exige disponible ≤ total.
Subir: `PUT` con `cantidadTotal = n` y luego `PATCH` con `cantidadDisponible = n`. Bajar: `PATCH` y
luego `PUT`. Así ambas quedan iguales y el cambio se registra. Motivo por defecto: «Ajuste de
cantidad».

## D3 — Imagen en un storage externo (Supabase Storage)

La app sube con `POST {STORAGE_URL}/storage/v1/object/{bucket}/{ruta}` y guarda la dirección
pública `…/object/public/{bucket}/{ruta}`. Configuración por entorno en BuildKonfig
(`STORAGE_URL_PRE/PRO`, `STORAGE_KEY_PRE/PRO`, `STORAGE_BUCKET`); la app PRO solo lleva la de PRO.
Se usa un **cliente HTTP aparte** (sin el token del backend ni sus cabeceras), con el mismo motor
conmutable: en Demo, el backend simulado hace de storage. Coil carga las imágenes con ese cliente
(`coil-network-ktor`, ya en el catálogo).

**Riesgo conocido**: con la clave pública (anon) cualquiera que la tenga puede subir al bucket; la
política del bucket debe limitar tipo, tamaño y ruta. La solución completa son URLs de subida
firmadas por el backend, fuera de alcance mientras el backend no cambie.

## D4 — Filtros

Búsqueda + selector de almacén en la propia lista (lo más usado, un toque) + panel con sección,
categoría, stock y orden, que enseña cuántos productos coinciden antes de aplicar. Filtros activos
como pastillas que se quitan. Componentes nuevos del design system: control segmentado, pastilla
quitable, botón de filtros con contador y contador numérico; del inventario: plano de almacenes.
