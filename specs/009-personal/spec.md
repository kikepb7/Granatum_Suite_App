# Feature Specification: Alta del propietario y gestión del personal

**Feature Branch**: `personal-feature`

**Created**: 2026-10-09

**Status**: Draft

**Input**: Decisión del responsable (2026-10-09): «el único que puede darse de alta es el
propietario del negocio; cuando un empleado quiera registrarse, será el propietario (ADMIN) el
que lo registre y le proporcionará las credenciales». El backend lo implementó en su feature 009
(`2ec33d0`): la app debe igualarlo. Incluye la gestión del personal, siguiente bloque pedido.

> **Estado verificado el 2026-10-09.** La app no tiene registro, ni alta de personas, ni gestión
> del personal. El contrato fijado pasa a `2ec33d0`: `POST /api/auth/registro` exige el código de
> arranque, desaparecen las solicitudes de registro (`/api/auth/registros`) y aparece
> `POST /api/auth/altas`. Ninguna ruta que la app ya usaba cambia.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - El propietario crea su cuenta (Priority: P1)

En una instalación nueva, el propietario abre la app, elige «Soy el propietario: crear la
cuenta», escribe su nombre, DNI, correo, una contraseña y el código de arranque que le dieron al
desplegar, y entra como administrador.

**Why this priority**: Sin propietario no hay nadie que dé de alta a los demás.

**Independent Test**: En un backend sin ADMIN, crear la cuenta desde la app y comprobar que entra
con las pestañas de administración.

**Acceptance Scenarios**:

1. **Given** una instalación sin administrador, **When** el propietario se registra con el código
   correcto, **Then** entra como ADMIN sin más pasos.
2. **Given** un código incorrecto, o que ya haya un administrador, **When** se registra, **Then**
   la app dice que el código no es válido o que la instalación ya tiene propietario, sin distinguir
   cuál (el servidor no lo distingue).
3. **Given** una contraseña que no cumple la política, un DNI no válido o un correo ya usado,
   **When** se envía, **Then** la app lo señala en el campo.
4. **Given** la pantalla de inicio de sesión, **When** alguien que no es el propietario la abre,
   **Then** queda claro que el acceso se lo da el propietario: no hay registro de empleados.

---

### User Story 2 - Dar de alta a una persona (Priority: P1)

El administrador da de alta a una persona con sus datos (nombre, DNI, puesto, contrato, fecha de
alta) y su acceso (correo y rol). La app le muestra una única vez la contraseña provisional para
que se la entregue; al entrar, la persona tiene que cambiarla.

**Why this priority**: Es la única forma de que el personal acceda a la app.

**Independent Test**: Dar de alta a una persona, iniciar sesión con la contraseña provisional en
otro móvil y comprobar que solo puede cambiarla.

**Acceptance Scenarios**:

1. **Given** el formulario de alta, **When** se completa y se envía, **Then** la app muestra el
   correo y la contraseña provisional, con opción de copiarla, avisando de que no se volverá a ver.
2. **Given** un DNI no válido, un correo ya usado o un DNI que ya tiene cuenta, **When** se envía,
   **Then** la app lo explica y no se crea nada.
3. **Given** un DNI con ficha pero sin cuenta, **When** se da de alta, **Then** la cuenta se
   vincula a esa ficha y la app lo indica.

---

### User Story 3 - Consultar y editar el personal (Priority: P2)

El administrador ve la lista del personal (activo e inactivo), la filtra, abre una ficha y
corrige nombre, puesto, contrato o fecha de alta. El DNI no se puede cambiar.

**Why this priority**: Las fichas cambian poco, pero hay que poder corregirlas.

**Independent Test**: Editar el puesto de una persona y comprobarlo en el servidor.

**Acceptance Scenarios**:

1. **Given** el personal, **When** se abre la lista, **Then** se ve cada persona con nombre,
   puesto, contrato y si está activa; se puede buscar y filtrar por activas o inactivas.
2. **Given** una ficha, **When** se edita y se guarda, **Then** el servidor la guarda.

---

### User Story 4 - Dar de baja, reactivar y restablecer el acceso (Priority: P2)

El administrador desactiva a quien deja la empresa (ya no puede entrar ni fichar) y reactiva a
quien vuelve. Si alguien pierde su contraseña, la restablece y obtiene otra provisional. Si una
ficha no tiene cuenta, le da acceso.

**Why this priority**: Es lo que se hace cuando alguien entra, sale o se olvida la contraseña.

**Independent Test**: Desactivar a una persona y comprobar que no puede entrar; restablecer la
contraseña de otra y entrar con la nueva.

**Acceptance Scenarios**:

1. **Given** una persona activa, **When** se desactiva con confirmación, **Then** queda inactiva y
   la app avisa de que su sesión abierta caduca en minutos, no al instante (regla del servidor).
2. **Given** una persona inactiva, **When** se reactiva, **Then** vuelve a poder entrar.
3. **Given** una persona con cuenta, **When** se restablece su contraseña con confirmación,
   **Then** la app muestra la nueva provisional una vez y sus sesiones abiertas se cierran.
4. **Given** una ficha sin cuenta, **When** se intenta restablecer, **Then** la app ofrece darle
   acceso con un correo y un rol, y muestra la contraseña provisional.

---

### Edge Cases

- **Contraseña provisional perdida** antes de entregarla: se restablece y sale otra.
- **Solo administración** gestiona el personal; la encargada sigue viendo su pantalla de equipo.
- **Sin conexión**: alta, edición, bajas y restablecer exigen conexión; son datos personales y no
  se guardan en el móvil ni en el log de red.
- **Rol del alta**: cualquiera de los cuatro, también ADMIN (así se nombra a un segundo
  administrador).
- **Desactivarse a uno mismo**: la app no lo ofrece sobre la ficha propia.

## Requirements *(mandatory)*

### Functional Requirements

**Registro del propietario**

- **FR-001**: La pantalla de inicio de sesión MUST ofrecer el registro del propietario y MUST NOT
  ofrecer registro para el personal.
- **FR-002**: El registro MUST pedir nombre (hasta 150), DNI o NIE (hasta 20), correo (hasta 254),
  contraseña (política de la spec 004) y código de arranque (hasta 200), todos obligatorios.
- **FR-003**: Tras registrarse, la app MUST iniciar sesión con esas credenciales sin pedirlas otra
  vez.
- **FR-004**: Los errores MUST mostrarse claros: código no válido o instalación con propietario
  (un solo mensaje), correo ya registrado, ficha con cuenta, DNI no válido, contraseña débil con
  sus requisitos, demasiados intentos.

**Alta de personas (ADMIN)**

- **FR-005**: La app MUST dar de alta en un solo paso con nombre (hasta 150), DNI o NIE (hasta
  20), puesto (hasta 100), contrato (jornada completa, parcial, por horas), fecha de alta, correo
  (hasta 254) y rol (ADMIN, ENCARGADO, EMPLEADO, REPRESENTANTE).
- **FR-006**: La app MUST mostrar la contraseña provisional una única vez, con opción de copiarla,
  y MUST NOT guardarla ni registrarla.
- **FR-007**: La app MUST indicar si la cuenta se vinculó a una ficha que ya existía.

**Personal (ADMIN)**

- **FR-008**: La app MUST listar el personal con búsqueda por nombre, DNI o puesto y filtro por
  activo/inactivo.
- **FR-009**: La app MUST permitir editar nombre, puesto, contrato y fecha de alta; el DNI MUST
  mostrarse sin poder editarse.
- **FR-010**: La app MUST permitir desactivar y reactivar con confirmación, salvo la ficha propia.
- **FR-011**: La app MUST permitir restablecer la contraseña con confirmación y, si la ficha no
  tiene cuenta, darle acceso con correo y rol.

**General**

- **FR-012**: Solo ADMIN MUST ver el personal; la pestaña Equipo de ADMIN pasa a ser el personal,
  con acceso a la jornada del equipo.
- **FR-013**: Las rutas de personal y de alta MUST quedar fuera del log de red.
- **FR-014**: El contrato fijado MUST ser el del backend `2ec33d0`.

### Key Entities

- **Persona del personal**: id, nombre, DNI, puesto, contrato, fecha de alta, activa.
- **Credenciales provisionales**: correo, rol, contraseña provisional (solo en memoria y en
  pantalla).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El propietario tiene cuenta y entra en menos de 2 minutos desde abrir la app.
- **SC-002**: Dar de alta a una persona lleva menos de 1 minuto.
- **SC-003**: Ninguna persona del personal puede crear una cuenta por su cuenta desde la app.
- **SC-004**: La contraseña provisional no aparece en ningún log ni almacenamiento del móvil.

## Assumptions

- La contraseña provisional se entrega en persona: no hay correo saliente.
- Las cuentas huérfanas (`/api/auth/cuentas/huerfanas`) no se muestran: la app no borra fichas, que
  es lo único que las crea.

## Dependencies

- Backend `2ec33d0` (feature 009 y 002 del backend) y spec 004 de la app (sesión y cambio
  obligatorio de contraseña).

## Out of Scope

- Ausencias, notificaciones (aplazadas por el responsable).
- Fichajes y correcciones del equipo (siguiente feature).
