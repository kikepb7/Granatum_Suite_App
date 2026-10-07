# Feature Specification: Sesión en almacén seguro y navegación por rol

**Feature Branch**: `002-secure-session-storage`

**Created**: 2026-10-05

**Status**: Draft

**Input**: Guardar las credenciales de sesión en el almacén seguro del sistema y comprobar que
la navegación por rol se comporta como debe.

> **Por qué esta spec es más estrecha que el enunciado original.** La spec 2 pedía además
> inicio de sesión con correo y contraseña, cambio obligatorio de contraseña temporal y
> renovación automática de sesión. Nada de eso es construible hoy. Verificado en el
> repositorio del backend el 2026-10-05: solo existe un extremo de autenticación, y es de
> desarrollo; no hay inicio de sesión real, ni entidad de persona usuaria, ni concepto de
> contraseña. El propio código del backend documenta que ese flujo llegará con su módulo de
> empleados. Inventar aquí el contrato incumpliría el principio VI de la constitución, así
> que se difiere. Ver «Deferred» al final.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Las credenciales no quedan a la vista (Priority: P1)

Una persona usa la aplicación en su teléfono de trabajo. Si ese teléfono se pierde, se repara
o cae en manos de alguien con conocimientos, sus credenciales de sesión no pueden leerse
hurgando en el almacenamiento de la aplicación.

**Why this priority**: Es la razón de ser de esta feature y una brecha declarada de la
constitución. Hoy las credenciales se guardan en texto plano, de modo que cualquiera con
acceso al almacenamiento de la app las lee tal cual. Mientras eso siga así, todo lo demás que
construyamos encima hereda el problema.

**Independent Test**: Inspeccionar el almacenamiento de la aplicación en Android y en iOS tras
haber guardado una sesión, y comprobar que no aparece ningún valor de credencial legible.

**Acceptance Scenarios**:

1. **Given** una sesión guardada, **When** se inspecciona el almacenamiento de la aplicación
   en Android, **Then** no se encuentra ninguna credencial en texto legible.
2. **Given** una sesión guardada, **When** se inspecciona el almacenamiento de la aplicación
   en iOS, **Then** tampoco se encuentra.
3. **Given** una sesión guardada, **When** se cierra la aplicación por completo y se vuelve a
   abrir, **Then** la sesión sigue disponible y la aplicación se comporta igual que antes.
4. **Given** una sesión guardada, **When** se borra la sesión, **Then** deja de existir en el
   almacén y la aplicación vuelve al estado de persona no identificada.

---

### User Story 2 - Cada rol ve lo que le corresponde (Priority: P2)

Una persona empleada abre la aplicación y solo encuentra su fichaje y su historial. Una
encargada o una administradora encuentran además el inventario y el panel de equipo.

**Why this priority**: La lógica ya está escrita, pero **nunca se ha comprobado con sesiones
reales de cada rol**, porque no había forma de iniciar sesión. Una restricción de acceso que
nadie ha visto funcionar es una suposición, no un control.

**Independent Test**: Cargar sucesivamente una sesión de cada rol y recorrer la aplicación
comprobando qué áreas aparecen y cuáles no.

**Acceptance Scenarios**:

1. **Given** una sesión con rol de persona empleada, **When** se recorre la aplicación,
   **Then** solo son alcanzables el fichaje y el historial propio.
2. **Given** una sesión con rol de encargada, **When** se recorre la aplicación, **Then**
   también son alcanzables el inventario y el panel de equipo.
3. **Given** una sesión con rol de administradora, **When** se recorre la aplicación,
   **Then** el resultado es el mismo que para encargada.
4. **Given** ninguna sesión, **When** se abre la aplicación, **Then** se aplica el nivel de
   acceso más restringido, nunca el más permisivo.

---

### User Story 3 - Actualizar la aplicación no deja a nadie atrapado (Priority: P3)

Alguien que ya tenía la aplicación instalada la actualiza. Su sesión estaba guardada en claro
por la versión anterior. Tras actualizar, esa sesión se descarta y la aplicación arranca
limpiamente en el estado inicial, sin dejar rastro de la credencial antigua.

**Why this priority**: Afecta a pocas personas y hoy a ninguna en producción, pero un estado
inconsistente de sesión es de los fallos más difíciles de diagnosticar a distancia.

**Independent Test**: Instalar la versión anterior, guardar una sesión, actualizar a la nueva
y abrir la aplicación.

**Acceptance Scenarios**:

1. **Given** una sesión guardada en claro por la versión anterior, **When** se actualiza y se
   abre la aplicación, **Then** arranca en el estado de persona no identificada, sin
   bloquearse ni mostrar un estado incoherente.
2. **Given** esa misma situación, **When** se completa el arranque, **Then** no queda ninguna
   copia legible de la credencial anterior en el almacenamiento.

---

### Edge Cases

- **El almacén seguro no está disponible.** Un dispositivo puede denegar el acceso al almacén
  —sin bloqueo de pantalla configurado, perfil restringido, almacén corrupto—. La aplicación
  debe comportarse como si no hubiera sesión en lugar de cerrarse, y la persona debe poder
  seguir usando lo que no requiera identificación.
- **Lectura y escritura simultáneas.** Si dos partes de la aplicación consultan la sesión a la
  vez mientras se está guardando, ninguna debe leer un valor a medio escribir.
- **Credencial corrupta o ilegible.** Si lo almacenado no puede interpretarse, debe tratarse
  como ausencia de sesión y descartarse, nunca propagarse como un fallo que bloquee el
  arranque.
- **Desinstalación.** Al desinstalar la aplicación no debe quedar ninguna credencial
  recuperable por una instalación posterior.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Las credenciales de sesión MUST guardarse en el almacén protegido que ofrece
  cada plataforma, no en el almacenamiento corriente de la aplicación.
- **FR-002**: Ninguna credencial MUST quedar en texto legible en el almacenamiento de la
  aplicación, en ninguna de las dos plataformas.
- **FR-003**: El resto de la aplicación MUST seguir consultando la sesión exactamente igual
  que hasta ahora: el cambio de dónde se guarda no puede obligar a tocar a quien la consume.
- **FR-004**: La sesión MUST sobrevivir al cierre completo de la aplicación y a su reinicio.
- **FR-005**: Borrar la sesión MUST eliminarla del almacén, sin dejar rastro recuperable.
- **FR-006**: Si el almacén seguro no está disponible, la aplicación MUST comportarse como si
  no hubiera sesión y continuar funcionando, en lugar de interrumpirse.
- **FR-007**: Una credencial ilegible o corrupta MUST tratarse como ausencia de sesión.
- **FR-008**: Al actualizar desde una versión que guardaba la sesión en claro, la aplicación
  MUST descartar esa sesión y volver al estado de persona no identificada. No se migra: una
  credencial que estuvo en texto legible deja de considerarse fiable.
- **FR-009**: Tras esa actualización, NO MUST quedar ninguna copia legible de la credencial
  anterior.
- **FR-010**: El acceso a inventario y al panel de equipo MUST restringirse a los roles de
  encargada y administradora.
- **FR-011**: Sin sesión, la aplicación MUST aplicar el nivel de acceso más restringido.
- **FR-012**: El comportamiento por rol MUST quedar comprobado con una sesión real de cada uno
  de los tres roles, no solo razonado sobre el código.

### Key Entities

- **Sesión**: lo que acredita a una persona frente al backend y determina qué puede ver. Se
  guarda como una unidad, se observa de forma continua y puede no existir. Su contenido es
  sensible; su presencia o ausencia, no.
- **Rol**: el nivel de acceso de la persona identificada. Tres valores: empleada, encargada y
  administradora. Las dos últimas comparten permisos de gestión. Viene dentro de la sesión y
  la aplicación no lo decide por su cuenta.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una inspección del almacenamiento de la aplicación no revela ninguna credencial
  legible, en ninguna de las dos plataformas.
- **SC-002**: La sesión sobrevive al 100 % de los reinicios de la aplicación.
- **SC-003**: Los tres roles se comprueban con una sesión real y cada uno alcanza exactamente
  las áreas que le corresponden, ni una más.
- **SC-004**: Con el almacén seguro no disponible, la aplicación arranca igualmente y permite
  usar lo que no requiere identificación.
- **SC-005**: Tras actualizar desde la versión anterior no queda ninguna credencial legible y
  la aplicación arranca en un estado coherente.

## Assumptions

- Cada plataforma ofrece un almacén protegido por el sistema, respaldado por hardware cuando
  lo hay. Es la vía prevista por el principio II de la constitución para esta clase de dato.
- El contrato actual de consulta de la sesión es suficiente y no necesita ampliarse: esta
  feature cambia dónde se guarda, no qué se guarda ni cómo se consulta.
- La lógica de navegación por rol ya está escrita y se da por correcta hasta que la
  comprobación demuestre lo contrario. Si falla, corregirla entra en esta feature.
- Para obtener sesiones de cada rol se usará el extremo de desarrollo que el backend ya
  expone. Es un andamio de verificación, no una vía de acceso de la aplicación, y no se
  integra en ninguna pantalla.
- No hay nadie en producción, así que una actualización que descartase la sesión no afectaría
  a ninguna persona real hoy.

## Dependencies

- Ninguna sobre el backend para el primer bloque: el almacenamiento seguro es enteramente
  local.
- El segundo bloque necesita que el backend esté levantado en perfil de desarrollo para poder
  emitir sesiones de cada rol.
- Esta feature **no** depende del contrato de API publicado, a diferencia de las specs 3 y 5.

## Deferred

Lo que el enunciado original de la spec 2 pedía y aquí no se hace, con su motivo. Vuelve
cuando el backend tenga su módulo de empleados:

| Diferido | Motivo |
|---|---|
| Pantalla de inicio de sesión | El backend no tiene flujo de autenticación |
| Contraseña temporal y cambio obligatorio | No existe el concepto de contraseña ni entidad de persona usuaria |
| Renovación automática de sesión contra el backend | El extremo no existe |
| Cierre de sesión contra el backend | El extremo no existe |

## Out of Scope

- Inventario (spec 3), fichaje (spec 4) y panel de encargado (spec 5).
- Biometría o cualquier otra verificación de la persona para desbloquear la aplicación.
- Cifrado de la base de datos local: esta feature cubre las credenciales, no los datos de
  negocio.
