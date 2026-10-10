# Architecture

This document explains how the pieces fit together: the module layering, the Gradle convention plugins that keep every `build.gradle.kts` tiny, dependency injection, networking, persistence, and the design system. Read [README.md](./README.md) first if you just want to get the app running.

## Module layering

```
composeApp  ──depends on──▶  feature/*/presentation ──▶ feature/*/domain ◀── feature/*/data ──▶ feature/*/database
     │                                    │                      ▲                  │
     │                                    ▼                      │                  ▼
     └──────────────────────────▶  core/presentation      core/domain ◀──────── core/data
                                          │                                          │
                                          ▼                                          ▼
                                   core/designsystem                          (Ktor, Room, DataStore)
```

Rules of thumb, enforced by which module can even see which (Gradle module dependencies, not just convention):

- **`domain` modules depend on nothing but Kotlin, coroutines, and `core/domain`.** No Koin, no Ktor, no Android, no Compose. This is the layer that survives everything else changing.
- **`data` modules implement `domain` interfaces**, talking to Ktor (remote) and/or Room (local). They depend on `core/data` for the shared `HttpClient`, `Result`/`DataError` helpers, and networking extensions.
- **`presentation` modules depend on `domain` (never on `data` directly)**, plus `core/designsystem` and `core/presentation`. ViewModels call use cases, not repositories.
- **`database` modules are pure Room** — entities, DAOs, the `RoomDatabase` subclass, and the `DatabaseFactory` expect/actual. Nothing else. This keeps Room's generated code isolated and lets `data` be the only module that touches both the network and the database.
- **`composeApp` is the only module that knows about every feature.** It aggregates Koin modules and wires the nav graph. Individual features never depend on each other or on `composeApp`.

## Gradle convention plugins (`build-logic`)

Every module's `build.gradle.kts` applies one convention plugin instead of hand-rolling Kotlin Multiplatform target lists, Android config, and dependency boilerplate. They live in `build-logic/convention/src/main/kotlin` and are registered as plugin IDs (`com.granatum.buildlogic.convention.*`) in `gradle/libs.versions.toml`, resolved via `includeBuild("build-logic")` in `settings.gradle.kts`.

| Plugin | Applies to | What it configures |
|---|---|---|
| `convention.kmp.library` | Pure Kotlin modules (`domain`, `database`, non-UI `data`) | Android + iOS targets, `kotlinx-serialization`, Java 17, `-Xexpect-actual-classes` opt-in, `namespace`/`resourcePrefix` derived from the Gradle path |
| `convention.cmp.library` | UI-bearing library modules (`designsystem`, `core/presentation`) | Everything `kmp.library` does, plus Compose Multiplatform (runtime/foundation/material3) |
| `convention.cmp.feature` | Feature `presentation` modules | Everything `cmp.library` does, plus Koin (compose + viewmodel), navigation, lifecycle — and a dependency on `core:presentation`/`core:designsystem` |
| `convention.cmp.application` | `composeApp` | Android application + Compose + iOS static-framework targets (the app entry point) |
| `convention.room` | Any module that needs Room | KSP + Room Gradle plugin, `commonMainApi` on `room-runtime`/`sqlite-bundled`, KSP compiler registered for Android and all three iOS targets |
| `convention.buildkonfig` | Modules that need build-time secrets | BuildKonfig, reading values out of `local.properties` (see below) |
| `convention.android.application` / `.compose` | Internal — used by `cmp.application` | Base Android application config (applicationId, SDK versions, packaging) |

A module's package name, iOS framework name, and Android resource prefix are all **derived automatically from its Gradle path** (`build-logic/convention/src/main/kotlin/com/template/convention/PathUtil.kt`) — `:feature:example:domain` becomes package `com.granatum.feature.example.domain`, framework name `FeatureExampleDomain`, resource prefix `feature_example_domain_`. Follow the existing folder layout and you never have to think about this.

## Dependency injection (Koin)

Every `data`, `domain`, and `presentation` module exposes exactly one Koin module (`val xModule = module { ... }`), typically in a `di` package:

```kotlin
// feature/example/domain/.../di/ExampleDomainModule.kt
val exampleDomainModule = module {
    singleOf(::GetExampleItemsUseCase)
    singleOf(::RefreshExampleItemsUseCase)
    singleOf(::CreateExampleItemUseCase)
}
```

When a module needs a platform-specific dependency (an HTTP engine, a database driver, a DataStore instance), it declares `expect val platformXModule: Module` in `commonMain` and provides `actual` implementations per platform, then `includes()` it:

```kotlin
// feature/example/data/.../di/ExampleDataModule.kt
expect val platformExampleDataModule: Module

val exampleDataModule = module {
    includes(platformExampleDataModule)
    single { get<DatabaseFactory>().create().setDriver(BundledSQLiteDriver()).build() }
    singleOf(::KtorExampleRepositoryImpl) bind ExampleService::class
    singleOf(::OfflineFirstExampleRepositoryImpl) bind ExampleRepository::class
}
```

`composeApp/src/commonMain/kotlin/.../di/initKoin.kt` is the single place that imports every feature's modules and calls `startKoin`. Add your new feature's three modules there. Bootstrapped from `AppApplication.onCreate()` on Android and from `iOSApp.init()` (via the Kotlin-exported `InitKoinKt.doInitKoin()`) on iOS.

## Networking

`core/data/.../networking/HttpClientFactory.kt` builds one shared Ktor `HttpClient`, injected via Koin, with:

- JSON content negotiation (`ignoreUnknownKeys = true`)
- 20s request/socket timeouts
- Logging bridged to the app's own `AppLogger` interface (backed by Kermit)
- WebSockets (20s ping interval)
- A default `x-api-key` header sourced from `BuildKonfig.API_KEY`
- **Automatic bearer-token refresh**: `Auth { bearer { ... } }` loads tokens from `SessionStorage`, and on 401 calls `/auth/refresh`, persists the new tokens, and retries — without every repository having to think about it. Requests under `auth/` are excluded to avoid a refresh loop.

The engine itself is platform-specific (OkHttp on Android, Darwin on iOS), swapped in via the same `expect/actual` Koin module pattern described above — `HttpClientFactory` itself is pure common code.

### Result / DataError instead of exceptions

`core/domain/util/Result.kt` defines a sealed `Result<D, E>` (`Success`/`Failure`) with `map`/`onSuccess`/`onFailure`/`asEmptyResult()` extensions, and `DataError` (`core/domain/util/DataError.kt`) enumerates `Remote`/`Local`/`ConnectionModel` failure reasons. `core/data/.../networking/HttpClientExt.kt` wraps every Ktor call (`get`/`post`/`put`/`delete`/`postMultipart`) in `safeCall { }`, which maps HTTP status codes and low-level exceptions (`UnknownHostException` on Android, `NSURLError*` on iOS, via `platformSafeCall` expect/actual) straight to `DataError.Remote` values. Nothing in `data` or `presentation` throws for expected failure paths.

`core/presentation/.../mapper/DataErrorToUiText.kt` turns a `DataError` into a `UiText` for display; `feature/example/presentation/.../mapper/ExampleErrorMappers.kt` shows the pattern for a feature's own domain-level error type (`CreateExampleItemError`).

## Persistence (Room)

Each feature that needs local storage gets its own `database` module — a separate Gradle module so Room's generated code and KSP processing stay isolated from `data`. The pattern (see `feature/example/database`):

```kotlin
@Database(entities = [ExampleItemEntity::class], version = 1)
@ConstructedBy(AppExampleDatabaseConstructor::class)
abstract class AppExampleDatabase : RoomDatabase() {
    abstract val exampleItemDao: ExampleItemDao
}

expect object AppExampleDatabaseConstructor : RoomDatabaseConstructor<AppExampleDatabase> {
    override fun initialize(): AppExampleDatabase
}

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<AppExampleDatabase>
}
```

`DatabaseFactory` is implemented per platform (`context.getDatabasePath(...)` on Android, `NSDocumentDirectory` on iOS) and instantiated in the **`data`** module's Koin module, not the database module itself — that's where `.setDriver(BundledSQLiteDriver()).build()` happens.

`feature/example/data/.../datasource/local/OfflineFirstExampleRepositoryImpl.kt` shows the offline-first shape: reads come from a `Flow` over the DAO, `refreshItems()`/writes hit the network and sync the result into Room on success. `feature/example/data/.../datasource/remote/KtorExampleRepositoryImpl.kt` shows the same feature's remote-only counterpart (`ExampleService`), useful when you need direct network access without going through the cache (e.g. one-off actions).

## Design system (`core/designsystem`)

A small, renameable component library: `theme/` (colors, typography, the `AppTheme` composable and the `AppTheme.colors/spacing/shapes/elevation` tokens in `AppTokens.kt`, with light and dark variants; dark mode follows the system through `isSystemInDarkTheme()` in `AppTheme`), and `components/` grouped by kind — `buttons` (`AppButton`, gradient `AppPrimaryButton`), `cards` (`AppCard`, `AppStatCard`), `chips` (`AppStatusChip`, `AppBadge`, `AppFilterChip`, `AppTone`), `lists` (`AppListItem`, `AppSectionHeader`), `feedback` (`AppEmptyState`, `AppErrorState`, `AppLoadingState`, `AppBanner`), `inputs` (`AppSearchField`, `AppSegmentedControl`), `textfields`, `dialogs`, `dropdown`, `layouts` (including `AppSurface`, an adaptive result/success layout, a snackbar scaffold), `navigation` (`AppBottomBar`, the floating pill), `topbar` (large-title `AppTopBar`), `avatar`, `brand`, `divider`, `icons` (`AppTabIcons`). Screens use `AppTheme.colors` (semantic tokens) and never hardcode a colour. Everything is prefixed `App*` — rename the prefix to match your brand if you like, it's a plain find-and-replace.

## `core/presentation`

Shared, framework-light Compose/ViewModel utilities that don't belong to any one feature:

- `util/UiText.kt` — a `DynamicString | Resource` sealed type so ViewModels never hold raw Android string resources
- `util/ObserveAsEvents.kt` — lifecycle-safe one-shot event collection from a ViewModel's event `Flow`
- `util/ScopedStoreRegistryViewModel.kt` — scopes nested `ViewModelStore`s (e.g. for dialogs/bottom sheets)
- `mediapicker/` and `permissions/` — cross-platform image picking and runtime permissions (Moko Permissions)

It also defines an extra **`mobileMain`** source set (in its `build.gradle.kts`) that sits between `commonMain` and `androidMain`/`iosMain` — for code that's mobile-only but not necessarily relevant to a future desktop/web target reachable from `commonMain`. Follow the same pattern if you add more shared-but-not-fully-common code later.

## Secrets & BuildKonfig

`convention.buildkonfig` reads values straight out of the gitignored `local.properties` at configuration time and fails the build if they're missing (see `BuildKonfigConventionPlugin.kt`) — no secret is ever hardcoded or committed. Currently only `API_KEY` is wired up (exposed as `BuildKonfig.API_KEY`); add more the same way if you need per-environment config (base URLs, feature flags, etc.).

## Adding a new feature

1. Copy `feature/example` to `feature/yourFeature`, keeping the same four-module shape (drop `database` if you don't need local persistence).
2. Rename packages from `com.granatum.feature.example.*` to `com.granatum.feature.yourFeature.*`, and the Kotlin folders to match.
3. Register the new modules in `settings.gradle.kts`.
4. Add the three Koin modules (`data`/`domain`/`presentation`) to `composeApp/.../di/initKoin.kt`.
5. Add the nav graph extension to `composeApp/.../navigation/NavigationRoot.kt`.
