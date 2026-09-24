# KMM Skeleton

A ready-to-clone **Kotlin Multiplatform Mobile** (Android + iOS) starting point with Compose Multiplatform, a modular Clean Architecture, and the networking/database/DI plumbing already wired up — so a new project starts with a working app on day one instead of a week of boilerplate.

It was extracted from a real production app, then stripped down to a neutral `feature/example` module that shows the full pattern (Ktor + Room offline-first + Koin) without any of that app's business logic.

See **[ARCHITECTURE.md](./ARCHITECTURE.md)** for the deep dive into how everything fits together.

## What's already wired up

- **Compose Multiplatform** UI shared between Android and iOS (`composeApp`)
- **Gradle convention plugins** (`build-logic`) so every module's `build.gradle.kts` is 5–20 lines, not 100
- **Koin** dependency injection, composed per module (`data`/`domain`/`presentation` each expose one Koin module)
- **Ktor** HTTP client with JSON, logging, timeouts, WebSockets, and automatic bearer-token refresh already configured
- **Room** (KMP) for offline-first local persistence, with the Android/iOS `DatabaseFactory` expect/actual already done
- **Ready-made email/password auth plumbing** in `core/data` (login, register, verify email, forgot/reset/change password, session storage, automatic token refresh) — build your own login screens against `AuthRepository`, or delete it if you don't need it
- **A small design system** (`core/designsystem`): theme, typography, buttons, text fields, dialogs, top bar, bottom bar, etc.
- A typed **`Result<D, E>` / `DataError`** model instead of throwing exceptions across layers
- **BuildKonfig** wired to `local.properties` for secrets (never hardcoded, never committed)

## Requirements

- JDK 17+
- Android Studio (latest stable) or IntelliJ IDEA with the Kotlin Multiplatform plugin
- Xcode (latest stable), only needed to build/run the iOS app
- A backend that speaks the routes referenced in `core/data` (`/auth/*`) and `feature/example` (`/example-items`) — point it at your own API, see below

## Getting started

1. **Clone this repo as your new project** (don't fork — you want a clean history):
   ```bash
   git clone https://github.com/<you>/KMM-Skeleton.git my-new-app
   cd my-new-app
   rm -rf .git && git init
   ```
2. **Copy `local.properties.example` to `local.properties`** and fill in `API_KEY` (any non-empty string works while you don't have a real backend yet — the build fails without it, see [ARCHITECTURE.md](./ARCHITECTURE.md#secrets--buildkonfig)).
3. **Point the app at your backend.** Edit `core/data/src/commonMain/kotlin/com/template/core/data/networking/UrlConstants.kt` — it defaults to `http://10.0.2.2:8080/api`, the Android emulator's alias for your machine's `localhost:8080`.
4. **Rename the package/app identity** to your own (see [below](#renaming-the-skeleton)).
5. **Run it:**
   - Android: `./gradlew :composeApp:assembleDebug`, or the run configuration in Android Studio.
   - iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the run configuration in your IDE.

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
  example/
    domain/               Model + repository interface + use cases for this feature.
    database/             Room database, entity, DAO (own Gradle module, per convention).
    data/                 Ktor + Room repository implementations, DTOs, mappers, DI.
    presentation/         ViewModel, screen, navigation graph, DI.

composeApp/              App shell: DI bootstrap, NavHost, Android/iOS entry points.
iosApp/                  Xcode project — the iOS app shell (SwiftUI + Compose bridge).
```

Every feature follows the same four-module shape as `feature/example`. Adding a new feature means copying that folder, renaming `example` → `yourFeature` throughout, and registering the new modules in `settings.gradle.kts` and `composeApp`'s DI (`initKoin.kt`) and nav graph (`NavigationRoot.kt`). Skip the `database` module if the feature has nothing to cache locally.

## Renaming the skeleton

Everything below currently uses `com.granatum.*` / `GranatumSuite` as placeholders. A project-wide find-and-replace covers it:

| Placeholder | Where | Replace with |
|---|---|---|
| `com.granatum.app` | `composeApp` package, Android `applicationId`, iOS bundle identifier | your app ID, e.g. `com.acme.myapp` |
| `com.granatum.core` | `core/*` module packages | `com.acme.myapp.core` (or whatever you prefer) |
| `com.granatum.feature` | `feature/*` module packages | `com.acme.myapp.feature` |
| `com.granatum.buildlogic.convention` | `build-logic` package + plugin IDs (`gradle/libs.versions.toml`, `build-logic/convention/build.gradle.kts`) | `com.acme.buildlogic.convention` |
| `GranatumSuite` | `settings.gradle.kts` (`rootProject.name`), `iosApp` product name/target, `strings.xml` app name | your app's name |

After renaming, move each package's Kotlin source directories to match (e.g. `com/template/core` → `com/acme/myapp/core`) — Kotlin's package declaration must match the folder path.

## Firebase / push notifications

Not included. The `google-services` Gradle plugin, Firebase BOM/dependencies, and the Firebase Swift Package were deliberately removed along with the original project's `GoogleService-Info.plist` (real credentials never belong in a template). If you need push notifications:
1. Re-add `alias(libs.plugins.google.services)` to `composeApp/build.gradle.kts` and the relevant catalog entries to `gradle/libs.versions.toml`.
2. Drop your own `google-services.json` into `composeApp/` and `GoogleService-Info.plist` into `iosApp/iosApp/` — both are gitignored already.
3. Add the Firebase Swift Package back to the Xcode project and wire an `AppDelegate` again (removed here since nothing used it).

## License

Use this however you like as a starting point for your own projects.
