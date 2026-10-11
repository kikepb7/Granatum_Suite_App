# Phase 0 — Investigación: entornos PRE/PRO y demo

## D1 — Build PRE y build PRO, no un selector en todas

La práctica habitual en apps móviles con entornos: **binarios separados** por entorno de
publicación (Android `applicationIdSuffix`, iOS bundle id por configuración) y, en el binario
interno, un **menú de entorno** para el equipo. El binario de tienda no puede repuntarse: contiene
una sola URL. Se descartó un único binario con selector oculto (un atajo descubierto expone
entornos internos) y los *product flavors* de Android (no llegan a `commonMain` ni a iOS; la
elección por propiedad de Gradle sirve a las dos plataformas y a la CI por igual).

## D2 — Cómo se elige la build

Propiedad `granatum.build` (`pre` por defecto), leída como el resto de la configuración: `-P`,
luego `local.properties`. En iOS, `Config.xcconfig` define `GRANATUM_BUILD` y la fase de build de
Xcode lo pasa a Gradle; el bundle id y el nombre salen de ahí (`.pre`, «Granatum PRE»).
Reemplaza `buildkonfig.flavor` (local/staging/prod): local pasa a ser un entorno más de PRE.
Claves: `BASE_URL_HTTP` (local; por defecto el emulador/simulador), `BASE_URL_HTTP_PRE`,
`BASE_URL_HTTP_PRO` (se aceptan las antiguas `_STAGING`/`_PROD`).

## D3 — Entorno en tiempo de ejecución (solo PRE)

`AppEnvironment` (LOCAL, PRE, PRO, DEMO) en el dominio; `EnvironmentRepository` en datos guarda la
elección en el DataStore de preferencias y la carga **antes de crear el cliente HTTP** (Koin
`createdAtStart`), porque el envío de fichajes en segundo plano puede arrancar sin interfaz.
`constructRoute` lee la URL del entorno activo.

## D4 — Cambiar de entorno sin mezclar datos

Cada feature con datos locales registra un `EnvironmentScopedData` (fichaje e inventario): dice
cuánto trabajo queda sin enviar y sabe borrarse. Cambiar = comprobar pendientes → cerrar sesión
(revocando contra el entorno anterior) → borrar → guardar → activar.

## D5 — Demo en un módulo propio

`:core:demo` contiene el backend simulado (contrato del real, datos de una floristería) y las
cuentas demo. `composeApp` solo depende de él en PRE: el módulo de Koin que lo registra está en
`src/environment/pre`, y en PRO un gemelo vacío en `src/environment/pro`. El cliente HTTP usa un
motor que, en el entorno Demo, envía las peticiones al backend simulado (`SwitchingEngine`); fuera
de Demo, al motor real de la plataforma.

## D6 — PRO endurecido

Sin log de red (`LogLevel.NONE`), `usesCleartextTraffic=false` y configuración de red sin
excepciones, HTTPS obligatoria en la URL de PRO.
