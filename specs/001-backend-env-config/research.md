# Phase 0 — Investigación: configuración por entorno y limpieza de autenticación

**Feature**: `001-backend-env-config` · **Fecha**: 2026-10-05

Todo lo que sigue está verificado contra el código, no inferido.

## D1 — Mecanismo de selección de entorno

**Decisión**: usar los *flavors* de BuildKonfig (`defaultConfigs("local") { }`,
`defaultConfigs("staging") { }`, `defaultConfigs("prod") { }`) y seleccionarlos al construir
con la propiedad Gradle `-Pbuildkonfig.flavor=<entorno>`. El `defaultConfigs { }` sin nombre
queda como base común.

**Justificación**: `BuildKonfigConventionPlugin` ya existe (BuildKonfig 0.17.1) y ya aplica
el patrón exacto que la feature necesita —leer de `local.properties` con
`gradleLocalProperties` y fallar con `IllegalStateException` si falta la clave—, hoy solo
para `API_KEY`. Extenderlo cuesta mucho menos que introducir un mecanismo nuevo y respeta el
principio XI.

**Alternativas descartadas**:

- *Build types / product flavors de AGP*: solo existen en Android. No sirven para iOS, y
  esta feature exige paridad.
- *expect/actual por plataforma*: violaría el principio II. La diferencia entre entornos es
  de configuración de build, no de capacidad de plataforma.
- *Variable de entorno en tiempo de ejecución*: no hay dónde leerla en móvil, y permitiría
  repuntar un binario de producción.

## D2 — La dirección del anfitrión difiere entre emulador y simulador

**Decisión**: adoptar la cadena de resolución de Squadfy_KMM —propiedad de Gradle, luego
`local.properties`, luego valor por defecto— **y cerrar el hueco que Squadfy deja**, usando
`targetConfigs` de BuildKonfig para que el target de iOS tome `localhost` sin que nadie
tenga que acordarse de nada.

Verificado en el jar de BuildKonfig 0.17.1: `BuildKonfigExtension` expone
`defaultConfigs(Action)`, `defaultConfigs(String, Action)`, `targetConfigs(Action)` y
`targetConfigs(String, Action)`. La configuración por target existe y está disponible.

**Qué hace Squadfy hoy** (`build-logic/.../BuildKonfigConventionPlugin.kt`):

```kotlin
fun resolve(key: String, default: String): String =
    providers.gradleProperty(key).orNull ?: localProperties.getProperty(key) ?: default
```

Con `DEFAULT_BASE_URL_HTTP = "http://10.0.2.2:8080/api"` para **todos** los targets. Su
propio KDoc reconoce la limitación: *«Any real build (release APK, iOS, desktop) must
override these via `-PBASE_URL_HTTP=...`»*.

**Por qué no basta copiarlo tal cual**: un único valor no puede servir a ambas plataformas
a la vez. `10.0.2.2` solo lo entiende el emulador de Android; en el simulador de iOS no
resuelve. Y `localhost` en el emulador de Android apunta al propio emulador, no al Mac. Con
un solo valor hay que editar configuración entre una ejecución y otra, lo que incumple
SC-001 y FR-008. Además, iOS se construye desde Xcode, donde pasar `-P...` es incómodo.

**Resultado**: se conserva la cadena `resolve()` de Squadfy —es buena y permite inyectar
desde CI sin tocar ficheros— y encima se añade un `targetConfigs` que da a iOS su propio
valor por defecto. Android sigue funcionando exactamente igual que en Squadfy; iOS pasa a
funcionar sin intervención.

**Alternativas descartadas**:

- *Copiar Squadfy literalmente, con override manual para iOS*: funciona, pero obliga a
  recordar el override en cada ejecución desde Xcode. Es la causa de que iOS contra backend
  local no se use en Squadfy.
- *`expect/actual` con la URL*: el principio II reserva `expect/actual` a cinco áreas y esta
  no es una de ellas. Además metería configuración en código fuente, contra FR-002.
- *Detección en tiempo de ejecución de la plataforma*: resuelve el síntoma metiendo una
  rama condicional en código compartido, y deja la URL como literal.

## D3 — Validación de que producción va cifrada

**Decisión**: validar en el propio convention plugin, en tiempo de configuración de Gradle.
Si el flavor es `staging` o `prod` y la URL no empieza por `https://`, lanzar
`IllegalStateException` y detener la construcción.

**Justificación**: FR-003 y SC-003 exigen que sea imposible publicar en claro, y el
principio V lo refuerza. El plugin ya tiene el patrón de fallo rápido para `API_KEY`, así
que es una línea más en el mismo sitio. Validar en tiempo de ejecución llegaría tarde: el
binario ya existiría.

**Alternativas descartadas**:

- *Revisión manual antes de publicar*: es exactamente lo que el criterio de éxito quiere
  eliminar.
- *`networkSecurityConfig` de Android*: es específico de Android y no cubre iOS.

## D4 — Alcance real de la limpieza de autenticación

**Decisión**: retirar **cinco** métodos, no cuatro. El inventario encontró uno que no
figuraba en el enunciado.

`AuthRepository` expone hoy ocho métodos:

| Método | Destino |
|---|---|
| `login` | **conservar** |
| `changePassword` | **conservar** |
| `logout` | **conservar** |
| `register` | retirar |
| `resendVerificationEmail` | retirar — **no estaba en el enunciado original** |
| `verifyEmail` | retirar |
| `forgotPassword` | retirar |
| `resetPassword` | retirar |

Rutas en `AuthRoutes`: conservar `LOGIN_ROUTE`; retirar `REGISTER_ROUTE`,
`VERIFY_EMAIL_ROUTE`, `RESEND_VERIFICATION_ROUTE`, `FORGOT_PASSWORD_ROUTE` y
`RESET_PASSWORD_ROUTE` (cinco, no dos).

**Justificación**: `resendVerificationEmail` pertenece al mismo flujo de verificación por
correo que el producto no usa. Dejarlo sería incoherente y volvería a aparecer como código
muerto en la siguiente revisión.

## D5 — DTOs huérfanos tras la limpieza

**Decisión**: eliminar `RegisterRequestDTO`, `ResetPasswordRequestDTO` y `EmailRequestDTO`.
**Conservar** `RefreshRequestDTO`, `LoginRequestDTO`, `ChangePasswordRequestDTO`,
`AuthInfoSerializableDTO` y `UserSerializableDTO`.

**Justificación**: se rastreó cada DTO. Los tres primeros solo los usa
`KtorAuthRepositoryImpl` en los métodos que se retiran, así que quedan huérfanos.
`RefreshRequestDTO` parece huérfano a primera vista —`AuthRepository` no declara ningún
`refresh`— pero lo usa también `HttpClientFactory.kt` para la renovación de token. Borrarlo
rompería la sesión.

## D6 — No hay interfaz de usuario de autenticación

**Decisión**: la limpieza no toca ninguna pantalla ni ruta de navegación.

**Justificación**: no existe ningún módulo ni carpeta de presentación de `auth` en el
repositorio. Los flujos a retirar viven únicamente en `core/domain` y `core/data`. Reduce
notablemente el riesgo del bloque 2.

**Consecuencia para la spec**: el escenario 1 de la historia 3 («la persona no ve opciones
de crear cuenta») se satisface de forma trivial, porque no hay pantalla de acceso todavía.
La verificación real es el escenario 2, sobre el código.

## D7 — Tests afectados: ninguno

**Decisión**: no hay que retirar ni reescribir ningún test.

**Justificación**: la búsqueda de referencias devolvió tres falsos positivos que conviene
dejar documentados para que nadie los toque por error:

| Fichero | Coincidencia | Por qué NO es de auth |
|---|---|---|
| `ConnectivityObserver.android.kt` | `registerNetworkCallback` | API de conectividad de Android |
| `rememberImagePickerLauncher.ios.kt` | `registeredTypeIdentifiers` | API del selector de fotos de iOS |
| `build-logic/convention/build.gradle.kts` | `register` | registro de plugins de Gradle |

No existe ningún fichero de test que ejercite los flujos retirados.

## D10 — `UrlConstants` se conserva como envoltorio, no se elimina

**Decisión**: mantener `UrlConstants` convertido en un envoltorio fino sobre `BuildKonfig`,
exactamente como lo hace Squadfy:

```kotlin
object UrlConstants {
    val BASE_URL_HTTP = BuildKonfig.BASE_URL_HTTP
}
```

**Justificación**: rectifica el enfoque inicial de esta investigación, que proponía borrar
el fichero y reescribir sus consumidores. El envoltorio deja `HttpClientExt.kt` **sin tocar**
—sigue importando `UrlConstants.BASE_URL_HTTP`—, de modo que el cambio queda confinado a dos
ficheros en lugar de propagarse. También da un punto único donde poner la documentación de
que el valor llega inyectado, y aísla al resto del código del nombre del objeto generado por
BuildKonfig.

Las constantes pasan de `const val` a `val`, porque un valor generado no es una constante
de compilación.

**Alternativa descartada**: *consumir `BuildKonfig` directamente en cada punto de uso*. Hoy
solo hay un consumidor, pero acopla todo el código al generador y obliga a tocar cada sitio
si cambia.

## D8 — `BASE_URL_WS` no lo consume nadie

**Decisión**: eliminar `BASE_URL_WS` en lugar de migrarlo a BuildKonfig.

**Justificación**: el rastreo de `UrlConstants` encontró un único consumidor real,
`HttpClientExt.kt`, y solo usa `BASE_URL_HTTP` (líneas 3, 134, 135 y 136). `BASE_URL_WS` no
aparece en ningún otro sitio: es código muerto heredado del esqueleto. Migrarlo significaría
arrastrar una clave de configuración por entorno que nadie lee.

**Riesgo asumido**: si una feature futura necesita WebSocket, habrá que reintroducir la
clave. Es trivial y preferible a mantener configuración especulativa.

**Divergencia deliberada respecto a Squadfy**: allí `BASE_URL_WS` **sí** se conserva, porque
`KtorWebSocketConnector` de `feature/chat` lo consume de verdad. Granatum no tiene chat ni
ningún otro consumidor de WebSocket, así que replicarlo sería copiar la forma sin la
necesidad.

## D9 — Nombre visible e icono

**Decisión**: el nombre visible se define por plataforma en recursos —`strings.xml` en
Android, `CFBundleDisplayName` en iOS— y no por BuildKonfig.

**Justificación**: es texto de interfaz y el principio IX exige que viva en recursos. Además
permite aplicar la mitigación del caso límite —un nombre corto solo en iOS si «Granatum
Suite» se ve recortado— sin tocar Android.

## Resumen de incógnitas resueltas

Ninguna queda abierta. Las tres decisiones de producto se cerraron en la spec antes de
llegar aquí (nombre, idioma e icono), y las nueve decisiones técnicas de arriba están
tomadas y justificadas.
