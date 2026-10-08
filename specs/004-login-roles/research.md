# Phase 0 — Investigación: inicio de sesión, sesión real y roles

**Feature**: `004-login-roles` · **Fecha**: 2026-10-08

Verificado contra la copia del contrato (`docs/openapi.json`, backend `d1857ad`), la
documentación del backend en ese commit, el código de la app y las fuentes de Ktor 3.2.3.

## D1 — De dónde salen la identidad y el rol

**Decisión**: de los claims del token de acceso. `sub` es el id de la persona empleada, `role`
el rol y `pwd_change` la marca de cambio pendiente. El payload se decodifica **sin verificar la
firma** con `kotlin.io.encoding.Base64.UrlSafe`, con padding opcional, y `kotlinx.serialization`.
Sin dependencias nuevas.

**Justificación**: el login devuelve solo `ParTokensResponse`; no existe endpoint de «quién soy»
y `GET /api/empleados/{id}` es solo `ADMIN`. El backend documenta los claims como parte de su
contrato (`docs/ARCHITECTURE.md`, «Seguridad y roles»). Verificar la firma no aporta nada en el
cliente: la app no tiene la clave, y quien manipulara el token solo conseguiría ver pantallas que
el servidor le seguiría rechazando.

**Reglas**: un token sin `sub` o con un payload ilegible es un login fallido (FR-011). Un
`role` desconocido se mapea a `DESCONOCIDO`, que recibe el mínimo privilegio y deja traza
(FR-010). `requiereCambioPassword` de la respuesta y `pwd_change` del token deben coincidir; si
discrepan, gana el más restrictivo.

**Alternativa descartada**: una librería JWT. Añade una dependencia para leer tres campos de un
JSON en base64.

## D2 — Forma de la sesión guardada y migración

**Decisión**: la sesión serializada pasa a `{accessToken, refreshToken, employeeId, role,
mustChangePassword, email}` bajo una clave nueva, `granatum.session.secure.v2`. Al arrancar se
borra la clave `v1` del almacén seguro, igual que `LegacySessionCleaner` ya borra la sesión en
claro de la versión anterior.

**Justificación**: la sesión `v1` tiene otra forma (un objeto `user`) y la crearon un flujo sin
login real o el sembrador de desarrollo. No hay nada que migrar: las credenciales de ese formato
nunca salieron de un inicio de sesión real. Con una clave nueva, la lectura nunca se encuentra
datos de otro formato, y borrar la vieja es idempotente.

`expiresIn` no se guarda (FR-023): la caducidad la decide el servidor con
`TOKEN_ACCESO_EXPIRADO`, no el reloj del dispositivo.

## D3 — Errores de autenticación

**Decisión**: un tipo propio en `core/domain`, `AuthError : Error`, con los casos que la UI
necesita distinguir:

| Caso | Origen |
|---|---|
| `InvalidCredentials` | `401 CREDENCIALES_INVALIDAS` (login y contraseña actual) |
| `TooManyAttempts(retryAfterSeconds?)` | `429 DEMASIADAS_PETICIONES` + `Retry-After` |
| `ServiceBusy(retryAfterSeconds?)` | `503 SERVICIO_SATURADO` + `Retry-After` |
| `WeakPassword(requirements)` | `422 PASSWORD_DEBIL` + `requisitos` |
| `Validation` | `400 VALIDACION` |
| `NoInternet` / `Timeout` | Sin respuesta |
| `InvalidSession` | Token sin `sub` o payload ilegible (D1) |
| `Unknown` | Cualquier otra cosa |

El repositorio de auth lee el cuerpo `{code, message}` en lugar de usar el mapeo genérico por
estado HTTP de `responseToResult`. La UI decide por **`code`**, nunca por `message`.

**Justificación**: `DataError.Remote` es un enum por estado HTTP. No distingue
`CREDENCIALES_INVALIDAS` de `TOKEN_ACCESO_EXPIRADO`, ambos `401`, ni puede llevar `Retry-After`
ni `requisitos`. La constitución fija `core/domain` como idéntico al de Squadfy, así que no se
amplía `DataError`. `AuthError` implementa la misma interfaz `Error` y viaja en el mismo
`Result<D, E>`, lo que respeta el principio IV.

**Dato del contrato**: el `openapi.json` del login no declara el `401`. El código está en
`specs/002-auth/contracts/README.md` del backend, que es la fuente de los códigos por ruta según
el propio `info.description` del OpenAPI. Queda anotado en `docs/api-contract.md`.

## D4 — Renovación del token

**Decisión**: se mantiene el plugin `Auth` de Ktor con el proveedor `bearer`, y se añaden tres
cambios en `HttpClientFactory`:

1. `reAuthorizeOnResponse { it.status == 401 && code(it) == "TOKEN_ACCESO_EXPIRADO" }`: solo
   ese código dispara una renovación.
2. Ante `401 NO_AUTENTICADO` en una ruta protegida, la sesión se cierra con el motivo
   `SESSION_REJECTED`.
3. `refreshTokens` clasifica el resultado de `/auth/refresh`:
   - **éxito**: se guarda la sesión nueva;
   - **`401 TOKEN_RENOVACION_INVALIDO`**: se cierra la sesión con `SESSION_REJECTED`;
   - **`401 EMPLEADO_INACTIVO`**: se cierra la sesión con `INACTIVE`;
   - **sin red, timeout o 5xx**: **se conserva la sesión** y se devuelve `null`, de modo que
     esta petición falla como «sin conexión» y la siguiente vuelve a intentarlo.

**Verificado en las fuentes de Ktor 3.2.3**:

- `AuthTokenHolder.setToken` serializa con un `Mutex`. Quien llega mientras otra coroutine
  renueva espera, ve que el valor cambió y **reutiliza el token nuevo sin ejecutar
  `refreshTokens` otra vez**. El refresh token rota en cada uso (contrato de `/auth/refresh`), y esto
  garantiza que no se gasta dos veces (FR-019, SC-006).
- `Auth.findProvider`: sin cabecera `WWW-Authenticate` y con un único proveedor instalado, se usa
  ese proveedor. El backend no envía la cabecera, y no hace falta.
- `AuthConfig.reAuthorizeOnResponse` existe y sustituye al criterio por defecto («cualquier
  `401`»).
- El cuerpo de la respuesta se puede leer dentro del criterio: Ktor 3 guarda en memoria los
  cuerpos que no son streaming, y quien hizo la petición lo vuelve a leer después. **Pendiente
  de confirmar con un test** (D11): si no fuera así, el criterio leería el cuerpo una sola
  vez y lo dejaría en un atributo de la llamada para quien venga detrás.

**Lo que corrige**: hoy cualquier fallo de la renovación, falta de red incluida, ejecuta
`sessionStorage.set(null)`. En una app *offline-first* eso expulsa a la persona en cuanto pierde
cobertura con el acceso caducado (FR-020, SC-005).

**El motivo del cierre** se publica en un `SessionEvents` (un `SharedFlow` en `core/data`) que
escucha la navegación para mostrar el mensaje en la pantalla de inicio de sesión (FR-021).

## D5 — Que nada sensible llegue al log

**Decisión**: en el plugin `Logging`, `filter { !it.url.encodedPath.contains("/auth/") }` y
`sanitizeHeader { it == Authorization || it == "x-api-key" }`.

**Justificación**: hoy el nivel es `LogLevel.ALL` sin filtro. El cuerpo del login, con la
contraseña, y las respuestas con los tokens irían al log de depuración (FR-030, SC-009). Se
filtran las rutas de auth enteras, no solo sus cabeceras, porque lo sensible va en el cuerpo.

## D6 — Fichajes de una persona, nunca con la sesión de otra

**Decisión**: `ClockEventEntity` gana la columna `employeeId: String?`, con migración automática
de Room 1 → 2. El esquema de la versión 1 ya está exportado en
`feature/clockin/database/schemas`. Cada fichaje nuevo guarda el `employeeId` de la sesión. La
sincronización solo envía los pendientes cuyo `employeeId` coincide con la sesión activa, y las
vistas (hoy, historial, contador de pendientes) solo muestran los de esa persona. Las filas
antiguas sin dueño no se envían ni se muestran.

**Justificación**: el servidor atribuye cada fichaje a quien firma la petición. Sin dueño en la
fila, si otra persona inicia sesión en el mismo dispositivo, la cola le enviaría los fichajes de
la anterior: un error en un registro de jornada con valor legal (FR-028, SC-008).

**Por qué las filas sin dueño no se envían**: se crearon sin ningún inicio de sesión real, contra
un endpoint de fichaje que el backend no tiene (`/attendance/events`). La fase 4 reescribe la
sincronización contra `/api/fichajes`; hasta entonces, adjudicárselas a quien inicie sesión sería
precisamente el error que D6 evita.

**Fuera de alcance**: el endpoint de sincronización sigue siendo el antiguo. Conectarlo a
`/api/fichajes` es la fase 4.

## D7 — Estructura: dónde vive cada pieza

**Decisión**:

| Pieza | Módulo |
|---|---|
| `AuthRepository`, `SessionStorage`, `AuthError`, `Session`, `UserRole` | `core/domain` (ya existen; se adaptan) |
| Implementaciones, DTOs, decodificación del token, `SessionEvents` | `core/data` |
| Pantallas de login y cambio de contraseña, sus ViewModels y sus textos | **nuevo** `feature/auth/presentation` |
| Puerta de sesión, navegación por rol y menú de cuenta | `composeApp` |

**Justificación**: el principio I pide la forma de `feature/<nombre>/{domain, database, data,
presentation}`, pero la sesión ya vive en `core` porque la necesita el cliente HTTP compartido, y
moverla a una feature obligaría a que `core/data` dependa de una feature, que el mismo principio
prohíbe. `feature/auth` tiene por eso solo `presentation`; crear `domain`, `data` y `database`
vacíos sería forma sin contenido. El módulo se crea con el convention plugin
`convention.cmp.feature`, igual que los existentes, sin configuración ad hoc (principio XI).

## D8 — Estado de la sesión y navegación

**Decisión**: un `SessionState` en `core/domain`:

```text
Loading ──► SignedOut(reason?) ◄──────────────┐
   │             │  login OK                  │ logout / rechazo definitivo
   │             ▼                            │
   └──► PasswordChangeRequired ──cambio OK──► Active(role)
```

`NavigationRoot` decide por él:

- `Loading`: pantalla de arranque. Evita el destello de la pantalla de login mientras se lee el
  almacén seguro.
- `SignedOut`: solo el grafo de auth, con el motivo si lo hay.
- `PasswordChangeRequired`: solo la pantalla de cambio, más cerrar sesión.
- `Active(role)`: las pestañas del rol.

Pestañas por rol, alineadas con el `SecurityConfig` del backend:

| Rol | Pestañas |
|---|---|
| `ADMIN`, `ENCARGADO` | Fichaje, Historial, Inventario, Equipo |
| `EMPLEADO` | Fichaje, Historial |
| `REPRESENTANTE`, `DESCONOCIDO` | Historial, en modo consulta |

`canManageTeam` se sustituye por propiedades explícitas en `UserRole` (`canClockIn`,
`canManageInventory`, `canSeeTeam`), para que añadir un rol obligue a decidir cada permiso en vez
de heredar uno por agrupación.

## D9 — Política de contraseña

**Decisión**: `PasswordValidator` pasa a la política del backend (FR-023): mínimo 8 caracteres,
mayúscula, minúscula, dígito y símbolo, y máximo 128, el del contrato. Los requisitos usan los
mismos identificadores que el `422` del servidor (`LONGITUD_MINIMA`, `FALTA_MAYUSCULA`…), así la
lista local y la del servidor se pintan con el mismo código.

**Hoy**: 9 caracteres, dígito y mayúscula. Rechaza contraseñas válidas de 8 y acepta otras sin
minúscula ni símbolo que el servidor rechazaría.

**Símbolo** = cualquier carácter que no sea letra, dígito ni espacio en blanco. Es la lectura más
amplia; si el servidor fuera más estricto, su `422` lo corregiría (FR-014).

## D10 — Retirar el sembrador de desarrollo

**Decisión**: se borra `DevSessionSeeder`, su llamada en `App.kt`, su registro en Koin y la
clave `DEV_SESSION_ROLE` de `BuildKonfigConventionPlugin`.

**Justificación**: su propio KDoc dice que existe porque no hay pantalla de login. Ahora la hay,
y el backend local permite crear una cuenta por rol (quickstart). Mantenerlo dejaría una vía de
entrada sin credenciales y además fabrica sesiones con la forma `v1`.

## D11 — Tests

**Decisión**: tests reales en `commonTest`, que se ejecutan en JVM y en el simulador de iOS por
la CI:

- Decodificación del token: claims válidos, rol desconocido, payload ilegible, sin `sub`,
  padding ausente.
- `PasswordValidator`: cada requisito por separado y los límites de longitud.
- Mapeo de respuestas a `AuthError`: cada código de la tabla de D3, `Retry-After` presente y
  ausente, `requisitos`.
- Máquina de estados de la sesión (D8).
- La decisión de renovar: qué respuestas renuevan, cuáles cierran sesión y cuáles la conservan
  (D4), con el `MockEngine` de Ktor. Incluye diez peticiones simultáneas con el acceso caducado
  y comprueba que hay una sola renovación (SC-006).

`MockEngine` (`io.ktor:ktor-client-mock`) es la única dependencia nueva, solo de test, y se
declara en el catálogo (principio XI). El trinquete de cobertura se sube a la cifra medida al
terminar, redondeada hacia abajo (principio VIII).

**Medido al terminar (2026-10-08)**: 67 tests, en verde en JVM y en el simulador de iOS.
Cobertura de líneas 529 / 5.740 = **9,22 %** (antes, 0 / 4.843). `granatum.coverage.minLine`
pasa de 0 a **9**.

## D12 — Verificación contra el backend real

**Decisión**: el backend de Granatum se levanta en local en el puerto **8090**. El 8080 lo ocupa
otro proceso del usuario y no se toca. La app se compila con
`-PBASE_URL_HTTP=http://10.0.2.2:8090/api` para el emulador de Android y con
`-PBASE_URL_HTTP_IOS=http://localhost:8090/api` para el simulador de iOS (spec 001). Las cuentas de prueba se crean con el
flujo real: el primer `ADMIN` con el código de arranque, y el resto con el registro y la
aprobación de ese `ADMIN`, una por rol y una restablecida para forzar el cambio de contraseña.

**Riesgo conocido**: el `docker compose` del backend publica PostgreSQL en el 5432, que ya usa el
PostgreSQL de Squadfy. Se resuelve en la verificación levantando la base del backend en otro
puerto, sin tocar los contenedores de Squadfy.

## Resumen

No queda ninguna incógnita abierta. La única dependencia nueva es `ktor-client-mock`, solo para
tests.
