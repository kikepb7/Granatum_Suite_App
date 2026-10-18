# Feature Specification: Google Drive (hojas de fichajes y recibos) y apellidos

**Feature Branch**: `invoice-ml-feature` · **Created**: 2026-10-18 · **Status**: Draft

**Input**: «Cualquier dato del fichaje de una persona empleada o de los recibos que se suban, con
imagen o con una foto de la cámara, debe quedar registrado automáticamente en una hoja de Google
Drive. Fichajes: cuando el admin quiere exportar, se rellena la hoja (nombre, apellidos, DNI, fecha,
entrada, salida). Recibos: el admin hace fotos o sube imágenes y, mediante Claude, los datos pasan
solos a la hoja.»

Decisiones del responsable del producto (2026-10-18):

- Google Workspace, con una pestaña por mes.
- Las imágenes no van a Drive.
- Las personas tienen nombre y apellidos por separado.

La parte de servidor es la spec 012 del backend
(`Granatum_Suite_Backend/specs/012-google-sheets-sync`). Ahí están el contrato y la
lógica de la copia. La app no habla nunca con Google: solo con el backend.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nombre y apellidos en la app (Priority: P1)

Al dar de alta a una persona, al editarla y al registrarse la propietaria, la app pide los
apellidos aparte del nombre. Las listas y fichas muestran el nombre completo. Las personas que se
dieron de alta antes aparecen marcadas como "Faltan los apellidos" hasta que el `ADMIN` las edita.

**Independent Test**: en Demo o contra el backend:

- dar de alta a una persona con nombre y apellidos y verla en la lista con el nombre completo;
- intentar guardar sin apellidos y ver el error en el campo;
- abrir una persona antigua, ver el aviso, completar sus apellidos y comprobar que el aviso
  desaparece.

**Acceptance Scenarios**:

1. **Given** el formulario de alta (onboarding), **When** el `ADMIN` deja vacíos los apellidos,
   **Then** no se envía y el campo muestra «Escribe los apellidos».
2. **Given** el registro de la propietaria, **When** se completa, **Then** también pide los
   apellidos.
3. **Given** una persona con `lastNamePending`, **When** aparece en la lista o en su ficha,
   **Then** se ve el aviso «Faltan los apellidos», y su edición pide completarlos.
4. **Given** una persona con apellidos, **When** aparece en listas y fichas, **Then** se muestra
   «Nombre Apellidos».

---

### User Story 2 - Configurar Google Drive (Priority: P1)

El `ADMIN` abre "Google Drive" desde el menú de su cuenta.

- La app le muestra el correo de la cuenta técnica, con un botón para copiarlo, y le explica que
  debe compartir las dos hojas con esa cuenta como **Editor**.
- El `ADMIN` pega los enlaces de las hojas de fichajes y de recibos y guarda.
- Si una hoja no está compartida, o solo como lectora, la app se lo dice en el campo
  correspondiente.

**Independent Test**: en Demo, abrir la pantalla, copiar el correo, guardar dos enlaces válidos.
Con un enlace que la demo trate como «no compartido», ver el error en ese campo.

**Acceptance Scenarios**:

1. **Given** un `ADMIN`, **When** abre el menú de su cuenta, **Then** ve la entrada «Google
   Drive». Otros roles no la ven.
2. **Given** el servidor sin la integración, **When** el `ADMIN` abre la pantalla, **Then** ve
   «La copia en Google Drive no está disponible en este servidor» y nada más.
3. **Given** dos enlaces válidos, **When** guarda, **Then** se confirman, y la pantalla muestra
   las hojas con un botón para abrir cada una en el navegador.
4. **Given** una hoja no compartida, de solo lectura o inexistente, **When** guarda, **Then** el
   campo de esa hoja explica qué hacer: compartirla con la cuenta como Editor.
5. **Given** la misma hoja en los dos campos, **When** guarda, **Then** se le avisa de que deben
   ser hojas distintas.

---

### User Story 3 - Exportar los fichajes de un mes (Priority: P1)

En la misma pantalla, el `ADMIN` elige un mes y pulsa «Exportar a Google Drive». La app le
confirma cuando la pestaña del mes está rellena, cuántas filas tiene y, si hay personas sin
apellidos, cuántas.

**Independent Test**: en Demo, exportar el mes actual y ver «Exportados N fichajes de octubre de
2026», con el aviso de apellidos pendientes si los hay.

**Acceptance Scenarios**:

1. **Given** la integración configurada, **When** el `ADMIN` elige un mes y exporta, **Then**
   ve el progreso y después el resultado: filas escritas, más el aviso de apellidos pendientes
   si los hay.
2. **Given** un mes futuro, **When** se intenta elegir, **Then** no se puede: el selector llega
   como mucho al mes actual.
3. **Given** que la exportación queda en cola porque Google no responde, **When** el `ADMIN`
   espera, **Then** la app indica que se completará en cuanto Google responda, y el estado lo
   refleja.
4. **Given** la hoja de fichajes sin configurar, **When** el `ADMIN` intenta exportar, **Then**
   se le indica que la configure primero.

---

### User Story 4 - Estado de la copia y reintentos (Priority: P2)

La pantalla muestra:

- la última copia correcta;
- cuántas escrituras están pendientes;
- las que han fallado con su motivo, para reintentarlas una a una o todas.

Desde ahí también se puede resincronizar un mes de recibos.

**Independent Test**: en Demo, con una tarea fallida simulada: verla con su motivo, reintentarla
y verla desaparecer de fallidas. Resincronizar un mes de recibos y ver el resultado.

**Acceptance Scenarios**:

1. **Given** tareas fallidas, **When** se abre la pantalla, **Then** se ve cada una con un texto
   comprensible («Hoja de recibos sin permiso de edición», «Google no responde») y un botón
   «Reintentar».
2. **Given** varias fallidas, **When** pulsa «Reintentar todas», **Then** vuelven a la cola y se
   indica cuántas.
3. **Given** un mes, **When** pulsa «Resincronizar recibos», **Then** la pestaña de recibos de ese
   mes se reescribe y se ve el resultado.

### Edge Cases

- **Sin conexión**: la pantalla muestra el error habitual de red y permite reintentar; nada se
  guarda a medias.
- **Rol distinto de `ADMIN`** (por ejemplo, si cambia el rol con la sesión abierta): el backend
  responde 403 y la app muestra el mensaje de permisos de siempre.
- **Recibos**: confirmar una factura en la app no cambia. La copia en la hoja la hace el servidor
  solo. La app no necesita pantalla nueva para eso, salvo el estado.
- **Varias exportaciones del mismo mes seguidas**: la app muestra el resultado de la tarea que le
  devuelve el servidor, que se une a la pendiente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Los formularios de alta (onboarding), edición de personas y registro de la
  propietaria **DEBEN** pedir los apellidos como campo obligatorio (máx. 150 caracteres).
- **FR-002**: Listas y fichas **DEBEN** mostrar «Nombre Apellidos», y el aviso «Faltan los
  apellidos» cuando el servidor indica `lastNamePending`.
- **FR-003**: Solo el `ADMIN` **DEBE** ver la entrada «Google Drive» en el menú de su cuenta.
- **FR-004**: La pantalla **DEBE**:
  - mostrar si la integración está disponible y el correo de la cuenta técnica, con acción para
    copiarlo;
  - mostrar las instrucciones para compartir;
  - permitir guardar las dos hojas, con errores por campo.
- **FR-005**: La pantalla **DEBE** permitir exportar los fichajes de un mes, hasta el actual, y
  mostrar el progreso consultando la tarea hasta que termine, falle o pase a la cola. Al final
  muestra el número de filas y los apellidos pendientes.
- **FR-006**: La pantalla **DEBE** mostrar el estado (última copia, pendientes, fallidas con
  motivo legible), permitir reintentar una o todas, y resincronizar un mes de recibos.
- **FR-007**: Todos los textos **DEBEN** estar en español, en composeResources. Los códigos y
  motivos del servidor se traducen a mensajes comprensibles.
- **FR-008**: El entorno Demo **DEBE** imitar las rutas nuevas y los apellidos, para poder probar
  la feature sin servidor ni Google.
- **FR-009**: La app **NO DEBE** contener ni pedir credenciales de Google.

### Key Entities

- **StaffMember**: gana `lastName` (opcional, para las personas antiguas) y `lastNamePending`.
- **Configuración de Google Drive**: disponible, correo de la cuenta técnica, hojas de fichajes y
  recibos (id y enlace), cuándo y quién la configuró.
- **Tarea de copia**: tipo (mes de fichajes, factura, mes de recibos), referencia, estado, filas,
  apellidos pendientes y motivo del error.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Un `ADMIN` configura las dos hojas desde la app en menos de 5 minutos, sin ayuda
  externa.
- **SC-002**: Exportar un mes muestra el resultado en la app en menos de 1 minuto cuando Google
  responde.
- **SC-003**: El 100 % de las altas y ediciones desde la app envían los apellidos.
- **SC-004**: La feature se puede recorrer entera en el entorno Demo, en Android y en iOS.

## Assumptions

- El contrato es el de la spec 012 del backend. La app fija la copia de `docs/openapi.json` en
  cuanto el backend lo publique en su rama.
- La entrada va en el menú de cuenta porque es configuración de empresa, no una tarea diaria.
- La exportación CSV de fichajes no existe en la app y sigue fuera de alcance.
- El selector de mes empieza en el mes actual y retrocede hasta el inicio del plazo de
  conservación.
