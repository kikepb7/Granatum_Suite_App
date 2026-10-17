# Feature Specification: Eventos

**Feature Branch**: `events-feature` · **Created**: 2026-10-17 · **Status**: Draft

**Input**: «Una feature nueva llamada events: otra pantalla y otra opción del bottom bar, para el
admin y para los trabajadores, en la que se crean eventos manualmente. Tarjetas con el nombre
(«Evento 1»), una descripción («Boda de Juan y María») y la fecha. En el detalle se editan los
materiales que se usarán, conectados con el inventario: al añadirlos se reducen en el inventario y
otro evento dispone de los actualizados; sin stock se avisa; cualquier material, eligiendo cuál y la
cantidad. Cuando finaliza un evento se marca como finalizado y los materiales vuelven al inventario.»

Contrato: backend feature 011 (`specs/011-events` del backend, `docs/openapi.json` fijado en
`dc2f907`). Decisiones del responsable: crean y editan `ADMIN`, `ENCARGADO` y `EMPLEADO`;
`REPRESENTANTE` solo consulta; finalización automática al acabar el día y botón «Finalizar ahora»;
sin stock suficiente no se asigna nada y se dice cuántas unidades hay.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Ver y crear eventos (Priority: P1)

Una pestaña «Eventos» en la barra inferior, para todos los roles, con tarjetas: nombre,
descripción, fecha («sáb, 13 jun 2027»), cuándo es («Hoy», «Mañana», «En 5 días»), estado y cuánto
material lleva. Próximos y finalizados por separado. Quien puede escribir crea un evento con
nombre (propuesto «Evento N»), descripción y fecha elegida en un calendario.

1. **Given** la pestaña Eventos, **When** se abre, **Then** se ven los próximos, el más cercano primero.
2. **Given** el formulario, **When** se guarda con nombre y fecha de hoy o futura, **Then** aparece la tarjeta.
3. **Given** un `REPRESENTANTE`, **When** abre Eventos, **Then** los ve pero no hay botón de crear ni de editar.

### User Story 2 - Materiales del evento (Priority: P1)

En el detalle se añaden materiales del inventario: un selector con búsqueda muestra cada material
con su imagen, categoría, descripción, localización y unidades libres; se elige la cantidad con un
contador limitado a lo disponible. Las cantidades se cambian con − / + y se quitan.

1. **Given** 8 unidades libres, **When** se añaden 5, **Then** el evento muestra 5 y el selector 3 libres.
2. **Given** que otra persona se ha llevado unidades mientras tanto, **When** se pide más de lo que
   queda, **Then** se avisa «Solo hay N disponibles» y no se añade nada.
3. **Given** un material del evento, **When** se baja la cantidad o se quita, **Then** las unidades vuelven.

### User Story 3 - Finalizar (Priority: P2)

«Finalizar ahora» con confirmación; el servidor también lo finaliza solo al acabar el día. Un evento
finalizado se ve, pero no se edita.

### Edge Cases

- Sin conexión: la lista y el detalle muestran lo último descargado; cambiar algo exige conexión.
- Un material que ya no existe en el inventario sigue apareciendo en el evento con lo que era.
- Inventario: borrar un material que está en un evento programado avisa de que está en uso.

## Requirements *(mandatory)*

- **FR-001**: Pestaña «Eventos» para `ADMIN`, `ENCARGADO`, `EMPLEADO` y `REPRESENTANTE`.
- **FR-002**: Crear, editar y borrar eventos (nombre 1–100, descripción ≤ 500, fecha de hoy o futura), solo roles que escriben.
- **FR-003**: Añadir cualquier material con cantidad entre 1 y las unidades libres; cambiarla y quitarlo.
- **FR-004**: Mostrar `STOCK_INSUFICIENTE` con las unidades disponibles.
- **FR-005**: Finalizar a mano; un evento finalizado es de solo lectura.
- **FR-006**: Lectura sin conexión desde caché local; escrituras solo con conexión.
- **FR-007**: El modo demo implementa el mismo contrato, compartiendo el stock con su inventario.
