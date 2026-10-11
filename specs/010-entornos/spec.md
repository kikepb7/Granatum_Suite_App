# Feature Specification: Entornos PRE y PRO, y usuarios demo en PRE

**Feature Branch**: `entornos-feature` · **Created**: 2026-10-11 · **Status**: Draft

**Input**: «Necesito un entorno de PRO y otro de PRE, con un botón únicamente en PRE para elegir
en qué entorno probar (PRE o PRO). Además, usuarios DEMO, mediante una librería o módulo concreto,
que solo se puedan probar en PRE.»

> **Estado verificado el 2026-10-11 (`main`).** El entorno se elige al compilar con
> `-Pbuildkonfig.flavor=local|staging|prod`, sin selector en la app. Las tres builds comparten el
> identificador `com.granatum.app` (no pueden convivir en un móvil), el nombre «Granatum Suite»,
> el log de red completo y el tráfico sin cifrar permitido. No hay usuarios demo en `main` (la
> rama `demo-feature` tiene un backend simulado sin fusionar).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dos apps: PRE para probar y PRO para trabajar (Priority: P1)

El equipo instala en el mismo móvil «Granatum PRE» y «Granatum Suite». La de PRO es la que se
publica en las tiendas: habla solo con producción y no tiene nada de pruebas.

**Acceptance Scenarios**:

1. **Given** las dos builds, **When** se instalan, **Then** conviven con nombre e identificador
   distintos.
2. **Given** la build PRO, **When** se inspecciona, **Then** no contiene la URL de PRE, ni el
   selector, ni el backend demo, ni log de red, ni tráfico sin cifrar.

### User Story 2 - Elegir el entorno en PRE (Priority: P1)

En la app PRE, la persona que prueba elige el entorno desde el inicio de sesión o desde su cuenta:
Local (su ordenador), PRE, PRO o Demo. Siempre ve en qué entorno está.

**Acceptance Scenarios**:

1. **Given** la app PRE, **When** se abre el login o la cuenta, **Then** aparece el entorno actual
   y la opción de cambiarlo; en la app PRO no aparece.
2. **Given** un cambio de entorno, **When** se confirma, **Then** se cierra la sesión, se borran
   los datos guardados del entorno anterior y se recuerda la elección al reabrir la app.
3. **Given** fichajes pendientes de enviar, **When** se intenta cambiar, **Then** la app lo impide
   y dice cuántos quedan, para que no se envíen al servidor equivocado.
4. **Given** una sesión abierta en PRE, **When** se navega, **Then** una franja indica el entorno
   (resaltada si es producción).

### User Story 3 - Usuarios demo, solo en PRE (Priority: P2)

Eligiendo «Demo» en la app PRE, la app entra sin servidor como administración, con datos de
ejemplo; al cerrar sesión se puede entrar como empleado o volver como administración con un toque.

**Acceptance Scenarios**:

1. **Given** el entorno Demo, **When** se selecciona, **Then** la app entra como administración.
2. **Given** la sesión demo cerrada, **When** se abre el login, **Then** ofrece las cuentas demo.
3. **Given** la app PRO, **When** se busca el modo demo, **Then** no existe: su módulo no se
   enlaza.

### Edge Cases

- PRE sin URL de PRE o de PRO configurada: esa opción no aparece.
- La revocación de la sesión al cambiar de entorno se intenta contra el entorno anterior; sin
  conexión, la sesión local se borra igualmente.
- Los datos demo se pierden al cerrar la app.

## Requirements *(mandatory)*

- **FR-001**: La build MUST elegirse al compilar con `granatum.build=pre|pro` (propiedad de Gradle,
  `local.properties` o ajuste `GRANATUM_BUILD` de Xcode); por defecto `pre`.
- **FR-002**: PRE y PRO MUST tener identificador y nombre distintos en Android e iOS.
- **FR-003**: La build PRO MUST contener solo la URL de producción (HTTPS obligatoria), sin log de
  red, sin tráfico sin cifrar y sin el módulo demo.
- **FR-004**: La build PRE MUST permitir elegir entre los entornos configurados (Local, PRE, PRO)
  y Demo, desde el login y desde la cuenta, y MUST recordarlo.
- **FR-005**: Cambiar de entorno MUST cerrar la sesión y borrar los datos locales de todas las
  features; MUST impedirse si hay fichajes sin enviar.
- **FR-006**: La build PRE MUST mostrar el entorno activo durante la sesión.
- **FR-007**: Los usuarios demo MUST vivir en un módulo propio (`:core:demo`) que solo se enlaza en
  PRE, con un backend simulado que respeta el contrato del real.
- **FR-008**: El entorno Demo MUST entrar como administración y ofrecer las cuentas demo en el
  login.

## Success Criteria *(mandatory)*

- **SC-001**: Cero referencias al módulo demo y a URLs no productivas en el binario PRO.
- **SC-002**: Cambiar de entorno en PRE lleva menos de 10 segundos.
- **SC-003**: Ningún fichaje se envía a un entorno distinto del que se registró.

## Out of Scope

- Distribución (TestFlight, Firebase App Distribution, tiendas) y firma: ver la viabilidad.
- Icono distinto para PRE.
