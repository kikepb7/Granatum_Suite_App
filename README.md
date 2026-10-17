# Granatum Suite

Aplicación **Kotlin Multiplatform** (Android + iOS) con UI en Compose Multiplatform para la gestión interna de Granatum: **fichaje** de jornada e **inventario** de materiales.

El fichaje es offline-first: funciona sin conexión y un fichaje nunca se pierde. Esa restricción manda sobre buena parte del diseño de la app.

Las reglas técnicas no negociables están en la **[constitución del proyecto](.specify/memory/constitution.md)**. El recorrido por la arquitectura, en **[ARCHITECTURE.md](./ARCHITECTURE.md)**.

## Qué hay montado

- **Compose Multiplatform** compartido entre Android e iOS (`composeApp`)
- **Convention plugins de Gradle** (`build-logic`): los `build.gradle.kts` de módulo tienen 5-20 líneas, no 100
- **Koin** para inyección, un módulo por cada capa de cada feature
- **Ktor** con JSON, logging, timeouts y renovación automática del token
- **Room** (KMP) para la caché local, con el `DatabaseFactory` expect/actual ya resuelto
- **Inicio de sesión real** contra el backend, con navegación por rol (`ADMIN`, `ENCARGADO`, `EMPLEADO`, `REPRESENTANTE`), cambio de contraseña obligatorio y voluntario, y una sesión que no se pierde por quedarse sin cobertura ([spec 004](specs/004-login-roles/spec.md)). Sin registro ni recuperación de contraseña: las cuentas las crea y restablece la administración
- **Design system** propio (`core/designsystem`)
- **`Result<D, E>` / `DataError`** en vez de excepciones entre capas
- **BuildKonfig** para la configuración por entorno, leída de `local.properties` y nunca commiteada

## Requisitos

- JDK 17+
- Android Studio (estable más reciente) o IntelliJ IDEA con el plugin de Kotlin Multiplatform
- Xcode (estable más reciente), solo para compilar y ejecutar en iOS
- El backend de Granatum ([Granatum_Suite_Backend](https://github.com/kikepb7/Granatum_Suite_Backend)) corriendo en local

## Puesta en marcha

1. **Clona el repositorio y copia la configuración local:**
   ```bash
   cp local.properties.example local.properties
   ```
   Rellena `API_KEY` con cualquier cadena no vacía mientras no haya backend real: el build falla si falta.

2. **Arranca el backend** en local. Por defecto escucha en el puerto 8080 y expone `/actuator/health`.

3. **Ejecuta la app:**
   - Android: `./gradlew :composeApp:assembleDebug`, o la configuración de ejecución de Android Studio.
   - iOS: abre `iosApp/iosApp.xcodeproj` en Xcode y ejecuta.

No hace falta configurar ninguna URL: el entorno `local` ya apunta a la máquina anfitriona en ambas plataformas.

## Versión de demostración (sin servidor)

La demo vive ahora en la **app PRE**: elige «Demo» en el selector de entorno. Ver
[Entornos: app PRE y app PRO](#entornos-app-pre-y-app-pro).

## Iniciar sesión en local

La app ya no entra sin credenciales: necesita una cuenta real del backend. Solo el propietario se
registra; al resto del personal lo da de alta él (backend `2ec33d0`, feature 009):

1. Arranca el backend con `AUTH_CODIGO_ARRANQUE` definido (24 caracteres o más) y, en la app, toca
   «¿Eres el propietario…? Crea la cuenta» e introduce ese código. Entras como `ADMIN`.
2. En **Equipo → Dar de alta**, crea a cada persona con su ficha, su correo y su rol. La app
   muestra una vez la contraseña provisional: al entrar con ella, la persona tiene que cambiarla.
3. Si alguien pierde la contraseña, ábrelo en **Equipo** y toca «Restablecer contraseña».

Si el backend no está en el 8080, la URL se cambia al compilar, sin tocar ningún fichero:

```bash
./gradlew :composeApp:installDebug -PBASE_URL_HTTP=http://10.0.2.2:8090/api
```

En iOS, desde Xcode, se pasa por entorno, porque la fase de build de Xcode llama a Gradle:
`ORG_GRADLE_PROJECT_BASE_URL_HTTP_IOS=http://localhost:8090/api`.

Para probar la renovación de la sesión sin esperar 15 minutos, arranca el backend con
`JWT_EXPIRATION_MINUTES=1`.

## Personal

Solo para la administración, en la pestaña **Equipo** ([spec 009](specs/009-personal/spec.md)):
lista del personal con búsqueda, alta de una persona en un paso (ficha y acceso), edición de la
ficha (el DNI no cambia), bajas y reactivaciones, y restablecer la contraseña o dar acceso a una
ficha que no lo tiene. La contraseña provisional solo se ve una vez y no se guarda en el móvil;
las rutas de personal no salen en el log de red. Desactivar no echa al instante: la sesión abierta
caduca en unos minutos (regla del backend). Desde el personal se llega a la jornada del equipo.

## Fichaje

Cada fichaje (entrada, pausa con su tipo, fin de pausa, salida) se guarda primero en el móvil y
se registra después en el servidor como parte de una **jornada** (`/api/fichajes`). Funciona sin
cobertura: lo pendiente se envía solo al recuperar la conexión, en el orden en que ocurrió y con
la hora a la que se pulsó, sin duplicarse aunque se reintente
([spec 005](specs/005-fichaje-real/spec.md)).

- Lo que el servidor rechaza para siempre se muestra con su motivo y deja de reintentarse. El
  servidor no acepta fichajes con más de 72 horas ni con el reloj del móvil adelantado más de
  5 minutos.
- Una jornada cerrada con un error se arregla pidiendo una corrección desde su detalle en el
  historial; la aprueba una encargada o la administración.
- En Android, WorkManager envía lo pendiente aunque la app esté cerrada. En iOS se envía al abrir
  la app o al recuperar la conexión.

Para probar el rechazo por antigüedad sin esperar tres días, arranca el backend local con
`--timetracking.reloj.tolerancia-pasado=PT2M`.

## Inventario

Solo para la administración y las encargadas
([spec 011](specs/011-inventario-productos/spec.md)). Cada **producto** tiene categoría (que hace
de nombre), descripción, dimensiones en centímetros (alto × ancho), cantidad, imagen, localización y
fechas de alta y de modificación. La **localización** es un almacén (A, B o C) y una sección (1 a 5),
y se elige tocando un plano de los tres almacenes.

- **Buscar y filtrar**: búsqueda por categoría, descripción o ubicación (sin distinguir tildes), un
  selector de almacén siempre visible y un panel con orden, stock, localización y categoría. El botón
  del panel dice cuántos productos quedarán antes de aplicar, y los filtros activos se ven como
  pastillas que se quitan con un toque.
- **Cantidad con historial**: en el detalle, con − / + y atajos ±5 / ±10; nada se envía hasta
  guardar, con un motivo opcional que queda en el historial.
- **Imágenes**: se hacen con la cámara o se eligen de la galería y se envían al servidor al guardar
  el producto (`PUT /api/materiales/{id}/imagen`). Las guarda él en su storage y devuelve una URL
  firmada temporal, que la app descarga sin enviar el token de sesión. Si la imagen falla, el
  producto ya queda guardado y volver a guardar solo reintenta la imagen.
- **Consultar funciona sin cobertura**; **modificar exige conexión** (el servidor no ofrece
  idempotencia y reintentar podría duplicar un alta).
- Sigue el contrato de materiales de la feature 010 del backend
  ([research](specs/011-inventario-productos/research.md)).
- Una categoría con productos no se puede borrar (el servidor respondería con un error 500).

## Eventos

Para todos los roles ([spec 012](specs/012-eventos/spec.md), backend feature 011). Cada evento tiene
nombre (se propone «Evento N»), descripción y fecha, y lleva material del inventario.

- **Reservar es retirar del inventario.** Añadir material a un evento descuenta sus unidades en el
  momento; bajar la cantidad, quitarlo, borrar el evento o finalizarlo las devuelve. El siguiente
  evento ve siempre el stock actualizado.
- **Sin stock no se reserva nada**: el selector limita la cantidad a las unidades libres y, si otra
  persona se las ha llevado mientras tanto, la app dice cuántas quedan.
- **Finalización**: el servidor finaliza los eventos al acabar su día; también hay «Finalizar
  ahora». Un evento finalizado se consulta pero no se modifica.
- `ADMIN`, `ENCARGADO` y `EMPLEADO` crean y editan; `REPRESENTANTE` solo consulta.
- Consultar funciona sin cobertura (caché local); cambiar algo exige conexión.
- En inventario, un material reservado para un evento programado no se puede borrar.

## Facturación

Solo para la administración: registra las facturas emitidas y recibidas del negocio contra
`/api/facturacion` ([spec 008](specs/008-facturacion/spec.md)). No emite facturas.

- **Subir**: foto con la cámara, imágenes de la galería o PDF, varios a la vez. Las fotos HEIC del
  iPhone se convierten a JPEG y las imágenes de más de 10 MB se reducen antes de enviarlas.
- **Revisar y confirmar**: el servidor lee el documento en segundo plano (si tiene
  `ANTHROPIC_API_KEY`; sin ella, las facturas se rellenan a mano) y deja un borrador con avisos.
  Los que impiden confirmar salen en rojo junto a su campo; la app ayuda a cuadrar el total. Todo
  cambio viaja con su versión: si otra persona tocó la factura, la app recarga en lugar de pisarla.
- **Trimestres y reportes**: cerrar un trimestre bloquea sus facturas hasta reabrirlo con un motivo.
  Los reportes mensuales, trimestrales y anuales se descargan en PDF o CSV.
- **Solo con conexión**: son datos fiscales de terceros y no se guardan en el móvil. Tampoco salen
  en el log de red.
- La cuenta se abre ahora desde el botón con la inicial de la barra superior, para que la barra
  inferior no pase de cinco pestañas.

## Entornos: app PRE y app PRO

Hay **dos apps**, elegidas al compilar ([spec 010](specs/010-entornos/spec.md)):

| | **PRE** (por defecto) | **PRO** (la de las tiendas) |
|---|---|---|
| Compilar | `-Pgranatum.build=pre` | `-Pgranatum.build=pro` |
| Android | `com.granatum.app.pre` · «Granatum PRE» | `com.granatum.app` · «Granatum Suite» |
| iOS | `GRANATUM_BUILD = pre` en `Config.xcconfig` | `xcodebuild … GRANATUM_BUILD=pro` |
| Entornos | Local, PRE, PRO y **Demo**, con selector | Solo producción |
| Selector | En el login y en la cuenta, con franja de entorno durante la sesión | No existe |
| Usuarios demo | Módulo `:core:demo` (entorno Demo) | No se enlaza |
| Log de red | Completo (salvo rutas sensibles) | Ninguno |
| Tráfico sin cifrar | Solo hacia el emulador (entorno Local) | Prohibido |

Las dos se instalan a la vez en el mismo móvil.

```bash
./gradlew :composeApp:installDebug                                  # PRE
./gradlew :composeApp:assembleRelease -Pgranatum.build=pro          # PRO (exige BASE_URL_HTTP_PRO)
```

Para que el botón Run de Android Studio compile otra cosa, pon `granatum.build=pro` en
`local.properties`. En iOS, Xcode pasa `GRANATUM_BUILD` a Gradle.

**Direcciones** (propiedad de Gradle `-PCLAVE=valor`, luego `local.properties`):

| Entorno | Clave | Por defecto | Cifrado |
|---|---|---|---|
| Local | `BASE_URL_HTTP` (`BASE_URL_HTTP_IOS` para iOS) | Android `http://10.0.2.2:8080/api` · iOS `http://localhost:8080/api` | no exigido |
| PRE | `BASE_URL_HTTP_PRE` (antes `_STAGING`) | sin valor: la opción no aparece | **obligatorio** |
| PRO | `BASE_URL_HTTP_PRO` (antes `_PROD`) | sin valor: la app PRO no compila | **obligatorio** |

**Cambiar de entorno en PRE** cierra la sesión (revocándola en el entorno anterior), borra los
datos guardados de fichaje e inventario y recuerda la elección. Si hay fichajes sin enviar, no deja
cambiar hasta que se envíen: nunca acaban en el servidor equivocado.

**Demo**: sin servidor, con datos de ejemplo de una floristería que se borran al cerrar la app.
Al elegirlo entra como administración (Lucía Ferrer); al cerrar sesión, el login ofrece entrar como
empleado (Marcos Vidal) o volver como administración con un toque. La CI comprueba que la app PRO
no contiene el módulo demo.

## Contrato de API

La app consume la API del [backend de Granatum](https://github.com/kikepb7/Granatum_Suite_Backend)
según una copia de su contrato OpenAPI en [`docs/openapi.json`](docs/openapi.json), fijada al
commit que indica [`docs/openapi.pin`](docs/openapi.pin). Para actualizarla:

```bash
scripts/sync-openapi.sh <commit-del-backend>
```

Qué cubre esa copia, qué hay que leer en la documentación del backend (códigos de error,
contenido del token) y por qué el pin es un commit y no una rama: [`docs/api-contract.md`](docs/api-contract.md).

## Integración continua

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) se ejecuta en cada PR contra `main` y en cada push a `main`.

| Job | Qué hace | Bloquea |
|---|---|---|
| `static-analysis` | ktlint en todos los módulos | **no** — informativo |
| `unit-tests` | tests unitarios, cobertura y trinquete | sí |
| `build-android` | `assembleDebug`; el APK queda descargable | sí |
| `build-ios` | enlaza el framework de `composeApp` y ejecuta los tests en el simulador de iOS | sí |
| `summary` | veredicto único: falla si algún job bloqueante falla o se cancela | sí |

No necesita secretos, así que también comprueba con seguridad las PRs que llegan desde forks.

**ktlint no bloquea** porque el código es anterior a la herramienta: al activarlo encontró unas 1.500 violaciones. Pasará a bloquear cuando el código esté saneado, con una enmienda de la constitución.

**La cobertura funciona como trinquete** (principio VIII). El mínimo vive en una línea de `gradle.properties`:

```properties
granatum.coverage.minLine=0
```

Arrancó en 0 % porque esa era la cobertura real al activar la CI. **Súbelo en la misma PR que añada tests**; bajarlo exige enmendar la constitución.

### Para que la CI bloquee de verdad

Sin este paso la CI informa, pero no impide fusionar. Lo configura quien administra el repositorio:

> Settings → Branches → Branch protection rules → `main` → *Require status checks to pass before merging* → marcar **`summary`**.

### Reproducirla en local

Los mismos comandos que ejecuta la CI:

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest :koverXmlReport :koverVerify
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test
```

Un cambio no está terminado hasta que compilan los dos targets: verificar solo Android no es verificar.

## Spec-driven development (Spec Kit)

The shared process lives in `.specify/` and **is** committed: the project constitution (`.specify/memory/constitution.md`), the spec/plan/tasks templates and the workflow scripts. Feature specs generated under `specs/` are committed too — they're the artefact worth reviewing.

The `/speckit-*` agent commands are **not** committed. They install into `.claude/skills/`, which is gitignored. Recreate them after cloning:

```bash
brew install uv
uv tool install specify-cli --from git+https://github.com/github/spec-kit.git
specify init . --integration claude --ignore-agent-tools --force
```

`--ignore-agent-tools` is mandatory on macOS. Spec Kit probes the `PATH` for a `claude` executable; the Claude Code desktop app ships only a Linux binary inside its bundle, so the tool check fails and `init` aborts without the flag.

Workflow: `/speckit-constitution` → `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement`.

## Project structure

```
build-logic/            Gradle convention plugins — the reusable "engine"
  convention/            android-application, cmp-application, kmp-library,
                          cmp-library, cmp-feature, room, buildkonfig plugins

core/
  domain/                Pure Kotlin: models, repository interfaces, Result/DataError,
                          AuthRepository/SessionStorage contracts. No framework deps.
  data/                   Ktor client + auth plumbing + DataStore session storage.
                          Implements core/domain's repository interfaces.
  designsystem/           Theme, typography, and reusable Compose components.
  presentation/           UiText, error-to-UiText mapping, permissions, media picker,
                          shared ViewModel/Compose utilities.

feature/
  clockin/               Fichaje — offline-first, the app's reason to exist.
    domain/               Model + repository interface + use cases for this feature.
    database/             Room database, entity, DAO (own Gradle module, per convention).
    data/                 Ktor + Room repository implementations, DTOs, mappers, DI.
    presentation/         ViewModel, screen, navigation graph, DI.
  inventory/             Materials — same four-module shape.

composeApp/              App shell: DI bootstrap, NavHost, Android/iOS entry points.
iosApp/                  Xcode project — the iOS app shell (SwiftUI + Compose bridge).
```

Toda feature sigue la misma forma de cuatro módulos. Añadir una significa replicar esa estructura y registrar los módulos en `settings.gradle.kts`, en la DI de `composeApp` (`initKoin.kt`) y en el grafo de navegación (`NavigationRoot.kt`). El módulo `database` se omite si la feature no cachea nada en local.

## Firebase / notificaciones push

No están incluidas. El plugin de `google-services`, las dependencias de Firebase y el Swift Package se retiraron junto con el `GoogleService-Info.plist` original: las credenciales reales no van en el repositorio. Si hacen falta:

1. Añade `alias(libs.plugins.google.services)` a `composeApp/build.gradle.kts` y las entradas correspondientes a `gradle/libs.versions.toml`.
2. Pon tu `google-services.json` en `composeApp/` y el `GoogleService-Info.plist` en `iosApp/iosApp/`. Ambos ya están gitignorados.
3. Vuelve a añadir el Swift Package de Firebase al proyecto de Xcode y cablea un `AppDelegate`.
