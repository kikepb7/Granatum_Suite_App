# Feature Specification: Productos del inventario y filtros rediseñados

**Feature Branch**: `inventory-refactor-feature` · **Created**: 2026-10-17 · **Status**: Draft

**Input**: «Los datos de los productos deben ser: Categoría, Descripción, Dimensiones (alto y
ancho), cantidad, imagen, Localización, fecha de alta y fecha de modificación. La localización se
divide en 3 almacenes (A, B y C) con secciones 1 a 5. La imagen debe poder subirse directamente al
storage. Rediseña los filtros, más intuitivos y de mejor calidad, con componentes propios, siguiendo
la estética de la app.»

Decisiones del responsable (2026-10-17): al principio solo cambiaba la app; después el backend
implementó el mismo modelo (su feature 010) y la app se alineó con él; **la categoría es el
nombre** del producto; la imagen va a un **storage externo**; **una sola cantidad, con historial**.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver los productos y encontrarlos rápido (Priority: P1)

La lista muestra cada producto con su imagen, su categoría como título, la descripción, su
localización («A · 3») y la cantidad. Se busca por texto y se filtra de forma visual: almacén con un
toque, y un panel con sección, categoría, stock y orden. Los filtros activos se ven como pastillas que
se quitan con un toque.

**Acceptance Scenarios**:

1. **Given** productos, **When** se abre el inventario, **Then** se ven con imagen (o la inicial de la
   categoría), categoría, descripción, localización y cantidad; los agotados destacan.
2. **Given** el selector de almacén, **When** se toca «B», **Then** solo se ven los del almacén B.
3. **Given** el panel de filtros, **When** se elige sección, categoría, stock u orden, **Then** el botón
   muestra cuántos productos coinciden antes de aplicar, y los filtros activos aparecen como pastillas.
4. **Given** filtros activos, **When** se quita una pastilla o se pulsa «Limpiar», **Then** se quitan.

### User Story 2 - Dar de alta y editar un producto (Priority: P1)

El formulario pide categoría, descripción, alto y ancho (cm), cantidad (solo en el alta), imagen y
localización. La localización se elige en un plano de los tres almacenes con sus cinco secciones.
La imagen se hace con la cámara o se elige de la galería y se sube al storage.

**Acceptance Scenarios**:

1. **Given** el formulario, **When** se completa y se guarda, **Then** el producto aparece con la
   imagen subida y su localización.
2. **Given** un dato obligatorio vacío o fuera de rango, **When** se guarda, **Then** se señala en el
   campo y no se envía.
3. **Given** que la imagen no se puede subir, **When** se guarda, **Then** el producto queda guardado,
   la app explica por qué falló la imagen y volver a guardar la reintenta sin duplicar el producto.

### User Story 3 - Ajustar la cantidad con historial (Priority: P2)

En el detalle, la cantidad se cambia con un contador (− / +) y atajos de ±5 y ±10, con un motivo
opcional; no se envía nada hasta guardar.
Cada cambio queda en el historial del producto.

### Edge Cases

- Sin storage en el servidor (503): el producto se guarda y la app explica que la imagen no.
- Las URL de imagen caducan: se renuevan al refrescar el producto.

## Requirements *(mandatory)*

- **FR-001**: Un producto MUST tener categoría (que hace de nombre), descripción (1–500), alto y ancho
  en cm (> 0), cantidad (≥ 0), imagen opcional, localización (almacén A/B/C y sección 1–5) y fechas
  de alta y modificación.
- **FR-002**: La imagen MUST hacerse con la cámara o elegirse de la galería y enviarse al servidor
  (JPEG/PNG/WebP, máx. 10 MB), que la guarda en su storage.
- **FR-003**: La cantidad MUST ajustarse con contador y atajos, con motivo opcional, y cada ajuste
  MUST quedar en el historial.
- **FR-004**: La lista MUST filtrarse por texto, almacén, sección, categoría y stock, y ordenarse
  (recientes, categoría A–Z, cantidad); los filtros activos MUST verse y quitarse con un toque.
- **FR-005**: La app MUST seguir el contrato de materiales del backend 010.
- **FR-006**: El token de sesión del backend MUST NOT enviarse al cargar las imágenes.

## Out of Scope

- Varias imágenes por producto.
