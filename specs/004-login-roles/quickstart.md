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

Recorrida el 2026-10-08 contra el backend `d1857ad` en local, puerto 8090, con la caducidad del
acceso a 1 minuto. Cuentas creadas con el flujo real: primer `ADMIN` por código de arranque y el
resto por registro y aprobación.

| Escenario | Criterio | Android (Pixel 8, emulador) | iOS (iPhone 17 Pro, simulador) |
|---|---|---|---|
| Sin sesión solo hay login | FR-001 | ✅ | ✅ también tras reinstalar: no hereda sesión |
| Entrar con cada rol | SC-003 | ✅ `ENCARGADO` 5 pestañas, `EMPLEADO` 3, `REPRESENTANTE` 2 sin fichar | ⏳ |
| Mensaje idéntico | SC-002 | ✅ correo inexistente, contraseña mala y cuenta desactivada | ⏳ |
| Demasiados intentos | FR-004 | ✅ «… dentro de 6 segundos», del `Retry-After` | ⏳ |
| Sin conexión al entrar | US1-5 | ✅ | ⏳ |
| Sobrevive al reinicio | US1-7 | ✅ sin destello del login | ⏳ |
| Cambio obligatorio | US3 | ✅ sigue tras reiniciar; contraseña actual mala conserva la nueva; el cambio correcto entra sin volver a iniciar sesión | ⏳ |
| Sin cobertura con acceso caducado | SC-004, SC-005 | ✅ abre y ficha en modo avión; al volver la red renueva y sigue dentro | ⏳ |
| Rechazo definitivo | FR-021 | ✅ persona desactivada → «Tu cuenta ya no está activa»; contraseña cambiada en otro dispositivo → «Tu sesión ha terminado» | ⏳ |
| Cerrar sesión con y sin red | FR-026, FR-027 | ✅ aviso con los fichajes pendientes, en plural correcto | ⏳ |
| Fichajes de otra persona | SC-008 | ✅ la encargada no ve ni envía el fichaje pendiente de la empleada; al volver la empleada, lo ve | ⏳ |
| Nada sensible en el log | SC-009 | ✅ 8.354 líneas: 0 contraseñas, 0 JWT, 0 peticiones `/auth/`, `Authorization: ***` | ⏳ |

**iOS, pendiente de recorrer a mano.** Las pruebas de iOS se automatizaron inyectando pulsaciones
de teclado. El simulador interpreta esas pulsaciones con la distribución española del Mac, de modo
que la `@` llega como `"`, y el menú «Pegar» de Compose no responde a toques inyectados. No se pudo
iniciar sesión desde la UI de iOS. Lo que sí está verificado en iOS:

- la app compila y enlaza;
- el escenario 1 se cumple, incluida la reinstalación sin heredar sesión;
- los 67 tests pasan en el simulador de iOS. Cubren la decodificación del token, el mapeo de
  errores, la renovación (diez peticiones simultáneas producen una sola), el cierre por rechazo
  definitivo, el log sin secretos y la propiedad de los fichajes.

Lo único específico de iOS que falta comprobar con una persona al teclado es que la sesión real
sobrevive al reinicio en el Keychain. El almacén ya se verificó en dispositivo en la spec 002.

**Lo que esta verificación no cubre.** El fichaje sigue enviándose a `/attendance/events`, que el
backend no tiene, así que los fichajes quedan pendientes o fallidos. Conectarlos a
`/api/fichajes` es la fase 4. Lo que aquí importa, y se ha comprobado, es que esa petición llega
con la sesión de quien fichó y que su fallo no cierra la sesión.

Y las comprobaciones que hace la CI: `testDebugUnitTest`, `iosSimulatorArm64Test`,
`:composeApp:assembleDebug` y `:composeApp:linkDebugFrameworkIosSimulatorArm64`.
