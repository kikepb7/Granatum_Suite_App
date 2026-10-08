# Feature Specification: Inicio de sesión, sesión real y navegación por roles

**Feature Branch**: `login-feature`

**Created**: 2026-10-08

**Status**: Draft

**Input**: Fase 2 del roadmap. Que la app entre con las credenciales reales del backend de
Granatum, conserve la sesión sin expulsar a nadie por perder cobertura y muestre a cada persona
solo lo que su rol le permite.

> **Por qué esta feature va antes que inventario, fichaje y equipo.** Todas las rutas del
> backend salvo el inicio de sesión exigen una sesión válida. Sin esta feature no hay forma de
> probar ninguna otra pantalla contra el servidor real.

> **Estado verificado el 2026-10-08.** La app no tiene pantalla de inicio de sesión: sin sesión
> muestra la app como si la persona fuera empleada. La forma en que espera recibir la sesión no
> coincide con la que envía el servidor, así que un inicio de sesión real fallaría hoy. Si la
> renovación de la sesión falla por cualquier motivo, incluida la falta de conexión, la app
> borra la sesión. Conoce tres roles de los cuatro que existen. Y su comprobación local de
> contraseñas no coincide con la política del servidor.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Entrar con mis credenciales (Priority: P1)

Una persona abre la app por primera vez, escribe el correo y la contraseña que le dio la
empresa y entra. Si se equivoca, la app se lo dice con claridad y sin revelar nada que ayude a
quien intente adivinar cuentas ajenas.

**Why this priority**: Es la puerta de todo lo demás. Sin ella la app no puede hablar con el
servidor real y cualquier persona ve pantallas que no le corresponden.

**Independent Test**: Con una cuenta de prueba del servidor de desarrollo, abrir la app sin
sesión, iniciar sesión y comprobar que entra. Repetir con una contraseña incorrecta y con un
correo inexistente: el mensaje debe ser idéntico.

**Acceptance Scenarios**:

1. **Given** que no hay sesión guardada, **When** se abre la app, **Then** solo se ve la
   pantalla de inicio de sesión y ninguna otra parte de la app es alcanzable.
2. **Given** credenciales correctas, **When** se envían, **Then** la persona entra y ve las
   secciones que corresponden a su rol.
3. **Given** un correo inexistente, una contraseña incorrecta, una persona dada de baja o una
   cuenta bloqueada, **When** se intenta entrar, **Then** se muestra el mismo mensaje en los
   cuatro casos.
4. **Given** demasiados intentos seguidos, **When** se intenta entrar, **Then** la app dice
   que hay que esperar e indica cuánto, si el servidor lo comunica.
5. **Given** que no hay conexión, **When** se intenta entrar, **Then** la app dice que no hay
   conexión, sin confundirlo con unas credenciales incorrectas.
6. **Given** un campo vacío o un correo con formato inválido, **When** se intenta enviar,
   **Then** la app lo señala antes de contactar con el servidor.
7. **Given** una sesión iniciada, **When** se cierra la app por completo y se vuelve a abrir,
   **Then** la persona sigue dentro sin volver a escribir sus credenciales.

---

### User Story 2 - Ver solo lo que mi rol permite (Priority: P1)

Cada persona ve las secciones que su rol le permite usar, y ninguna más. Quien representa a la
plantilla consulta el registro de jornada, pero no ficha.

**Why this priority**: Mostrar una sección que el servidor va a rechazar es un error visible y
confunde; mostrar a una persona empleada las pantallas de gestión es además un problema de
confianza. Comparte prioridad con la historia 1 porque sin ella el inicio de sesión lleva a una
app que no corresponde al rol.

**Independent Test**: Iniciar sesión con una cuenta de cada rol y comprobar qué secciones
aparecen y cuáles no son alcanzables de ninguna forma.

**Acceptance Scenarios**:

1. **Given** una persona con rol de administración o de encargada, **When** entra, **Then** ve
   fichaje, historial, inventario y equipo.
2. **Given** una persona empleada, **When** entra, **Then** ve fichaje e historial, y no hay
   forma de llegar a inventario ni a equipo.
3. **Given** una persona representante, **When** entra, **Then** ve el registro de jornada en
   modo consulta, sin ninguna acción para fichar, y no ve inventario ni equipo.
4. **Given** una sesión guardada, **When** se abre la app, **Then** el rol que decide la
   navegación es el que asignó el servidor, no uno por defecto.

---

### User Story 3 - Cambiar la contraseña temporal antes de empezar (Priority: P2)

Quien entra por primera vez con una contraseña temporal, o tras un restablecimiento hecho por
la administración, tiene que elegir una propia antes de poder usar la app. La app le explica
qué requisitos debe cumplir y cuáles le faltan.

**Why this priority**: El servidor no deja hacer nada más a esa sesión, así que sin esta
pantalla esas personas quedan atascadas. Va detrás de las historias 1 y 2 porque solo afecta a
los primeros accesos.

**Independent Test**: Con una cuenta marcada para cambio de contraseña, iniciar sesión y
comprobar que solo aparece la pantalla de cambio; cambiarla y comprobar que se entra en la app
sin volver a iniciar sesión.

**Acceptance Scenarios**:

1. **Given** que el servidor indica que la contraseña debe cambiarse, **When** la persona
   entra, **Then** solo ve la pantalla de cambio de contraseña y ninguna otra sección.
2. **Given** una contraseña nueva que no cumple la política, **When** se escribe, **Then** la
   app muestra qué requisitos faltan antes de enviarla.
3. **Given** que el servidor rechaza la contraseña nueva por la política, **When** se envía,
   **Then** la app muestra exactamente los requisitos que el servidor dice incumplidos.
4. **Given** que la contraseña actual es incorrecta, **When** se envía, **Then** la app lo dice
   sin borrar la contraseña nueva escrita.
5. **Given** un cambio correcto, **When** termina, **Then** la persona entra en la app con su
   rol, sin volver a iniciar sesión.
6. **Given** que la persona sale de la app sin cambiar la contraseña, **When** vuelve a abrirla,
   **Then** sigue en la pantalla de cambio.
7. **Given** la pantalla de cambio obligatorio, **When** la persona prefiere no continuar,
   **Then** puede cerrar sesión.

---

### User Story 4 - No perder la sesión por quedarme sin cobertura (Priority: P2)

Una persona con la sesión iniciada abre la app en un sitio sin conexión, ficha, y la app no la
expulsa. Cuando vuelve la conexión, la sesión se renueva sola y lo pendiente se envía.

**Why this priority**: El fichaje es *offline-first*. Hoy, si la renovación coincide con la
falta de conexión, la app borra la sesión y la persona no puede fichar justo donde más lo
necesita.

**Independent Test**: Con una sesión iniciada y la conexión cortada, esperar a que caduque el
acceso, abrir la app y fichar. Restablecer la conexión y comprobar que se sigue dentro y que
el fichaje se envía.

**Acceptance Scenarios**:

1. **Given** una sesión guardada y sin conexión, **When** se abre la app, **Then** la persona
   entra y puede usar lo que funciona sin conexión.
2. **Given** que la renovación de la sesión falla por falta de conexión o por un error
   temporal del servidor, **When** ocurre, **Then** la sesión se conserva y se reintenta más
   tarde.
3. **Given** varias peticiones que necesitan renovar la sesión a la vez, **When** ocurren,
   **Then** la sesión se renueva una sola vez y todas continúan con la sesión nueva.
4. **Given** que el servidor da la sesión por inválida o revocada, **When** la app lo detecta,
   **Then** borra la sesión y lleva a la pantalla de inicio de sesión, explicando que hay que
   volver a entrar.
5. **Given** que el servidor indica que la persona ya no está activa, **When** la app intenta
   renovar, **Then** cierra la sesión y lo dice expresamente.

---

### User Story 5 - Gestionar mi sesión (Priority: P3)

Una persona puede ver con qué cuenta está dentro, cerrar sesión y cambiar su contraseña cuando
quiera.

**Why this priority**: Es necesario para compartir un dispositivo o reaccionar ante una
sospecha, pero no bloquea el uso diario.

**Independent Test**: Con una sesión iniciada, cerrar sesión con y sin conexión y comprobar que
en ambos casos se vuelve a la pantalla de inicio de sesión; cambiar la contraseña desde la app
y entrar con la nueva.

**Acceptance Scenarios**:

1. **Given** una sesión iniciada, **When** se abre el menú de la cuenta, **Then** se ve el
   correo con el que se entró, su inicial y el rol.
2. **Given** una sesión iniciada con conexión, **When** se cierra sesión, **Then** la sesión se
   revoca en el servidor y se borra del dispositivo.
3. **Given** una sesión iniciada sin conexión, **When** se cierra sesión, **Then** la sesión se
   borra del dispositivo igualmente y la app vuelve a la pantalla de inicio de sesión.
4. **Given** una sesión iniciada, **When** se cambia la contraseña desde el menú de la cuenta,
   **Then** se aplican la misma validación y los mismos mensajes que en el cambio obligatorio, y
   la persona sigue dentro.
5. **Given** fichajes pendientes de enviar, **When** se cierra sesión, **Then** la app avisa
   antes de cerrar de que hay fichajes sin enviar.

---

### Edge Cases

- **Sesión guardada con la forma antigua.** Quien actualice la app desde una versión anterior
  tiene guardada una sesión con otra forma. Debe descartarse sin bloquear el arranque y llevar a
  iniciar sesión.
- **Rol desconocido.** Si el servidor asigna un rol que esta versión de la app no conoce, la app
  no puede adivinar sus permisos: muestra solo lo mínimo, el registro de jornada en consulta, y
  deja traza.
- **Sesión que no se puede interpretar.** Si la sesión recibida no permite saber quién es la
  persona ni su rol, se trata como un inicio de sesión fallido, nunca como un rol por defecto.
- **Contraseña con espacios o caracteres no latinos.** Se envía tal cual la escribió la persona;
  la app no recorta ni transforma la contraseña. El correo sí se recorta y se compara sin
  distinguir mayúsculas.
- **Doble envío.** Pulsar dos veces «Entrar» o «Cambiar» no debe lanzar dos peticiones.
- **Cambio de contraseña en otro dispositivo.** Cambiar la contraseña revoca las demás sesiones
  de la cuenta. El otro dispositivo debe acabar en la pantalla de inicio de sesión con un
  mensaje que no lo presente como un fallo de la app.
- **Persona dada de baja.** Si se desactiva a una persona con sesión iniciada, en su siguiente
  renovación la app la saca y le explica el motivo.
- **Otra persona entra en el mismo dispositivo con fichajes pendientes de la anterior.** El
  servidor atribuye cada fichaje a quien tiene la sesión. Enviar los fichajes pendientes de una
  persona con la sesión de otra los registraría a nombre de la equivocada, que es un error en un
  registro de jornada con valor legal.
- **Reloj del dispositivo desajustado.** La app no decide que la sesión ha caducado por la hora
  del dispositivo; es el servidor quien lo dice.

## Requirements *(mandatory)*

### Functional Requirements

**Inicio de sesión**

- **FR-001**: Sin una sesión válida guardada, la app MUST mostrar solo la pantalla de inicio
  de sesión. Ninguna otra sección MUST ser alcanzable.
- **FR-002**: La pantalla de inicio de sesión MUST pedir correo y contraseña, MUST validar en
  local que ninguno está vacío y que el correo tiene formato válido, y MUST respetar las
  longitudes máximas del servidor (254 para el correo y 128 para la contraseña).
- **FR-003**: Ante credenciales rechazadas, la app MUST mostrar un único mensaje, el mismo para
  correo inexistente, contraseña incorrecta, persona inactiva y cuenta bloqueada.
- **FR-004**: La app MUST distinguir y explicar en español: credenciales rechazadas, demasiados
  intentos (indicando el tiempo de espera si el servidor lo comunica), servicio saturado, falta
  de conexión y error inesperado.
- **FR-005**: Mientras un envío está en curso, la app MUST impedir un segundo envío y mostrar
  que está trabajando.
- **FR-006**: La sesión MUST guardarse en el almacén seguro del dispositivo (principio VII) y
  sobrevivir al cierre completo de la app.

**Identidad y roles**

- **FR-007**: La app MUST obtener el identificador de la persona empleada y su rol de la propia
  sesión que entrega el servidor, sin pedir datos adicionales.
- **FR-008**: La app MUST reconocer los cuatro roles: administración, encargada, empleada y
  representante.
- **FR-009**: La navegación MUST depender del rol de la sesión:
  - Administración y encargada: fichaje, historial, inventario y equipo.
  - Empleada: fichaje e historial.
  - Representante: registro de jornada en modo consulta, sin acciones de fichaje.
- **FR-010**: Un rol desconocido MUST tratarse como el de menor privilegio (consulta del
  registro de jornada) y dejar traza.
- **FR-011**: Una sesión de la que no se pueda obtener la identidad o el rol MUST tratarse como
  un inicio de sesión fallido, nunca como un rol por defecto.

**Cambio de contraseña**

- **FR-012**: Si el servidor indica que la contraseña debe cambiarse, la app MUST mostrar solo
  la pantalla de cambio de contraseña hasta que el cambio tenga éxito o la persona cierre
  sesión, también tras cerrar y reabrir la app.
- **FR-013**: La app MUST validar en local la política del servidor —mínimo 8 caracteres y al
  menos una mayúscula, una minúscula, un dígito y un símbolo— y mostrar qué requisitos faltan
  mientras se escribe.
- **FR-014**: Si el servidor rechaza la contraseña nueva por la política, la app MUST mostrar
  los requisitos que el servidor declare incumplidos.
- **FR-015**: Si la contraseña actual es incorrecta, la app MUST decirlo y conservar lo escrito
  en el campo de la contraseña nueva.
- **FR-016**: Tras un cambio correcto, la app MUST continuar con la sesión nueva que entrega el
  servidor, sin pedir que se inicie sesión de nuevo.
- **FR-017**: Una persona con sesión normal MUST poder cambiar su contraseña desde el menú de la
  cuenta, con la misma validación y los mismos mensajes.

**Continuidad de la sesión**

- **FR-018**: La app MUST renovar la sesión sin intervención de la persona cuando el servidor
  indique que el acceso ha caducado.
- **FR-019**: Si varias peticiones necesitan renovar a la vez, la app MUST renovar una sola vez
  y reutilizar el resultado. Cada renovación invalida la anterior, así que dos renovaciones con
  la misma sesión cerrarían la sesión.
- **FR-020**: La app MUST cerrar la sesión **solo** cuando el servidor la rechace de forma
  definitiva: sesión de renovación inválida, persona inactiva o sesión no autenticada. La falta
  de conexión, un tiempo de espera agotado o un error temporal del servidor MUST NOT cerrar la
  sesión.
- **FR-021**: Al cerrar la sesión porque la persona está inactiva, la app MUST decirlo
  expresamente. En los demás cierres forzados, MUST decir que hay que volver a iniciar sesión.
- **FR-022**: Con una sesión guardada y sin conexión, la app MUST abrirse y permitir lo que
  funciona sin conexión, aunque el acceso haya caducado.
- **FR-023**: La app MUST NOT decidir por la hora del dispositivo que una sesión ha caducado.
- **FR-024**: Una sesión guardada en una forma que esta versión no entiende MUST descartarse sin
  bloquear el arranque, y la app MUST mostrar la pantalla de inicio de sesión.

**Gestión de la sesión**

- **FR-025**: La app MUST mostrar en un menú de la cuenta el correo con el que se inició sesión,
  su inicial y el rol.
- **FR-026**: Cerrar sesión MUST borrar siempre la sesión del dispositivo y volver a la pantalla
  de inicio de sesión. La revocación en el servidor MUST intentarse, y su fallo MUST NOT impedir
  el cierre local.
- **FR-027**: Si hay fichajes pendientes de enviar, la app MUST avisar antes de cerrar sesión y
  dejar que la persona decida.
- **FR-028**: Los fichajes pendientes MUST quedar asociados a la persona que los hizo y MUST
  NOT enviarse nunca con la sesión de otra persona. Si entra otra persona en el dispositivo, los
  pendientes de la anterior se conservan sin enviar hasta que ella vuelva a iniciar sesión.
- **FR-029**: El mecanismo provisional que fabricaba sesiones de desarrollo sin iniciar sesión
  MUST retirarse.

**Privacidad**

- **FR-030**: Ni la contraseña ni la sesión MUST aparecer en ningún registro de la app, tampoco
  en los registros de depuración de las peticiones.

### Key Entities

- **Sesión**: lo que la app conserva entre aperturas. Incluye la credencial de acceso, la de
  renovación, el identificador de la persona empleada, su rol, si está pendiente de cambio de
  contraseña y el correo con el que se entró. Vive solo en el almacén seguro.
- **Rol**: administración, encargada, empleada o representante. Decide qué secciones existen
  para la persona. Lo asigna el servidor.
- **Motivo de cierre**: por qué terminó una sesión que la persona no cerró: inactiva, rechazada
  por el servidor o contraseña cambiada en otro dispositivo. Decide el mensaje de la pantalla de
  inicio de sesión.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Una persona con credenciales válidas entra en la app en menos de 30 segundos
  desde que la abre por primera vez.
- **SC-002**: Los cuatro motivos de credenciales rechazadas producen un mensaje idéntico el
  100 % de las veces.
- **SC-003**: Con cada uno de los cuatro roles, el 100 % de las secciones visibles son las que
  el rol permite, y ninguna de las demás es alcanzable.
- **SC-004**: Con la conexión cortada, una persona con sesión guardada abre la app y ficha en
  el 100 % de los intentos, aunque el acceso haya caducado.
- **SC-005**: Ninguna sesión se cierra por falta de conexión o por errores temporales del
  servidor.
- **SC-006**: Diez peticiones simultáneas con el acceso caducado producen exactamente una
  renovación, y la persona sigue dentro.
- **SC-007**: Quien entra con una contraseña temporal completa el cambio y llega a la app en
  menos de 2 minutos, sin volver a iniciar sesión.
- **SC-008**: Ningún fichaje pendiente se registra a nombre de una persona distinta de la que
  lo hizo, también cuando varias personas usan el mismo dispositivo.
- **SC-009**: Ninguna contraseña ni credencial de sesión aparece en los registros de la app.

## Assumptions

- Las cuentas las crea y restablece la administración en el servidor; esta feature no ofrece
  autoregistro ni recuperación de contraseña olvidada.
- La app no puede mostrar el nombre de la persona: el servidor no ofrece a cada persona sus
  propios datos. Se muestran el correo con que entró, su inicial y el rol.
- La persona representante consulta el registro de jornada con el mismo alcance que el servidor
  le dé. Qué registros ve exactamente lo decide el servidor, no la app.
- La sección «equipo» de administración y encargada se mantiene como existe hoy. Conectarla al
  servidor real es la fase 5.
- Un dispositivo tiene una sola sesión a la vez.
- Los textos son solo en español, como el resto de la app (principio IX).
- Para verificar se usarán cuentas de prueba creadas en el servidor de desarrollo local, una por
  rol y una pendiente de cambio de contraseña.

## Dependencies

- Contrato fijado en `docs/openapi.json` (backend `d1857ad`) y los códigos de error de la feature
  de acceso del backend (ver `docs/api-contract.md`).
- Almacén seguro de la spec 002, que ya guarda la sesión cifrada.
- Servidor de desarrollo de Granatum en local para la verificación en dispositivo.

## Out of Scope

- Autoregistro del personal y su aprobación.
- Gestión de cuentas por la administración.
- Recuperar una contraseña olvidada.
- Biometría y varias cuentas en el mismo dispositivo.
- Inventario, fichaje y equipo contra el servidor real (fases 3 a 5).
