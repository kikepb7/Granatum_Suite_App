# Feature Specification: Fichaje real contra el backend

**Feature Branch**: `fichaje-feature`

**Created**: 2026-10-08

**Status**: Draft

**Input**: Fase 3 del roadmap. Que cada fichaje llegue al registro de jornada del servidor,
también los hechos sin cobertura, en orden, sin duplicarse y sin perderse, y que cada persona
pueda consultar su jornada y pedir que se corrija.

> **Por qué esta fase va antes que inventario y equipo.** Es la función principal de la app y
> tiene valor legal: el registro de jornada es obligatorio. Hoy **ningún fichaje llega al
> servidor**. La app los envía a una ruta que no existe y se quedan en el dispositivo como
> pendientes o fallidos.

> **Estado verificado el 2026-10-08.** La app guarda cada fichaje como un evento suelto y los
> envía uno a uno a `/attendance/events`. El servidor no conoce eventos sueltos. Conoce
> **jornadas**: la entrada crea una jornada y le asigna un identificador, y las pausas y la
> salida se registran sobre esa jornada. Una pausa hecha sin cobertura no se puede enviar hasta
> que el servidor haya asignado identificador a su jornada. El historial y el estado del día se
> calculan solo con lo que hay en el dispositivo. Pedir una corrección no tiene pantalla.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mis fichajes llegan al registro oficial (Priority: P1)

Una persona ficha la entrada, sus pausas y la salida, y todo queda registrado en el servidor con
la hora a la que ocurrió de verdad, haya cobertura o no.

**Why this priority**: Es la razón de ser de la app. Sin ella, el registro de jornada no existe.

**Independent Test**: Con cobertura, fichar entrada, pausa, fin de pausa y salida, y comprobar
en el servidor una jornada cerrada con su pausa y las horas correctas.

**Acceptance Scenarios**:

1. **Given** cobertura, **When** la persona ficha la entrada, **Then** la jornada aparece en el
   servidor en segundos y la app la marca como enviada.
2. **Given** una jornada abierta, **When** la persona inicia una pausa, **Then** elige el tipo
   (comida, descanso u otro) y la pausa queda registrada en esa jornada.
3. **Given** una pausa abierta, **When** la termina, **Then** se registra el fin de esa pausa.
4. **Given** una jornada sin pausas abiertas, **When** la persona ficha la salida, **Then** la
   jornada queda cerrada en el servidor con los minutos trabajados.
5. **Given** cualquier fichaje, **When** se registra, **Then** el servidor guarda la hora en que
   la persona pulsó, no la hora a la que llegó la petición.

---

### User Story 2 - Fichar sin cobertura y que se envíe solo (Priority: P1)

En un sitio sin cobertura, la persona ficha la entrada, una pausa y la salida. Cuando recupera la
conexión, todo se envía solo, en el orden en que ocurrió, sin duplicarse.

**Why this priority**: Es lo que hace útil la app en el trabajo real. Comparte prioridad con la
historia 1 porque un fichaje que solo funciona con cobertura no sirve como registro.

**Independent Test**: En modo avión, fichar entrada, pausa, fin de pausa y salida. Recuperar la
conexión y comprobar en el servidor una sola jornada, completa, con las horas de cuando se
pulsó.

**Acceptance Scenarios**:

1. **Given** que no hay cobertura, **When** la persona ficha, **Then** el fichaje se guarda en
   el dispositivo al instante, el estado del día cambia y queda marcado como pendiente.
2. **Given** varios fichajes pendientes de la misma jornada, **When** vuelve la conexión,
   **Then** se envían en el orden en que ocurrieron, y los que dependen de la entrada esperan a
   que el servidor la haya registrado.
3. **Given** un envío cuya respuesta se perdió, **When** se reintenta, **Then** el servidor no
   lo duplica.
4. **Given** que el envío falla por un problema temporal, **When** ocurre, **Then** se reintenta
   más tarde sin intervención de la persona.

---

### User Story 3 - Ver mi jornada real (Priority: P2)

La persona ve su estado del día y su historial tal como constan en el servidor, junto con lo que
aún tiene pendiente de enviar. Si fichó desde otro dispositivo, también lo ve.

**Why this priority**: Saber qué consta en el registro es un derecho de la persona y evita
dudas. Va detrás del envío porque sin él no habría nada que consultar.

**Independent Test**: Fichar en un dispositivo y abrir la app en otro con la misma cuenta: el
segundo muestra la jornada. Consultar el historial de un mes y comparar con el servidor.

**Acceptance Scenarios**:

1. **Given** jornadas en el servidor, **When** la persona abre el historial, **Then** ve cada
   día con la entrada, las pausas, la salida y las horas trabajadas que calcula el servidor.
2. **Given** fichajes aún no enviados, **When** abre el historial o el estado del día, **Then**
   aparecen junto a lo del servidor, marcados como pendientes.
3. **Given** una jornada abierta desde otro dispositivo, **When** abre la app, **Then** el estado
   del día lo refleja y permite continuar esa jornada.
4. **Given** que no hay cobertura, **When** abre el historial, **Then** ve lo último que se
   descargó, sin errores bloqueantes.
5. **Given** una jornada que el servidor marca como incompleta, corregida o reconstruida,
   **When** la ve, **Then** la app lo indica.

---

### User Story 4 - Saber qué ha fallado y arreglarlo (Priority: P2)

Si un fichaje no se puede registrar de forma definitiva, la persona lo sabe, entiende por qué y
puede pedir que se corrija.

**Why this priority**: Un fichaje rechazado en silencio es peor que uno que no se hizo. Sin esta
historia, los fallos definitivos se reintentarían para siempre o se perderían.

**Independent Test**: Provocar un rechazo definitivo y comprobar que la app lo muestra con su
motivo, deja de reintentarlo y ofrece pedir una corrección.

**Acceptance Scenarios**:

1. **Given** un fichaje pendiente desde hace más de 72 horas, **When** se intenta enviar y el
   servidor lo rechaza por antigüedad, **Then** la app explica que es demasiado antiguo y ofrece
   pedir una corrección.
2. **Given** que la hora del dispositivo está adelantada, **When** el servidor rechaza el fichaje
   por desviación del reloj, **Then** la app avisa de que el reloj del dispositivo no es correcto.
3. **Given** una entrada rechazada porque ya había una jornada abierta en el servidor, **When**
   ocurre, **Then** la app adopta la jornada del servidor en lugar de crear otra, y sigue con los
   fichajes que dependían de ella.
4. **Given** un rechazo definitivo de cualquier otro tipo, **When** ocurre, **Then** el fichaje
   queda marcado como rechazado con su motivo, deja de reintentarse y no bloquea los de otras
   jornadas.
5. **Given** que la persona ya no está activa, **When** se intenta enviar, **Then** la app deja
   de reintentar y lo explica.

---

### User Story 5 - Pedir la corrección de una jornada (Priority: P3)

La persona ve una jornada cerrada con un error, por ejemplo una salida olvidada, y pide que se
corrija indicando los valores correctos y el motivo.

**Why this priority**: El servidor nunca modifica un fichaje directamente: solo con una
corrección aprobada. Es la única vía para arreglar un error, pero no se usa a diario.

**Independent Test**: Desde el historial, pedir la corrección de una jornada cerrada y comprobar
que aparece en el servidor como pendiente, y que la app la muestra con su estado.

**Acceptance Scenarios**:

1. **Given** una jornada cerrada, **When** la persona pide corregirla, **Then** propone entrada,
   salida y pausas y escribe un motivo obligatorio.
2. **Given** unos valores incoherentes (salida antes de la entrada, pausas solapadas), **When**
   los escribe, **Then** la app los señala antes de enviar.
3. **Given** una corrección enviada, **When** consulta esa jornada, **Then** ve la solicitud con
   su estado (pendiente, aprobada o rechazada) y, si se rechazó, el motivo.
4. **Given** una jornada en curso, **When** intenta pedir una corrección, **Then** la app no lo
   permite y explica que solo se corrigen jornadas cerradas.

---

### Edge Cases

- **Jornada que cruza la medianoche.** Pertenece al día en que empezó, igual que en el servidor.
- **Cambio de hora.** Las horas viajan con zona y se muestran en hora local, así que el día de
  23 o 25 horas no descuadra nada.
- **Dos dispositivos con la misma cuenta.** El servidor es la fuente de verdad. Si los dos
  intentan abrir jornada, el segundo adopta la del primero.
- **Pausa sin terminar al fichar la salida.** La app no deja fichar la salida hasta cerrar la
  pausa, igual que el servidor.
- **Fichajes antiguos sin dueño.** Los guardados antes de que existiera el inicio de sesión no
  pertenecen a nadie y se descartan al migrar, sin enviarse nunca.
- **Fichajes de la versión anterior con dueño.** Se conservan y se envían con el formato nuevo,
  siempre que sigan dentro de las 72 horas.
- **Cola larga.** Después de días sin cobertura puede haber muchos fichajes; se envían todos, en
  orden, sin bloquear la interfaz.
- **Sesión caducada durante el envío.** Se renueva sola (spec 004) y el envío continúa.
- **Representante.** No ficha. Su historial es la consulta de la jornada de la plantilla, que
  llega en la fase de equipo; aquí no cambia.

## Requirements *(mandatory)*

### Functional Requirements

**Registro**

- **FR-001**: Cada fichaje MUST registrarse en el servidor como parte de una jornada: la entrada
  la abre, las pausas y la salida se registran sobre ella.
- **FR-002**: Cada fichaje MUST enviarse con un identificador único generado en el dispositivo,
  que permite reintentarlo sin duplicarlo, y con la hora en que ocurrió de verdad.
- **FR-003**: Al iniciar una pausa, la persona MUST elegir su tipo: comida, descanso u otro.
- **FR-004**: La app MUST impedir las transiciones que el servidor rechazaría: abrir jornada con
  otra abierta, abrir pausa con otra abierta, cerrar una pausa que no está abierta y fichar la
  salida con una pausa abierta.

**Sin cobertura**

- **FR-005**: Cada fichaje MUST guardarse en el dispositivo al instante y reflejarse en el
  estado del día sin esperar al servidor.
- **FR-006**: Los fichajes pendientes MUST enviarse en el orden en que ocurrieron. Un fichaje que
  depende de una entrada todavía no registrada MUST esperar a que lo esté.
- **FR-007**: El envío MUST reanudarse solo al recuperar la conexión y periódicamente mientras
  quede algo pendiente, también con la app en segundo plano donde la plataforma lo permita.
- **FR-008**: Un fallo temporal (sin conexión, tiempo agotado, servidor no disponible,
  demasiadas peticiones) MUST reintentarse más tarde.

**Rechazos**

- **FR-009**: Un rechazo definitivo MUST marcar el fichaje como rechazado con su motivo y MUST
  dejar de reintentarlo.
- **FR-010**: Un fichaje rechazado MUST NOT bloquear el envío de los fichajes de otras jornadas.
- **FR-011**: Si el servidor rechaza una entrada porque ya hay una jornada abierta, la app MUST
  adoptar esa jornada y continuar con los fichajes que dependían de la entrada.
- **FR-012**: La app MUST distinguir y explicar en español: fichaje demasiado antiguo, reloj del
  dispositivo desajustado, persona inactiva, transición no válida y error inesperado.
- **FR-013**: Ante un fichaje rechazado por antigüedad, la app MUST ofrecer pedir una corrección
  de esa jornada.

**Consulta**

- **FR-014**: El estado del día y el historial MUST combinar lo que consta en el servidor con lo
  pendiente del dispositivo, distinguiendo una cosa de otra.
- **FR-015**: Las horas trabajadas MUST ser las que calcula el servidor en las jornadas que ya
  constan; en las que no, MUST mostrarse la estimación local como provisional.
- **FR-016**: La app MUST conservar lo último descargado para consultarlo sin cobertura.
- **FR-017**: La app MUST indicar las jornadas incompletas, corregidas o reconstruidas.
- **FR-018**: Una jornada abierta en otro dispositivo MUST reflejarse en el estado del día y MUST
  poder continuarse.

**Correcciones**

- **FR-019**: La persona MUST poder pedir la corrección de una jornada cerrada propia, proponiendo
  entrada, salida y pausas, con un motivo obligatorio.
- **FR-020**: La app MUST validar antes de enviar que la salida es posterior a la entrada y que
  las pausas no se solapan ni salen de la jornada.
- **FR-021**: La app MUST mostrar las correcciones de cada jornada con su estado y, si se
  rechazó, el motivo de la resolución.
- **FR-022**: Pedir una corrección MUST requerir conexión. Sin ella, la app MUST decirlo, sin
  guardar la solicitud para más tarde.

**Datos y migración**

- **FR-023**: Los fichajes anteriores a esta feature sin dueño MUST descartarse al migrar. Los
  que tienen dueño MUST conservarse y enviarse con el formato nuevo.
- **FR-024**: La app MUST NOT enviar ubicación: el servidor la admite como opcional, y pedir
  permiso de localización queda fuera de esta feature.
- **FR-025**: La ruta antigua de envío MUST retirarse.

### Key Entities

- **Jornada**: la unidad del servidor. Entrada, salida, pausas, estado (en curso, cerrada,
  incompleta), minutos trabajados. En el dispositivo tiene además un identificador local, para
  existir antes de que el servidor le asigne el suyo.
- **Fichaje**: una acción de la persona (entrada, inicio de pausa, fin de pausa, salida). Lleva
  su identificador único, la hora en que ocurrió, la jornada a la que pertenece y su estado de
  envío: pendiente, enviado o rechazado con motivo.
- **Pausa**: tipo (comida, descanso, otro), inicio y fin.
- **Corrección**: la petición de cambiar una jornada cerrada. Lleva los valores propuestos, el
  motivo, el estado y, al resolverse, el motivo de la resolución.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los fichajes hechos con cobertura constan en el servidor en menos de
  10 segundos.
- **SC-002**: El 100 % de los fichajes hechos sin cobertura constan en el servidor, en orden y sin
  duplicados, en menos de 1 minuto desde que vuelve la conexión.
- **SC-003**: Reintentar un mismo fichaje cualquier número de veces produce exactamente un
  registro en el servidor.
- **SC-004**: Ningún fichaje rechazado de forma definitiva se reintenta, y ninguno desaparece sin
  que la persona lo vea.
- **SC-005**: El historial de un mes coincide al minuto con lo que consta en el servidor.
- **SC-006**: Pulsar un botón de fichaje actualiza el estado del día en menos de medio segundo,
  con o sin cobertura.
- **SC-007**: Una persona pide la corrección de una jornada en menos de 2 minutos.

## Assumptions

- El servidor es la fuente de verdad del registro. El dispositivo guarda lo pendiente y una copia
  de lo descargado.
- El historial muestra por defecto el mes en curso y deja navegar a meses anteriores.
- La consulta de la jornada de la plantilla (encargada, administración, representante), aprobar
  correcciones y exportar son la fase de equipo.
- Sin ubicación por ahora (FR-024).
- Los textos son solo en español (principio IX).
- La verificación se hace contra el backend local con las cuentas de prueba de la spec 004.

## Dependencies

- Contrato fijado en `docs/openapi.json` (backend `d1857ad`) y el contrato de jornada del backend
  (`specs/001-timetracking/contracts/`), incluidas las reglas de idempotencia y la tolerancia de
  reloj de 5 minutos hacia el futuro y 72 horas hacia el pasado.
- Sesión y fichajes con dueño de la spec 004.

## Out of Scope

- Consulta de la jornada de otras personas, aprobación de correcciones y exportación.
- Ubicación del fichaje.
- Ausencias y notificaciones.
- Fichar en nombre de otra persona.
