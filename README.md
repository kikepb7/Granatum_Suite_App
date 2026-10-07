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
- **Autenticación** en `core/data`: login, cambio de contraseña, cierre de sesión y renovación de token. Sin registro, verificación por correo ni recuperación: las cuentas las crea la empresa
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

## Entornos

El entorno se elige **al compilar** y queda fijado en el binario. No se puede cambiar en ejecución, a propósito: así ningún binario de producción puede repuntarse a otro servidor.

```bash
./gradlew :composeApp:assembleDebug                                # local (por defecto)
./gradlew :composeApp:assembleDebug   -Pbuildkonfig.flavor=staging
./gradlew :composeApp:assembleRelease -Pbuildkonfig.flavor=prod
```

| Entorno | Dirección | Cifrado |
|---|---|---|
| `local` | Android `http://10.0.2.2:8080/api` · iOS `http://localhost:8080/api` | no exigido |
| `staging` | de `BASE_URL_HTTP_STAGING` | **obligatorio** |
| `prod` | de `BASE_URL_HTTP_PROD` | **obligatorio** |

Android e iOS resuelven direcciones distintas en `local` porque `10.0.2.2` es un alias que solo entiende el emulador de Android; el simulador de iOS comparte la red del Mac y usa `localhost`. El convention plugin lo resuelve por target, así que no hay que hacer nada.

`staging` y `prod` **no tienen valor por defecto** y el build falla si falta su clave o si la URL no es `https://`. Un binario apuntando al servidor equivocado es peor que un build fallido.

Cada clave se resuelve en este orden: propiedad de Gradle (`-PCLAVE=valor`, cómodo desde CI), luego `local.properties`, luego el valor por defecto. Todas están documentadas en [`local.properties.example`](./local.properties.example).

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
