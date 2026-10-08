# Quickstart — Validación de la feature

**Feature**: `006-inventario-real` · **Fecha**: 2026-10-08

Contra el backend local de las specs 004 y 005 (puerto 8090), con la cuenta `ENCARGADO`.

| | Escenario | Esperado | Criterio |
|---|---|---|---|
| ☐ | Lista real | Los materiales creados por la API aparecen con todos sus datos | SC-001 |
| ☐ | Buscar y filtrar | Por texto, categoría, estado y agotados | FR-002 |
| ☐ | Sin cobertura | Se ve lo último descargado, con aviso; modificar dice que hace falta conexión | FR-003, FR-014 |
| ☐ | Ajustar cantidad | Nuevo valor en el servidor y entrada en el historial con el motivo; fuera de rango o sin motivo no se envía | US2 |
| ☐ | Alta | El material aparece en el servidor; los campos vacíos o fuera de rango se marcan antes | US3 |
| ☐ | Edición | Los cambios se guardan y las fotos se conservan | FR-010 |
| ☐ | Borrado | Pide confirmación y desaparece del servidor | FR-011 |
| ☐ | Categorías | Crear, renombrar; borrar solo si no tiene materiales | US4 |
| ☐ | Borrado por otra persona | Abrir un material borrado por la API lo quita de la lista con aviso | FR-015 |
| ☐ | Empleada | No ve el inventario | FR-016 |
