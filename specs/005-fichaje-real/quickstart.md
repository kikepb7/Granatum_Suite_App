# Quickstart — Validación de la feature

**Feature**: `005-fichaje-real` · **Fecha**: 2026-10-08

Diez escenarios contra el backend real. La lógica la cubren los tests (research D12). Estos
escenarios cubren lo que solo se ve con el servidor y el dispositivo.

## Requisitos previos

- El backend de Granatum en local, en el puerto 8090, como en la spec 004, con estos dos ajustes:
  - `--timetracking.reloj.tolerancia-pasado=PT2M`, para el escenario 7;
  - `JWT_EXPIRATION_MINUTES=1`, para que la renovación ocurra durante los envíos.
- Cuentas de prueba de la spec 004: una `EMPLEADO` y una `ENCARGADO`.
- La app compilada con `-PBASE_URL_HTTP=http://10.0.2.2:8090/api`.
- La forma de comprobar el servidor: `GET /api/fichajes/empleado/{id}?desde&hasta` con el token
  de la cuenta, o `GET /api/fichajes` con el `ADMIN`.

## Escenario 1 — Jornada completa con cobertura

Entrada, pausa de comida, fin de pausa, salida. **Esperado**: en el servidor, una jornada
`CERRADO` con una pausa `COMIDA` y los minutos trabajados; en la app, todo marcado como enviado
en segundos. (US1, SC-001)

## Escenario 2 — Jornada completa sin cobertura

En modo avión: entrada, pausa, fin de pausa, salida. Desactivar el modo avión. **Esperado**: en
menos de un minuto, una sola jornada en el servidor, con las horas de cuando se pulsó y no de
cuando se envió. (US2, SC-002)

## Escenario 3 — Reintento sin duplicar

Cortar la red justo después de enviar una entrada, de modo que se pierda la respuesta, y
recuperarla. **Esperado**: una sola jornada. En el log del servidor, la segunda petición devuelve
la respuesta original. (SC-003)

## Escenario 4 — Jornada abierta desde otro dispositivo

Abrir jornada por la API con la misma cuenta y, en la app, fichar la entrada sin haberla
refrescado. **Esperado**: la app adopta la jornada del servidor, el estado del día muestra «en
curso» y la pausa y la salida siguientes se registran en esa jornada. (US4-3, FR-011)

## Escenario 5 — Historial real

Consultar el mes con jornadas creadas por la app y por la API. **Esperado**: coinciden al minuto
con el servidor; las pendientes aparecen marcadas; sin cobertura se ve lo último descargado.
(US3, SC-005)

## Escenario 6 — Pausa obligatoria antes de salir

En pausa, intentar fichar la salida. **Esperado**: la app no lo permite y explica que hay que
cerrar la pausa. (FR-004)

## Escenario 7 — Demasiado antiguo

Con la tolerancia del backend en 2 minutos: fichar en modo avión, esperar 3 minutos y recuperar
la red. **Esperado**: el fichaje queda rechazado con el texto de «más de 72 horas», deja de
reintentarse y ofrece pedir una corrección; las demás jornadas se siguen enviando. (US4-1, FR-010,
FR-013)

## Escenario 8 — Reloj adelantado

Adelantar la hora del emulador más de 5 minutos y fichar. **Esperado**: rechazado con el texto de
«la hora de este dispositivo no es correcta». (US4-2)

## Escenario 9 — Pedir una corrección

Sobre una jornada cerrada, proponer otra hora de salida con un motivo. Antes, probar una salida
anterior a la entrada. **Esperado**: lo incoherente se señala antes de enviar; la solicitud
aparece en el servidor como `PENDIENTE` y en la app con ese estado. Si la `ENCARGADO` la rechaza
por la API con un motivo, la app muestra el motivo. (US5)

## Escenario 10 — Migración desde la versión anterior

Instalar la versión de `main` actual, fichar en modo avión de modo que queden pendientes con
dueño, y actualizar a esta versión sin desinstalar. **Esperado**: los fichajes se conservan, se
agrupan en su jornada y se envían con el formato nuevo; los antiguos sin dueño desaparecen.
(FR-023)

## Lista de verificación final

| | Escenario | Criterio |
|---|---|---|
| ☐ | Jornada con cobertura | SC-001 |
| ☐ | Jornada sin cobertura | SC-002 |
| ☐ | Reintento sin duplicar | SC-003 |
| ☐ | Adopción | FR-011 |
| ☐ | Historial real | SC-005 |
| ☐ | Pausa antes de salir | FR-004 |
| ☐ | Demasiado antiguo | FR-013 |
| ☐ | Reloj adelantado | US4-2 |
| ☐ | Corrección | US5 |
| ☐ | Migración | FR-023 |
