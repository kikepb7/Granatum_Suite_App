# Quickstart — Validación de la feature

**Feature**: `004-login-roles` · **Fecha**: 2026-10-08

Doce escenarios. Los tests de `commonTest` cubren la lógica (research D11). Estos escenarios
cubren lo que solo se ve con el servidor real y en dispositivo.

> **Compilar no demuestra nada.** Igual que en la spec 002, lo que importa aquí ocurre contra el
> servidor y en el dispositivo. Un inicio de sesión que no deserializa o una renovación que
> expulsa sin red son compatibles con un `BUILD SUCCESSFUL` y con tests en verde.

## Requisitos previos

1. Backend de Granatum en local en el **puerto 8090** (research D12). El 8080 pertenece a otro
   proceso y no se toca.
2. Cuentas de prueba creadas con el flujo real:
   - Una cuenta por rol: `ADMIN`, `ENCARGADO`, `EMPLEADO` y `REPRESENTANTE`.
   - Una cuenta restablecida por el `ADMIN`, que queda pendiente de cambio de contraseña.
3. La app compilada contra ese backend:

   ```bash
   ./gradlew :composeApp:installDebug -PBASE_URL_HTTP=http://10.0.2.2:8090/api
   ```

   Para el simulador de iOS, en la misma compilación:
   `-PBASE_URL_HTTP_IOS=http://localhost:8090/api`.

## Escenario 1 — Sin sesión solo hay login

Instalar desde cero y abrir. **Esperado**: solo la pantalla de inicio de sesión; ninguna pestaña.
(FR-001, US1-1)

## Escenario 2 — Entrar con cada rol

Iniciar sesión con cada una de las cuatro cuentas. **Esperado**: las pestañas de la tabla de
`data-model.md`. Con la representante, ninguna acción de fichar. (FR-009, SC-003)

## Escenario 3 — Mensaje idéntico ante credenciales rechazadas

Contraseña incorrecta, correo inexistente y, si se puede preparar, una cuenta desactivada.
**Esperado**: el mismo texto en todos los casos. (FR-003, SC-002)

## Escenario 4 — Demasiados intentos

Repetir intentos fallidos hasta agotar el límite del backend. **Esperado**: mensaje de espera con
la cifra de `Retry-After`. (FR-004)

## Escenario 5 — Sin conexión al entrar

Modo avión e intentar entrar. **Esperado**: «sin conexión», no «credenciales incorrectas».
(US1-5)

## Escenario 6 — La sesión sobrevive al reinicio

Entrar, forzar el cierre de la app y reabrir. **Esperado**: dentro, con el mismo rol, sin pasar
por el login ni ver un destello del login. (US1-7, D8)

## Escenario 7 — Cambio obligatorio de contraseña

Entrar con la cuenta restablecida.

**Esperado**:

- Solo la pantalla de cambio.
- Los requisitos se tachan mientras se escribe.
- Con la contraseña actual mal, se muestra el error y se conserva la nueva.
- Al cerrar y reabrir la app sin cambiarla, sigue la pantalla de cambio.
- Tras un cambio correcto, se entra sin volver a iniciar sesión.

(US3, FR-012 a FR-016)

## Escenario 8 — Sin cobertura con el acceso caducado

Esta es la prueba central de la historia 4. Para provocarla, arrancar el backend con una
caducidad del acceso de 1 minuto.

1. Entrar y esperar a que caduque el acceso.
2. Activar el modo avión, abrir la app y fichar.
3. Desactivar el modo avión.

**Esperado**: no hay expulsión en ningún momento; al volver la red se renueva sola y el fichaje
pasa a enviado o, mientras el endpoint de fichaje no sea el real, queda pendiente sin que se
cierre la sesión. (FR-020, FR-022, SC-004, SC-005)

## Escenario 9 — Rechazo definitivo

Con una sesión abierta, desactivar a la persona desde el `ADMIN` y forzar una renovación.
**Esperado**: vuelve al login con el mensaje de cuenta inactiva. Repetir cambiando la contraseña
desde otro dispositivo: vuelve al login con «tu sesión ha terminado». (FR-021)

## Escenario 10 — Cerrar sesión con y sin conexión

Desde el menú de la cuenta, con red y en modo avión. **Esperado**: en ambos casos vuelve al login
de inmediato. Con fichajes pendientes, antes aparece el aviso. (FR-026, FR-027)

## Escenario 11 — Fichajes de otra persona

1. La persona A ficha en modo avión, de modo que queda pendiente, y cierra sesión.
2. La persona B entra con red.

**Esperado**: B no ve el fichaje de A y no se envía con la sesión de B. Cuando A vuelve a entrar,
lo ve pendiente. (FR-028, SC-008)

## Escenario 12 — Nada sensible en el log

Repetir el escenario 2 con el log de depuración abierto (Logcat o la consola del simulador).
**Esperado**: ninguna contraseña, ningún token, ninguna cabecera `Authorization` ni `x-api-key`.
(FR-030, SC-009)

## Lista de verificación final

| | Escenario | Criterio |
|---|---|---|
| ☐ | Sin sesión solo hay login | FR-001 |
| ☐ | Entrar con cada rol | SC-003 |
| ☐ | Mensaje idéntico | SC-002 |
| ☐ | Demasiados intentos | FR-004 |
| ☐ | Sin conexión al entrar | US1-5 |
| ☐ | Sobrevive al reinicio | US1-7 |
| ☐ | Cambio obligatorio | US3 |
| ☐ | Sin cobertura con acceso caducado | SC-004, SC-005 |
| ☐ | Rechazo definitivo | FR-021 |
| ☐ | Cerrar sesión con y sin red | FR-026 |
| ☐ | Fichajes de otra persona | SC-008 |
| ☐ | Nada sensible en el log | SC-009 |

Y las comprobaciones que hace la CI: `testDebugUnitTest`, `iosSimulatorArm64Test`,
`:composeApp:assembleDebug` y `:composeApp:linkDebugFrameworkIosSimulatorArm64`.
