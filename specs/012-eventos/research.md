# Investigación: eventos (app)

## D1 — Módulos y arquitectura

`feature/events/{domain,data,database,presentation}`, como inventario. Se aplican las skills de
arquitectura con las adaptaciones a KMP ya justificadas en el proyecto: Koin en lugar de Hilt y
`kotlin.test` en lugar de JUnit 5, porque los tests corren también en iOS; dobles escritos a mano en
lugar de MockK, que no funciona en Kotlin/Native. Del resto, se sigue la skill: casos de uso
`[Verbo][Sujeto]UseCase` con un verbo como función, `[Feature]ScreenUiState` / `UiState` /
`UiAction` / `UiEvent`, `internal` por defecto, pantalla y contenido separados con previews, mapper
de presentación dedicado (las fechas se formatean fuera de los composables) y Turbine para los flujos.

## D2 — Caché sin conexión

Room v1 con `event` (una fila por evento) y los materiales como JSON en una columna: la caché es
solo de lectura, se reemplaza entera al refrescar y nunca se consulta por material. Se vacía al
cambiar de entorno (`EnvironmentScopedData`).

## D3 — Las cantidades se fijan, no se suman

`PUT /api/eventos/{id}/materiales/{materialId} {cantidad}` fija la cantidad final; el contador del
detalle envía la nueva cantidad y el servidor mueve solo la diferencia. El selector limita el
contador a las unidades libres más las que el evento ya lleva de ese material.

## D4 — Barra inferior con seis destinos

El `ADMIN` pasa a tener seis (Fichajes, Inventario, Eventos, Histórico, Facturación, Equipo). La
decisión D11 de facturación limitaba la barra a cinco; la barra reparte el ancho por pesos y con
seis el destino seleccionado sigue mostrando su etiqueta completa (verificado en un iPhone de 402 pt).

## D5 — Inventario: material en uso

El backend responde `409 MATERIAL_EN_USO` al borrar un material asignado a un evento programado; el
inventario lo muestra como error propio (`InventoryError.ProductInUse`).
