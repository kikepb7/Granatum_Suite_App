# Tasks: Inicio de sesión, sesión real y navegación por roles

**Input**: Design documents from `/specs/004-login-roles/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/auth-api.md, quickstart.md

**Tests**: SÍ. research D11 los exige: son los primeros tests de código de producción del
proyecto. Cada test se escribe **antes** de su implementación y debe fallar primero.

> **Los tests no sustituyen al dispositivo.** Igual que en la spec 002, un login que no
> deserializa contra el servidor real o una renovación que expulsa sin red son compatibles con
> tests en verde contra un `MockEngine` mal configurado. La fase final recorre el quickstart
> contra el backend real.

Rutas abreviadas:

- `domain/` = `core/domain/src/commonMain/kotlin/com/granatum/core/domain/`
- `data/` = `core/data/src/commonMain/kotlin/com/granatum/core/data/`
- `authui/` = `feature/auth/presentation/src/commonMain/kotlin/com/granatum/feature/auth/presentation/`
- `domainTest/` y `dataTest/` = los `commonTest` equivalentes

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

---

## Phase 1: Setup

**Purpose**: catálogo, módulo nuevo y dependencias de test.

- [X] T001 Añadir `ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }` a `gradle/libs.versions.toml` (research D11). Ninguna versión nueva: reutiliza `ktor = "3.2.3"`
- [X] T002 Añadir a `core/domain/build.gradle.kts` y `core/data/build.gradle.kts` las dependencias de `commonTest`: `libs.kotlin.test` y `libs.kotlinx.coroutines.test` (si no está en el catálogo, añadirla con `version.ref = "kotlinx-coroutines"`); en `core/data` además `libs.ktor.client.mock`. Solo bloques `dependencies`, sin configuración de build (principio XI)
- [X] T003 Crear el módulo `feature/auth/presentation` con `build.gradle.kts` copiado del patrón de `feature/clockin/presentation/build.gradle.kts` (plugin `convention.cmp.feature`; dependencias `core.domain`, `core.designsystem`, `core.presentation`, `koin.common`, `compose.components.resources`) y registrarlo en `settings.gradle.kts` con `include(":feature:auth:presentation")`
- [X] T004 Añadir `implementation(projects.feature.auth.presentation)` a `composeApp/build.gradle.kts` (bloque `dependencies` de `commonMain`) y comprobar `./gradlew :feature:auth:presentation:compileKotlinIosSimulatorArm64 :composeApp:assembleDebug`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: dominio y datos hablan el idioma del servidor. Bloquea todas las historias.

### Tests del dominio (escribir primero) ⚠️

- [X] T005 [P] Test de `PasswordValidator` en `domainTest/validation/PasswordValidatorTest.kt`: cada requisito por separado (`LONGITUD_MINIMA` con 7 caracteres y no con 8; `LONGITUD_MAXIMA` con 129 y no con 128; `FALTA_MAYUSCULA`, `FALTA_MINUSCULA`, `FALTA_DIGITO`, `FALTA_SIMBOLO`); el espacio en blanco no cuenta como símbolo; `"Granatum1!"` es válida (conjunto vacío)
- [X] T006 [P] Test de la máquina de estados en `domainTest/auth/SessionStateTest.kt`, con todas las transiciones de la tabla de `data-model.md`: sin sesión → `SignedOut(null)`; `mustChangePassword` → `PasswordChangeRequired`; sesión válida → `Active(role)`; cierre con motivo → `SignedOut(reason)`
- [X] T007 [P] Test de permisos de `UserRole` en `domainTest/auth/UserRoleTest.kt`, con la tabla exacta de `data-model.md`: `ADMIN`/`ENCARGADO` los tres permisos; `EMPLEADO` solo `canClockIn`; `REPRESENTANTE` y `DESCONOCIDO` ninguno

### Dominio

- [X] T008 [P] Crear `domain/auth/model/Session.kt` con `accessToken`, `refreshToken`, `employeeId`, `role: UserRole`, `mustChangePassword`, `email` (data-model). Borrar `domain/auth/model/AuthInfoModel.kt` y `domain/auth/model/UserModel.kt`
- [X] T009 [P] Reescribir `domain/auth/model/UserRole.kt`: `ADMIN, ENCARGADO, EMPLEADO, REPRESENTANTE, DESCONOCIDO`, con `canClockIn`, `canManageInventory` y `canSeeTeam` según `data-model.md`. Eliminar `canManageTeam`
- [X] T010 [P] Crear `domain/auth/model/SignOutReason.kt` (`INACTIVE`, `SESSION_REJECTED`) y `domain/auth/model/SessionState.kt` (`Loading`, `SignedOut(reason: SignOutReason?)`, `PasswordChangeRequired`, `Active(role: UserRole)`), con una función pura `sessionStateOf(session: Session?, loaded: Boolean, reason: SignOutReason?)` que haga pasar T006
- [X] T011 [P] Crear `domain/auth/AuthError.kt` como `sealed interface AuthError : Error` con los casos de `data-model.md`: `InvalidCredentials`, `TooManyAttempts(retryAfterSeconds: Long?)`, `ServiceBusy(retryAfterSeconds: Long?)`, `WeakPassword(requirements: Set<PasswordRequirement>)`, `Validation`, `NoInternet`, `Timeout`, `InvalidSession`, `Unknown`
- [X] T012 [P] Crear `domain/validation/PasswordRequirement.kt` con `LONGITUD_MINIMA, LONGITUD_MAXIMA, FALTA_MAYUSCULA, FALTA_MINUSCULA, FALTA_DIGITO, FALTA_SIMBOLO`, y reescribir `domain/validation/PasswordValidator.kt` para que `validate(password): Set<PasswordRequirement>` aplique la tabla de `data-model.md` (mínimo 8, máximo 128). Borrar `PasswordValidationState.kt` si queda sin uso. Hace pasar T005
- [X] T013 Cambiar `domain/auth/repository/SessionStorage.kt` a `fun observeSession(): Flow<Session?>` y `suspend fun set(session: Session?)`, y `domain/auth/repository/AuthRepository.kt` a `login(email, password): Result<Session, AuthError>`, `changePassword(current, new): Result<Session, AuthError>` y `logout(): EmptyResult<Nothing>`; esta última borra siempre la sesión local (FR-026)
  - *Hecho con una diferencia: `logout()` devuelve `Unit`, no `EmptyResult`. No tiene ningún fallo que comunicar, porque borra la sesión local siempre.*

### Tests de datos (escribir primero) ⚠️

- [X] T014 [P] Test de `AccessTokenClaims` en `dataTest/auth/token/AccessTokenClaimsTest.kt`, con tokens construidos en el test (header, payload en base64url y una firma cualquiera): claims válidos; payload sin padding; `role` desconocido → `DESCONOCIDO`; sin `sub` → fallo; payload que no es JSON → fallo; menos de tres segmentos → fallo; `pwd_change: true` leído
- [X] T015 [P] Test de `AuthErrorMapper` en `dataTest/auth/AuthErrorMapperTest.kt`: cada fila de la tabla de `contracts/auth-api.md`; `Retry-After: 30` → 30; `Retry-After` ausente o no numérico → `null`; `422 PASSWORD_DEBIL` con `requisitos: ["FALTA_SIMBOLO", "DESCONOCIDO_X"]` → `{FALTA_SIMBOLO}`; cuerpo que no es JSON → `Unknown`
- [X] T016 [P] Test de `SecureSessionStorage` en `dataTest/auth/storage/SecureSessionStorageTest.kt` con un `SecureStore` falso en memoria: guarda y lee `v2`; una entrada `v2` ilegible se borra y da `null` (FR-024); `load()` borra la clave `granatum.session.secure.v1`
- [X] T017 [P] Test de `KtorAuthRepositoryImpl` en `dataTest/auth/KtorAuthRepositoryImplTest.kt` con `MockEngine`: el login envía `{email recortado, password tal cual}` a `/auth/login`; con `200` guarda una `Session` con el `email` en minúsculas y el `employeeId` del `sub`; con `401 CREDENCIALES_INVALIDAS` devuelve `InvalidCredentials` y no guarda nada; `change-password` envía `passwordActual`/`passwordNueva`; `logout` sin red borra igualmente la sesión local

### Datos

- [X] T018 [P] Reescribir los DTOs en `data/auth/dto/`: `ParTokensResponseDto(accessToken, refreshToken, expiresIn: Long, requiereCambioPassword: Boolean)`, `LoginRequestDto(email, password)`, `RefreshRequestDto(refreshToken)`, `LogoutRequestDto(refreshToken)`, `CambioPasswordRequestDto(passwordActual, passwordNueva)`, `ErrorBodyDto(code, message, requisitos: List<String>? = null)` y `StoredSessionDto` para la forma `v2`. Borrar `AuthInfoSerializableDTO`, `UserSerializableDTO`, `ChangePasswordRequestDTO`, `LoginRequestDTO` y `RefreshRequestDTO`. Los nombres de campo coinciden letra por letra con `docs/openapi.json`
- [X] T019 [P] Crear `data/auth/token/AccessTokenClaims.kt`: decodifica el segundo segmento con `Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)` y `kotlinx.serialization`, sin verificar firma (research D1), y devuelve `employeeId`, `role` y `pwdChange` o un fallo. Hace pasar T014
- [X] T020 [P] Crear `data/auth/AuthErrorMapper.kt`: `suspend fun HttpResponse.toAuthError(): AuthError`, que lee `ErrorBodyDto` y la cabecera `Retry-After` según `contracts/auth-api.md`, y `fun Throwable.toAuthError()` para sin red y timeout. Hace pasar T015
- [X] T021 Crear `data/mappers/SessionMapper.kt` (sustituye a `AuthInfoMapper.kt`, que se borra): `ParTokensResponseDto + email → Result<Session, AuthError>` aplicando D1 (`mustChangePassword` = `requiereCambioPassword || pwdChange`; sin `sub` → `InvalidSession`), y `Session ↔ StoredSessionDto`
- [X] T022 Adaptar `data/auth/storage/SecureSessionStorage.kt` y `data/auth/storage/SecureStore.kt`: clave `SECURE_SESSION_KEY = "granatum.session.secure.v2"`, constante `PREVIOUS_SESSION_KEY = "granatum.session.secure.v1"` que `load()` borra siempre, `observeSession()` y `set(Session?)`. Hace pasar T016
- [X] T023 Reescribir `data/auth/KtorAuthRepositoryImpl.kt` según `contracts/auth-api.md`: el login y el cambio de contraseña leen ellos mismos la respuesta (`toAuthError()` en los no-2xx) en lugar del `post` genérico; `logout()` intenta `POST /auth/logout` con un timeout corto, ignora el resultado, borra la sesión y vacía la caché de tokens del `BearerAuthProvider`. Hace pasar T017
- [X] T024 Crear `data/auth/SessionStateHolder.kt`: combina `observeSession()`, si la carga terminó y el último `SignOutReason` publicado, y expone un `StateFlow<SessionState>` con `sessionStateOf` (T010). Crear `data/auth/SessionEvents.kt`, un `MutableSharedFlow<SignOutReason>` con `emit(reason)`, que borra la sesión y deja el motivo para la pantalla de login. Registrar ambos en `data/di/CoreDataModule.kt`
- [X] T025 Ajustar todos los usos del modelo antiguo para que compile (`HttpClientFactory.kt`, `CoreDataModule.kt`, `composeApp/.../App.kt`, `composeApp/.../navigation/NavigationRoot.kt`) usando `observeSession()`; el comportamiento nuevo llega en las historias. Comprobar `./gradlew :core:data:allTests :composeApp:assembleDebug :core:data:compileKotlinIosSimulatorArm64` en verde

**Checkpoint**: dominio y datos alineados con el servidor; T005–T007 y T014–T017 en verde en JVM y en iOS.

---

## Phase 3: User Story 1 + User Story 2 — Entrar y ver solo lo del rol (Priority: P1) 🎯 MVP

**Goal**: sin sesión solo hay login; con sesión, las pestañas del rol del servidor.

**Independent Test**: quickstart escenarios 1, 2, 3, 5 y 6.

> US1 y US2 van juntas: la pantalla de login sin puerta de navegación no se puede probar, y la
> puerta sin login tampoco.

- [X] T026 [P] [US1] Crear `feature/auth/presentation/src/commonMain/composeResources/values/strings.xml` con todos los textos de login: título «Granatum Suite», correo, contraseña, «Entrar», validaciones (vacío, formato, longitud), y un mensaje por cada `AuthError` (`InvalidCredentials`: «Correo o contraseña incorrectos», el mismo para los cuatro casos de FR-003; `TooManyAttempts` con y sin segundos; `ServiceBusy`; `NoInternet`; `Timeout`; `InvalidSession`; `Unknown`) y por cada `SignOutReason` (textos de `data-model.md`)
- [X] T027 [P] [US1] Crear `authui/error/AuthErrorUiText.kt`: `AuthError.toUiText()` y `SignOutReason.toUiText()` sobre los recursos de T026 (principio IV: el texto solo en presentation)
- [X] T028 [P] [US1] Test de `LoginViewModel` en `feature/auth/presentation/src/commonTest/.../login/LoginViewModelTest.kt` con un `AuthRepository` falso: no envía con campos vacíos ni con correo inválido; un segundo «Entrar» mientras el primero está en curso no llama dos veces (FR-005); cada `AuthError` se refleja en el estado; el error se limpia al editar
- [X] T029 [US1] Crear `authui/login/LoginState.kt`, `LoginAction.kt` y `LoginViewModel.kt` (patrón MVI de los ViewModels existentes): valida en local (FR-002), llama a `AuthRepository.login` y expone `isLoading`, errores de campo y error global. Hace pasar T028
- [X] T030 [US1] Crear `authui/login/LoginScreen.kt` solo con componentes de `core/designsystem` (`AppPasswordTextField` y los existentes de texto y botón): campos con `maxLength` (254 correo, 128 contraseña), teclado de correo, «Entrar» deshabilitado y con progreso durante el envío, mensaje del motivo de cierre si llega uno, objetivos táctiles ≥ 48dp y semántica para lector de pantalla (principio IX)
  - *Hecho con una diferencia: `AppTextField` no admite `InputTransformation`, así que las longitudes máximas se validan en el ViewModel (con su mensaje) en vez de impedir la escritura. FR-002 se cumple igual: nunca se envía nada más largo.*
- [X] T031 [US1] Crear `authui/navigation/AuthGraphRoutes.kt` (`LoginRoute`, `ChangePasswordRoute`) y `authui/di/AuthPresentationModule.kt` (`viewModelOf(::LoginViewModel)`); registrar el módulo en la inicialización de Koin de `composeApp`
- [X] T032 [US2] Reescribir `composeApp/src/commonMain/kotlin/com/granatum/app/navigation/NavigationRoot.kt` como puerta por `SessionStateHolder.state` (research D8): `Loading` → pantalla de arranque con el logo; `SignedOut(reason)` → `LoginScreen(reason)`; `PasswordChangeRequired` → marcador hasta US3; `Active(role)` → pestañas. Eliminar la caída en `EMPLEADO` por defecto (FR-001)
- [X] T033 [US2] Pestañas por rol en `NavigationRoot.kt` con las propiedades de `UserRole` (FR-009): Fichaje si `canClockIn`, Historial siempre, Inventario si `canManageInventory`, Equipo si `canSeeTeam`; los grafos de inventario y equipo solo se registran si el rol lo permite, para que no sean alcanzables por ruta. El destino inicial es Fichaje si `canClockIn` y si no Historial
- [X] T034 [US2] Historial en modo consulta para quien no puede fichar: comprobar en `feature/clockin/presentation/.../history/` que la pantalla de historial no ofrece ninguna acción de fichar; si la tuviera, ocultarla con un parámetro `canClockIn` pasado desde `NavigationRoot` (US2-3)
- [X] T035 [US2] Rol desconocido: en `data/mappers/SessionMapper.kt`, registrar con `AppLogger.warn` el valor del claim cuando se mapea a `DESCONOCIDO`, sin el token (FR-010)

**Checkpoint**: entrar con cada rol muestra sus pestañas; sin sesión solo hay login.

---

## Phase 4: User Story 3 — Cambio obligatorio de contraseña (Priority: P2)

**Goal**: quien debe cambiar la contraseña no puede hacer nada más hasta cambiarla.

**Independent Test**: quickstart escenario 7.

- [X] T036 [P] [US3] Añadir a `strings.xml` (T026) los textos del cambio: título, explicación, contraseña actual, nueva, repetir, «Cambiar contraseña», «Cerrar sesión», un texto por `PasswordRequirement` («Al menos 8 caracteres», «Una mayúscula», «Una minúscula», «Un número», «Un símbolo», «Como máximo 128 caracteres»), «Las contraseñas no coinciden», «La contraseña actual no es correcta», y el genérico de contraseña débil
- [X] T037 [P] [US3] Test de `ChangePasswordViewModel` en `feature/auth/presentation/src/commonTest/.../password/ChangePasswordViewModelTest.kt`: los requisitos incumplidos se recalculan al escribir; no envía si quedan requisitos o si no coincide la repetición; `InvalidCredentials` conserva la contraseña nueva escrita (FR-015); `WeakPassword(reqs)` muestra exactamente `reqs` (FR-014); doble envío bloqueado
- [X] T038 [US3] Crear `authui/password/ChangePasswordState.kt`, `ChangePasswordAction.kt` y `ChangePasswordViewModel.kt`, con un parámetro `mode: Mandatory | Voluntary` que decide si se ofrece «Cerrar sesión» o «Cancelar». Hace pasar T037
- [X] T039 [US3] Crear `authui/password/ChangePasswordScreen.kt`: tres campos con `AppPasswordTextField` y `maxLength = 128`, lista de requisitos con estado cumplido o pendiente que no dependa solo del color (icono + texto, principio IX), error global, y botón con progreso
- [X] T040 [US3] En `NavigationRoot.kt`, `PasswordChangeRequired` → `ChangePasswordScreen(mode = Mandatory)` como única pantalla; «Cerrar sesión» llama a `AuthRepository.logout()` (US3-7). Al guardar la sesión nueva, el estado pasa solo a `Active(role)` (FR-016)

**Checkpoint**: una cuenta restablecida solo ve el cambio y entra tras cambiarla.

---

## Phase 5: User Story 4 — No perder la sesión sin cobertura (Priority: P2)

**Goal**: solo un rechazo definitivo del servidor cierra la sesión.

**Independent Test**: quickstart escenarios 8 y 9; T041 con `MockEngine`.

- [X] T041 [P] [US4] Test de renovación en `dataTest/networking/HttpClientFactoryRefreshTest.kt` con `MockEngine` y un `SessionStorage` en memoria:
  - (a) `401 TOKEN_ACCESO_EXPIRADO` → una llamada a `/auth/refresh`, sesión nueva guardada, petición repetida con el token nuevo.
  - (b) **Diez peticiones simultáneas** con el acceso caducado → **exactamente una** llamada a `/auth/refresh` (SC-006).
  - (c) refresh con `401 TOKEN_RENOVACION_INVALIDO` → sesión borrada y `SESSION_REJECTED` emitido.
  - (d) refresh con `401 EMPLEADO_INACTIVO` → `INACTIVE`.
  - (e) refresh sin red (excepción del motor), con `503` y con `500` → **la sesión se conserva** (FR-020).
  - (f) `401 NO_AUTENTICADO` en una ruta protegida → sesión borrada, `SESSION_REJECTED`.
  - (g) `401 CREDENCIALES_INVALIDAS` en `change-password` → ni renovación ni cierre.
  - (h) el cuerpo del `401` sigue legible para quien hizo la petición (confirma research D4).
- [X] T042 [US4] En `data/networking/HttpClientFactory.kt`: `reAuthorizeOnResponse { it.status == HttpStatusCode.Unauthorized && it.errorCode() == "TOKEN_ACCESO_EXPIRADO" }`, con un `errorCode()` que lea `ErrorBodyDto` sin lanzar. `loadTokens` lee `observeSession()`
- [X] T043 [US4] Reescribir `refreshTokens` en `HttpClientFactory.kt` según la tabla de `/auth/refresh` de `contracts/auth-api.md`: con éxito, `SessionMapper` y conservar `email`; con `TOKEN_RENOVACION_INVALIDO` o `EMPLEADO_INACTIVO`, `SessionEvents.emit(...)`; con cualquier otra cosa o una excepción, conservar la sesión y devolver `null`. Eliminar el `sessionStorage.set(null)` incondicional
- [X] T044 [US4] Tratar `401 NO_AUTENTICADO` en rutas protegidas: un `HttpResponseValidator` o `ResponseObserver` en `HttpClientFactory.kt` que, fuera de `/auth/`, emita `SESSION_REJECTED`. Hace pasar T041
- [X] T045 [US4] Conectar `SessionEvents` con `NavigationRoot`: tras un cierre forzado, `SignedOut(reason)` muestra el mensaje en la pantalla de login (FR-021)
- [X] T046 [US4] Comprobar que con una sesión guardada y sin red la app abre en `Active(role)`: `SessionStateHolder` no depende de ninguna llamada de red (FR-022, FR-023); dejarlo explícito en su KDoc

**Checkpoint**: T041 en verde; una sesión solo cae por un rechazo definitivo.

---

## Phase 6: User Story 5 — Gestionar mi sesión y fichajes con dueño (Priority: P3)

**Goal**: menú de cuenta, cierre de sesión siempre local, cambio voluntario, aviso de pendientes, y
fichajes atados a quien los hizo (FR-028).

**Independent Test**: quickstart escenarios 10 y 11.

### Fichajes con dueño (FR-028)

- [X] T047 [US5] Añadir `val employeeId: String?` a `feature/clockin/database/src/commonMain/kotlin/com/granatum/feature/clockin/database/entity/ClockEventEntity.kt`, subir `version = 2` y `autoMigrations = [AutoMigration(from = 1, to = 2)]` en `AppClockInDatabase.kt`, y regenerar y versionar `feature/clockin/database/schemas/.../2.json`. Si Room no genera la automigración en KMP, `Migration(1, 2)` con `ALTER TABLE clock_event ADD COLUMN employeeId TEXT` (plan, riesgos)
- [X] T048 [US5] En `ClockEventDao.kt`, añadir `employeeId = :employeeId` a todas las consultas de lectura (`observeAllEvents`, `observeEventsBetween`, `getLatestEvent`, `observeLatestEvent`, `observePendingCount`, `getPendingEvents`) y recibirlo como parámetro. Las filas con `employeeId` nulo no salen en ninguna
- [X] T049 [US5] En `feature/clockin/data/.../datasource/local/OfflineFirstClockInRepositoryImpl.kt` y `.../sync/ClockEventSyncManager.kt`, inyectar `SessionStorage`: sellar cada fichaje nuevo con el `employeeId` de la sesión; sin sesión no se ficha; leer, contar y sincronizar solo los de la persona con sesión, y reaccionar al cambio de sesión (`flatMapLatest` sobre `observeSession()`). Añadir `projects.core.domain` a `feature/clockin/data` si no está
  - *Además: `ClockEventSyncManager` recibe el flujo de conectividad en vez de la `expect class ConnectivityObserver`, que en Android exige un `Context`. Así T050 se ejecuta en `commonTest`.*
- [X] T050 [P] [US5] Test en `feature/clockin/data/src/commonTest/.../ClockEventOwnershipTest.kt` con un DAO falso: los pendientes de A no se envían con la sesión de B; al volver A, se envían; las filas sin dueño nunca se envían (SC-008)

### Menú de cuenta

- [X] T051 [P] [US5] Añadir a `composeApp/src/commonMain/composeResources/values/strings.xml` los textos del menú: «Cuenta», nombre visible de cada rol («Administración», «Encargada», «Empleada», «Representante», «Rol sin reconocer»), «Cambiar contraseña», «Cerrar sesión», el aviso «Tienes N fichajes sin enviar. Si cierras sesión no se enviarán hasta que vuelvas a entrar.» y sus botones
- [X] T052 [US5] Crear `composeApp/src/commonMain/kotlin/com/granatum/app/account/AccountMenu.kt`: avatar con la inicial del correo en la barra superior, que abre una hoja con el correo, la inicial y el rol (FR-025), «Cambiar contraseña» y «Cerrar sesión»
  - *Hecho con una diferencia: el menú de cuenta es una pestaña «Cuenta» en la barra inferior, no un avatar en la barra superior. Cada pantalla tiene ya su propia cabecera y un avatar superpuesto chocaba con ellas; una pestaña es además un objetivo táctil más claro (principio IX).*
- [X] T053 [US5] Cerrar sesión: si `observePendingCount() > 0`, diálogo de aviso (FR-027) y después `AuthRepository.logout()`; sin pendientes, directo. Comprobar que en modo avión vuelve al login de inmediato (FR-026)
- [X] T054 [US5] Cambio voluntario: ruta a `ChangePasswordScreen(mode = Voluntary)` desde el menú; al terminar, vuelve a la pestaña anterior con un aviso de éxito (FR-017)

**Checkpoint**: quickstart escenarios 10 y 11 en verde.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T055 [P] Log sin datos sensibles en `data/networking/HttpClientFactory.kt` (research D5): `filter { !it.url.encodedPath.contains("/auth/") }` y `sanitizeHeader { it == HttpHeaders.Authorization || it == "x-api-key" }` (FR-030)
- [X] T056 [P] Retirar el sembrador (research D10): borrar `data/auth/DevSessionSeeder.kt`, su registro en `CoreDataModule.kt` y su llamada en `composeApp/.../App.kt`; quitar `DEV_SESSION_ROLE` de `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`, de `local.properties.example` y del README. `grep -r DEV_SESSION` debe quedar vacío fuera de `specs/`
- [X] T057 [P] Revisar que no queda ninguna cadena visible en el código de `feature/auth/presentation` ni en `AccountMenu.kt` (principio IX), ni `println` ni `android.util.Log` (principio IV)
- [X] T058 Levantar el backend de Granatum en local en el **puerto 8090** (research D12). **No tocar** el proceso que escucha en el 8080 ni los contenedores `squadfy_backend-*`; la base de datos del backend va en un puerto libre distinto del 5432. Arrancarlo con la caducidad del acceso en 1 minuto para el escenario 8
- [X] T059 Crear las cuentas de prueba con el flujo real del backend: el primer `ADMIN` con `POST /api/auth/registro` y el código de arranque; el resto (`ENCARGADO`, `EMPLEADO`, `REPRESENTANTE` y una cuenta que se restablece para forzar el cambio) con registro y aprobación de ese `ADMIN`. Las credenciales de prueba van a un fichero gitignorado del proyecto, nunca al chat ni a git
- [X] T060 Recorrer el quickstart en el emulador de Android (`-PBASE_URL_HTTP=http://10.0.2.2:8090/api`) y anotar el resultado de cada escenario en `quickstart.md`
- [ ] T061 Recorrer el quickstart en el simulador de iOS (`-PBASE_URL_HTTP_IOS=http://localhost:8090/api`), como mínimo los escenarios 1, 2, 6, 7, 8 y 12, y anotar el resultado (principio XII)
  - *Parcial. Verificado en iOS: compilación, escenario 1, reinstalación sin heredar sesión y los 67 tests en el simulador. Sin hacer: los escenarios con inicio de sesión. La inyección de teclado convierte la `@` en `"` por la distribución española del Mac. Ver la lista de `quickstart.md`.*
- [X] T062 Medir la cobertura (`./gradlew koverXmlReport`), subir `granatum.coverage.minLine` en `gradle.properties` a la cifra medida redondeada hacia abajo y anotarla en `research.md` D11 (principio VIII)
- [X] T063 [P] README: sección «Iniciar sesión en local» (backend en el 8090, cómo crear cuentas de prueba, propiedades de URL) y retirar la mención a `DEV_SESSION_ROLE`
- [X] T064 Verificación final de la CI en local: `./gradlew ktlintCheck testDebugUnitTest koverVerify :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (1)** → **Foundational (2)** → bloquea todas las historias.
- **US1+US2 (3)**: tras la fase 2. Es el MVP.
- **US3 (4)**: tras la fase 3, porque usa la puerta de `NavigationRoot`.
- **US4 (5)**: tras la fase 2. Es independiente de las pantallas salvo T045, que necesita la puerta de la fase 3.
- **US5 (6)**: tras la fase 3; T054 además tras la fase 4.
- **Polish (7)**: tras todas. T058–T061 necesitan la app completa.

### Within Each Phase

Tests (⚠️) antes que su implementación, y deben fallar primero. Dominio antes que datos, y datos
antes que UI.

### Parallel Opportunities

- Fase 2: T005, T006 y T007 en paralelo; T008–T012 en paralelo; T014–T017 en paralelo; T018,
  T019 y T020 en paralelo.
- Fase 3: T026, T027 y T028 en paralelo.
- La fase 5 (US4) puede ir en paralelo con la fase 4 (US3): tocan ficheros distintos.
- Fase 7: T055, T056, T057 y T063 en paralelo.

## Parallel Example: Foundational

```text
T005 PasswordValidatorTest   T006 SessionStateTest   T007 UserRoleTest
T014 AccessTokenClaimsTest   T015 AuthErrorMapperTest  T016 SecureSessionStorageTest  T017 KtorAuthRepositoryImplTest
```

## Implementation Strategy

### MVP

Fases 1, 2 y 3: entrar contra el servidor real y ver las pestañas del rol. Con eso ya se pueden
probar contra el backend real las fases siguientes del roadmap.

### Incremental Delivery

1. MVP (US1 + US2).
2. US4: la renovación deja de expulsar. Es la corrección de mayor impacto para el uso offline.
3. US3: desbloquea a quien tiene contraseña temporal.
4. US5 y FR-028.
5. Polish y verificación real.
