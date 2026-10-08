# Feature Specification: Facturación contra el backend

**Feature Branch**: `facturacion-feature`

**Created**: 2026-10-09

**Status**: Draft

**Input**: Prioridad del responsable (2026-10-09): «céntrate en la parte de facturación; que siga
las reglas del backend; más adelante añadiremos funcionalidades». Que la administración registre
las facturas del negocio desde el móvil fotografiándolas o subiéndolas, las revise, las confirme,
cierre los trimestres y saque los reportes.

> **Estado verificado el 2026-10-09.** La app no tiene nada de facturación. El backend la ofrece
> en `/api/facturacion` (13 rutas, feature 004 del backend, `specs/004-invoices`): **registra**
> facturas emitidas y recibidas a partir de su documento, con reconocimiento automático opcional;
> no emite facturas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Subir facturas desde el móvil (Priority: P1)

La administradora fotografía una factura en papel, o elige fotos o PDF del móvil, y las sube. Cada
documento se convierte en una factura que el servidor intenta leer sola. La app dice qué pasó con
cada fichero.

**Why this priority**: Es la entrada de todo el módulo y el motivo de tenerlo en el móvil.

**Independent Test**: Subir una foto y un PDF; comprobar que aparecen en la lista como pendientes
de reconocer (o ya como borrador si el reconocimiento está activo).

**Acceptance Scenarios**:

1. **Given** la lista de facturas, **When** se hace una foto o se eligen fotos o PDF y se suben,
   **Then** cada uno aparece como factura nueva y la app resume el resultado por fichero.
2. **Given** un fichero ya subido antes, **When** se sube otra vez, **Then** la app avisa de que
   está duplicado y lleva a la factura existente.
3. **Given** un fichero que no es imagen JPEG, PNG o WebP ni PDF, que pesa más de 10 MB, está
   vacío o es un PDF ilegible, **When** se sube, **Then** la app explica el motivo de ese fichero
   sin afectar a los demás.
4. **Given** fotos del iPhone en HEIC, **When** se eligen, **Then** la app las convierte a JPEG
   antes de subirlas.
5. **Given** facturas pendientes de reconocer en pantalla, **When** el servidor termina de leerlas,
   **Then** la lista se actualiza sola sin que haga falta refrescar.

---

### User Story 2 - Revisar, corregir y confirmar (Priority: P1)

La administradora abre una factura, ve el documento original y los datos leídos, corrige lo que
haga falta y la confirma. Los avisos del servidor le dicen qué impide confirmar (falta un dato, el
NIF no es válido, los importes no cuadran, está duplicada) y qué solo conviene revisar.

**Why this priority**: Una factura no cuenta en los reportes hasta que se confirma.

**Independent Test**: Corregir un borrador con un aviso bloqueante hasta que desaparezca y
confirmarlo; comprobar que pasa a confirmada en el servidor.

**Acceptance Scenarios**:

1. **Given** una factura, **When** se abre, **Then** se ven tipo, emisor y destinatario (nombre y
   NIF), número, fecha, concepto, líneas de IVA (tipo, base, cuota, recargo y causa si la cuota es
   cero), retenciones, total, si es rectificativa, su estado y sus avisos junto a cada campo.
2. **Given** un borrador, **When** se cambian datos y se guarda, **Then** el servidor recalcula
   los avisos y la app los muestra.
3. **Given** un borrador sin avisos bloqueantes, **When** se confirma, **Then** queda confirmado.
4. **Given** avisos bloqueantes, **When** se intenta confirmar, **Then** la app no lo permite y
   señala los campos.
5. **Given** campos que el reconocimiento marcó como dudosos, **When** se abre la factura,
   **Then** se resaltan para revisarlos.
6. **Given** que otra persona (o el reconocimiento) cambió la factura mientras tanto, **When** se
   guarda, **Then** la app avisa, recarga la versión nueva y no pisa los cambios ajenos.
7. **Given** una factura, **When** se pide el original, **Then** se ve la imagen o se abre el PDF.

---

### User Story 3 - Descartar y corregir confirmadas (Priority: P2)

Se descarta un documento que no debía subirse. Una factura confirmada se puede corregir o
descartar mientras su trimestre esté abierto, y queda constancia en su historial.

**Why this priority**: Los errores pasan; sin esto no hay forma de arreglarlos.

**Independent Test**: Descartar un borrador; corregir una confirmada y ver la corrección en su
historial; comprobar que con el trimestre cerrado no se permite.

**Acceptance Scenarios**:

1. **Given** una factura pendiente, en borrador o confirmada con trimestre abierto, **When** se
   descarta con confirmación, **Then** queda descartada y ya no se puede editar.
2. **Given** una confirmada con el trimestre abierto, **When** se corrige sin dejar avisos
   bloqueantes, **Then** se guarda y el historial lo registra.
3. **Given** una confirmada de un trimestre cerrado, **When** se abre, **Then** la app indica que
   el trimestre está cerrado y no ofrece corregirla ni descartarla.
4. **Given** una factura, **When** se abre su historial, **Then** se ven los intentos de
   reconocimiento (resultado, modelo, fecha, error) y los cambios (acción, fecha, valores
   anteriores).

---

### User Story 4 - Encontrar facturas (Priority: P2)

La lista muestra primero las más recientes, por páginas, y se filtra por estado, tipo, fechas o
por nombre o NIF de emisor o destinatario.

**Why this priority**: Con decenas de facturas al trimestre, hace falta encontrar una rápido.

**Independent Test**: Con varias facturas, filtrar por cada criterio y comprobar el resultado.

**Acceptance Scenarios**:

1. **Given** facturas, **When** se abre la lista, **Then** se ven con emisor o destinatario, número,
   fecha, total, estado y número de avisos, y se cargan más al llegar al final.
2. **Given** un filtro de estado, tipo, rango de fechas o un texto, **When** se aplica, **Then**
   solo se ven las que coinciden.

---

### User Story 5 - Trimestres y reportes (Priority: P2)

La administradora ve los cuatro trimestres del año, cierra uno cuando ha confirmado todo (para la
declaración de IVA) y lo reabre con un motivo si hace falta. Consulta el resumen de un mes,
trimestre o año (emitidas y recibidas: número, base, IVA por tipo, recargo, retenciones, total y
bases sin cuota) y lo descarga en CSV o PDF.

**Why this priority**: Es lo que se lleva a la gestoría cada trimestre.

**Independent Test**: Cerrar un trimestre con todo confirmado, intentar cerrar otro con borradores,
reabrir con motivo y descargar el reporte trimestral en PDF.

**Acceptance Scenarios**:

1. **Given** un año, **When** se abren los trimestres, **Then** se ven los cuatro con su estado y
   sus cierres y reaperturas (quién y cuándo, con motivo).
2. **Given** un trimestre sin facturas pendientes ni en borrador, **When** se cierra con
   confirmación, **Then** queda cerrado.
3. **Given** un trimestre con facturas pendientes o en borrador, **When** se intenta cerrar,
   **Then** la app dice cuántas faltan.
4. **Given** un trimestre cerrado, **When** se reabre, **Then** la app exige un motivo de 10 a 500
   caracteres.
5. **Given** un periodo, **When** se pide el reporte, **Then** se ve el resumen y se puede
   descargar o compartir en CSV o PDF.

---

### User Story 6 - Datos fiscales de la empresa (Priority: P3)

La administradora introduce la razón social y el NIF del negocio. Sin ellos se pueden subir y
editar facturas, pero no confirmarlas; con ellos el servidor distingue solo emitidas de recibidas.

**Why this priority**: Se hace una vez.

**Independent Test**: Intentar confirmar sin datos de empresa, introducirlos y confirmar.

**Acceptance Scenarios**:

1. **Given** que la empresa no está configurada, **When** se entra en facturación, **Then** la app
   lo indica y ofrece configurarla.
2. **Given** el formulario, **When** se guarda un NIF no válido, **Then** la app lo señala.
3. **Given** la empresa, **When** se consulta, **Then** se ve también si el reconocimiento
   automático está activo.

---

### Edge Cases

- **Solo administración.** Las demás personas no ven facturación.
- **Sin conexión.** Facturación funciona solo con conexión: consultar, subir y modificar. Sin ella,
  la app lo dice y permite reintentar. Los datos fiscales no se guardan en el móvil.
- **Reconocimiento desactivado.** Si el servidor no tiene el reconocimiento activo (modo manual),
  los documentos quedan pendientes y se rellenan a mano; la app no ofrece «reconocer de nuevo».
- **Reconocimiento que sobrescribe.** Volver a reconocer reemplaza los datos de un borrador; la app
  lo advierte antes.
- **Importes.** Se escriben con coma decimal y dos decimales como mucho; se muestran en euros. La
  app ayuda a cuadrar: muestra la diferencia entre el total y las líneas menos retenciones.
- **NIF extranjeros.** El servidor solo acepta NIF, NIE y CIF españoles: un NIF europeo deja un
  aviso bloqueante. Es una regla del backend.
- **Fechas.** Fecha futura o de hace más de cuatro años son avisos, no bloqueos.
- **Nada se borra.** Descartar no elimina; la factura sigue consultable y descargable.

## Requirements *(mandatory)*

### Functional Requirements

**Subida**

- **FR-001**: La app MUST permitir subir uno o varios documentos a la vez desde la cámara, la
  galería o los archivos del móvil.
- **FR-002**: La app MUST convertir a JPEG las imágenes en formatos que el servidor no admite
  (HEIC/HEIF) y reducir las que superen 10 MB; MUST rechazar antes de enviar lo que no pueda
  subirse y no superar 50 MB por envío.
- **FR-003**: La app MUST mostrar el resultado de cada fichero: aceptado, duplicado (con acceso a
  la existente), formato no admitido, demasiado grande, vacío o PDF ilegible.
- **FR-004**: Mientras haya facturas pendientes de reconocer en pantalla, la app MUST consultarlas
  periódicamente y actualizar la vista al cambiar.

**Revisión**

- **FR-005**: La app MUST mostrar y permitir editar todos los campos de la factura con los límites
  del servidor: nombres hasta 200, NIF hasta 20, número hasta 60, concepto hasta 500, importes con
  hasta 10 enteros y 2 decimales, tipo de IVA entre 0 y 100, causa obligatoria si la cuota es 0.
- **FR-006**: La app MUST mostrar los avisos del servidor junto a su campo, distinguiendo los que
  bloquean la confirmación de los informativos, y resaltar los campos dudosos.
- **FR-007**: La app MUST enviar siempre la versión de la factura y, ante un conflicto de versión,
  recargarla y avisar sin perder la posibilidad de revisar.
- **FR-008**: La app MUST permitir confirmar un borrador y descartar con confirmación, solo cuando
  el estado y el trimestre lo permiten.
- **FR-009**: La app MUST mostrar el documento original: las imágenes dentro de la app y los PDF
  con el visor del sistema.
- **FR-010**: La app MUST mostrar el historial de reconocimientos y cambios.
- **FR-011**: La app MUST permitir pedir un nuevo reconocimiento si el servidor lo tiene activo,
  avisando de que reemplaza los datos del borrador.

**Lista**

- **FR-012**: La app MUST listar las facturas por páginas con filtros por estado, tipo, rango de
  fechas y texto (nombre o NIF de emisor o destinatario).

**Trimestres y reportes**

- **FR-013**: La app MUST mostrar los trimestres de un año con su estado y su historial, y
  permitir cerrarlos y reabrirlos con un motivo de 10 a 500 caracteres.
- **FR-014**: La app MUST mostrar el reporte mensual, trimestral o anual y permitir descargarlo o
  compartirlo en CSV o PDF.

**Empresa**

- **FR-015**: La app MUST permitir consultar y guardar la razón social (hasta 200) y el NIF (hasta
  20) de la empresa, y mostrar si el reconocimiento automático está activo.

**General**

- **FR-016**: Solo ADMIN MUST ver facturación.
- **FR-017**: Los errores del servidor MUST mostrarse con mensajes comprensibles: versión
  desactualizada, estado no permitido, trimestre cerrado, trimestre con pendientes, factura
  incoherente o duplicada, empresa sin configurar, NIF no válido y reconocimiento no disponible.
- **FR-018**: La app MUST dejar el módulo preparado para crecer: el modelo de dominio y el acceso
  a datos no dependen de las pantallas, y cada pantalla es un destino propio.
- **FR-019**: La barra inferior MUST seguir teniendo como máximo cinco destinos: la cuenta pasa a
  un botón en la barra superior de las pantallas principales.

### Key Entities

- **Factura**: estado (pendiente de reconocer, borrador, confirmada, descartada), tipo (emitida,
  recibida), versión, emisor y destinatario (nombre, NIF), número, fecha de emisión, concepto,
  moneda, rectificativa, líneas de IVA, retenciones, total, trimestre cerrado, reconocimiento
  (resultado, campos dudosos) y avisos (campo, código, bloquea, mensaje).
- **Línea de IVA**: tipo, base, cuota, recargo y causa sin cuota (exenta, inversión del sujeto
  pasivo, intracomunitaria).
- **Trimestre**: año, número, cerrado y eventos (cierre o reapertura, autor, fecha, motivo).
- **Reporte**: periodo y, para emitidas y recibidas, número de facturas, base, IVA por tipo,
  recargo, retenciones, total y bases sin cuota por causa; facturas pendientes; trimestres cerrados.
- **Empresa**: razón social, NIF y reconocimiento activo.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Subir una factura fotografiada lleva menos de 20 segundos desde la lista.
- **SC-002**: Confirmar un borrador bien reconocido lleva menos de 30 segundos.
- **SC-003**: Ningún fichero que el servidor rechazaría por formato o tamaño se envía sin avisar.
- **SC-004**: Ningún cambio ajeno se pisa: todo guardado viaja con su versión.
- **SC-005**: El reporte trimestral en PDF está en el móvil en menos de 10 segundos.

## Assumptions

- Los importes viajan como texto decimal con punto; la app los convierte a céntimos para operar y
  nunca usa coma flotante.
- La lista del servidor tiene orden fijo (fecha de emisión descendente, sin fecha primero).
- El historial muestra el identificador del autor; los nombres no están disponibles en esta ruta.

## Dependencies

- Contrato fijado en `docs/openapi.json` (backend `d1857ad`); los códigos de error del módulo no
  están en el contrato y se toman del código del backend (`InvoicesExceptions.kt`).
- Navegación por rol de la spec 004.

## Out of Scope

- Emitir facturas, SII, VeriFactu, TicketBAI, pagos y contabilidad (fuera del backend).
- Uso sin conexión.
- Funcionalidades futuras que el responsable añadirá más adelante.
