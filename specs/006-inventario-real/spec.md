# Feature Specification: Inventario real contra el backend

**Feature Branch**: `inventario-feature`

**Created**: 2026-10-08

**Status**: Draft

**Input**: Fase 5 del roadmap, adelantada a la de equipo. Que encargadas y administración
gestionen materiales y categorías tal como los guarda el servidor, y ajusten existencias dejando
constancia del motivo.

> **Por qué antes que equipo.** La fase de equipo está bloqueada por el contrato: los fichajes y
> las correcciones solo traen el identificador de la persona, y la única ruta que da nombres es
> solo de administración. Una encargada vería identificadores en lugar de personas. El
> inventario no tiene ese problema.

> **Estado verificado el 2026-10-08.** El inventario de la app no habla con el servidor real. Usa
> rutas que no existen (`/inventory/materials`), seis categorías fijas en lugar de las que crea
> la empresa, estados que el servidor no conoce (disponible, stock bajo, agotado, reservado) y una
> sola cantidad, cuando el servidor distingue la disponible de la total. No permite borrar
> materiales ni gestionar categorías.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consultar el inventario real (Priority: P1)

Una encargada abre el inventario y ve los materiales tal como constan en el servidor, con su
categoría, existencias, estado y ubicación. Busca y filtra para encontrar uno rápido. Sin
cobertura sigue viendo lo último que se descargó.

**Why this priority**: Es el uso diario. Sin él no hay nada que gestionar.

**Independent Test**: Crear materiales por la API, abrir el inventario y comprobar que aparecen
con todos sus datos. Repetir en modo avión.

**Acceptance Scenarios**:

1. **Given** materiales en el servidor, **When** se abre el inventario, **Then** aparecen con
   nombre, categoría, disponibles sobre total, estado y ubicación.
2. **Given** una búsqueda o un filtro por categoría o estado, **When** se aplica, **Then** solo se
   ven los que coinciden.
3. **Given** que no hay cobertura, **When** se abre, **Then** se ve lo último descargado, avisando
   de que puede no estar al día.
4. **Given** un material, **When** se abre su detalle, **Then** se ven todos sus datos: tamaño,
   color, material, proveedor, precio, fotos y fechas.

---

### User Story 2 - Ajustar existencias con motivo (Priority: P1)

Al usar o recibir material, la encargada cambia la cantidad disponible e indica el motivo. El
servidor guarda el cambio en el historial del material.

**Why this priority**: Es la operación más frecuente después de consultar, y la que da
trazabilidad.

**Independent Test**: Ajustar la cantidad de un material con un motivo y comprobar el nuevo valor
y la entrada en su historial.

**Acceptance Scenarios**:

1. **Given** un material, **When** se ajusta la cantidad disponible con un motivo, **Then** el
   servidor la guarda y el detalle la muestra.
2. **Given** una cantidad negativa o mayor que la total, **When** se intenta guardar, **Then** la
   app lo impide antes de enviar.
3. **Given** que falta el motivo, **When** se intenta guardar, **Then** la app lo pide.
4. **Given** un material, **When** se abre su historial, **Then** se ve cada cambio con el tipo,
   el valor anterior, el nuevo, el motivo y la fecha.

---

### User Story 3 - Dar de alta, editar y retirar materiales (Priority: P2)

La encargada registra un material nuevo con todos sus datos, corrige uno existente o retira uno
que ya no se usa.

**Why this priority**: Necesario para mantener el inventario, pero menos frecuente que consultar
y ajustar.

**Independent Test**: Crear un material desde la app, editarlo y borrarlo, comprobando cada paso
en el servidor.

**Acceptance Scenarios**:

1. **Given** el formulario de alta, **When** se rellenan todos los datos obligatorios y se guarda,
   **Then** el material aparece en el servidor y en la lista.
2. **Given** un campo obligatorio vacío o un número fuera de rango, **When** se intenta guardar,
   **Then** la app lo señala en el campo antes de enviar.
3. **Given** un material, **When** se edita y se guarda, **Then** el servidor guarda los cambios y
   las fotos que tenía se conservan.
4. **Given** un material, **When** se borra, **Then** la app pide confirmación y después
   desaparece del servidor y de la lista.

---

### User Story 4 - Gestionar las categorías (Priority: P3)

La administración o la encargada crean, renombran y borran las categorías con las que se
clasifican los materiales.

**Why this priority**: Las categorías se definen al principio y cambian poco.

**Independent Test**: Crear una categoría, usarla en un material, intentar borrarla y comprobar
que la app no lo permite mientras tenga materiales.

**Acceptance Scenarios**:

1. **Given** la lista de categorías, **When** se crea una con nombre y descripción opcional,
   **Then** aparece y se puede elegir en el formulario de material.
2. **Given** una categoría, **When** se renombra, **Then** el cambio se ve en sus materiales.
3. **Given** una categoría con materiales, **When** se intenta borrar, **Then** la app no lo
   permite y explica por qué.
4. **Given** una categoría sin materiales, **When** se borra con confirmación, **Then**
   desaparece.

---

### Edge Cases

- **Modificar sin cobertura.** El inventario es compartido y el servidor no ofrece claves de
  idempotencia para él: reintentar el alta de un material sin cobertura podría duplicarlo. Toda
  modificación exige conexión y la app lo dice. Consultar sí funciona sin conexión.
- **Material borrado por otra persona.** Al abrirlo o modificarlo, el servidor responde que no
  existe; la app lo quita de la lista y lo explica.
- **Categoría borrada por otra persona** mientras se edita un material: al guardar, el servidor
  responde que no existe; la app pide elegir otra.
- **Fotos.** El servidor guarda direcciones de fotos, pero no ofrece forma de subirlas. La app
  las muestra y las conserva al editar, sin poder añadir nuevas.
- **Precio.** Admite decimales; se muestra en euros con dos decimales.
- **Medidas.** Alto y ancho obligatorios, diámetro opcional, en mm, cm, m o pulgadas.

## Requirements *(mandatory)*

### Functional Requirements

**Consulta**

- **FR-001**: La app MUST mostrar los materiales del servidor con nombre, categoría, cantidad
  disponible sobre total, estado y ubicación.
- **FR-002**: La app MUST permitir buscar por nombre y filtrar por categoría y por estado.
- **FR-003**: La app MUST conservar lo último descargado de materiales y categorías para
  consultarlo sin cobertura, e indicar cuándo lo mostrado puede no estar al día.
- **FR-004**: El detalle MUST mostrar todos los datos del material, incluidas sus fotos.

**Existencias**

- **FR-005**: La app MUST permitir cambiar la cantidad disponible con un motivo obligatorio.
- **FR-006**: La app MUST validar antes de enviar que la cantidad disponible está entre 0 y la
  total.
- **FR-007**: La app MUST mostrar el historial de cambios de cada material con el tipo de cambio,
  el valor anterior, el nuevo, el motivo y la fecha.

**Materiales**

- **FR-008**: La app MUST permitir dar de alta un material con todos los datos que exige el
  servidor: nombre, categoría, cantidades disponible y total, medidas (alto, ancho, diámetro
  opcional y unidad), color, material, estado, ubicación, precio unitario y proveedor.
- **FR-009**: La app MUST validar en local los límites del servidor: nombre de hasta 140
  caracteres, textos obligatorios no vacíos, cantidades y precio no negativos, disponible no mayor
  que total y medidas no negativas.
- **FR-010**: La app MUST permitir editar un material, conservando sus fotos.
- **FR-011**: La app MUST permitir borrar un material, con confirmación.

**Categorías**

- **FR-012**: La app MUST permitir crear, renombrar y borrar categorías: nombre de hasta 100
  caracteres, descripción opcional de hasta 500.
- **FR-013**: La app MUST impedir borrar una categoría que tenga materiales, y explicarlo.

**General**

- **FR-014**: Toda modificación MUST requerir conexión. Sin ella, la app MUST decirlo y no
  guardar nada para más tarde.
- **FR-015**: Si el servidor responde que el material o la categoría no existen, la app MUST
  actualizar lo que muestra y explicarlo.
- **FR-016**: Solo administración y encargada MUST ver el inventario (spec 004).
- **FR-017**: Las rutas antiguas del inventario y sus datos guardados MUST retirarse.

### Key Entities

- **Material**: nombre, categoría, cantidad disponible, cantidad total, medidas, color, material,
  estado (nuevo, usado, dañado, roto, en reparación), ubicación, precio unitario, proveedor, fotos,
  fecha de alta y de última modificación.
- **Categoría**: nombre y descripción.
- **Cambio de historial**: tipo (cantidad, estado, ubicación, precio, proveedor u otro), valor
  anterior, valor nuevo, motivo y fecha.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El inventario mostrado coincide con el del servidor en todos los campos.
- **SC-002**: Encontrar un material con la búsqueda lleva menos de 10 segundos.
- **SC-003**: Ajustar una cantidad con su motivo lleva menos de 20 segundos.
- **SC-004**: Ningún valor que el servidor rechazaría por formato llega a enviarse.
- **SC-005**: Sin cobertura, el inventario se consulta el 100 % de las veces y ninguna
  modificación se pierde ni se duplica (porque no se intenta).

## Assumptions

- El servidor no ofrece paginación ni filtros: se descarga la lista completa y se filtra en el
  dispositivo. El inventario de una floristería cabe de sobra en una lista.
- El historial no muestra quién hizo cada cambio: el servidor solo da un identificador de usuario
  y no hay forma de traducirlo a un nombre (el mismo problema que bloquea la fase de equipo).
- Los textos son solo en español (principio IX).

## Dependencies

- Contrato fijado en `docs/openapi.json` (backend `d1857ad`). El inventario del backend es
  anterior a Spec Kit y no tiene contrato escrito: sus códigos de error son los de su manejador,
  `MATERIAL_NOT_FOUND` y `CATEGORIA_NOT_FOUND` (404), y los comunes `VALIDACION` (400) e
  `INVALID_OPERATION` (400, cantidad fuera de rango).
- Navegación por rol de la spec 004.

## Out of Scope

- Subir fotos (el servidor no lo permite).
- Modificar el inventario sin cobertura.
- Mostrar quién hizo cada cambio del historial.
