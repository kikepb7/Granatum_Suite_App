# Implementation Plan: Inicio de sesión, sesión real y navegación por roles

**Branch**: `login-feature` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-login-roles/spec.md`

## Summary

La app inicia sesión contra el backend real, guarda una sesión cuya identidad y rol salen del
propio token, y decide la navegación por un estado de sesión explícito. Con eso se corrigen tres
defectos que hoy impiden usarla contra el servidor:

- el login no deserializa, porque la app espera una forma de respuesta que el servidor no envía;
- la renovación del token borra la sesión ante cualquier fallo, incluida la falta de red;
- sin sesión, la app se muestra como si la persona fuera empleada.

Se añaden la pantalla de login, el cambio de contraseña (obligatorio y voluntario), el menú de
cuenta, el cuarto rol y la atribución de cada fichaje pendiente a quien lo hizo. Se retira el
sembrador de sesiones de desarrollo. Ver [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 17, Gradle 8.14.3

**Primary Dependencies**: Compose Multiplatform 1.9.0-beta01, Ktor 3.2.3 (plugins `Auth` y
`Logging`), Koin 4.1.0, Room 2.7.2, kotlinx.serialization 1.9.0. Nueva solo para tests:
`ktor-client-mock` 3.2.3.

**Storage**: almacén seguro de la spec 002 (Keystore y Keychain) para la sesión; Room para la
columna nueva de fichajes.

**Testing**: `commonTest` con `kotlin-test` y `MockEngine`, ejecutados en JVM y en el simulador
de iOS por la CI; verificación en dispositivo contra el backend local (quickstart).

**Target Platform**: Android (minSdk del proyecto) e iOS (simulador arm64 y dispositivo).

**Project Type**: aplicación móvil KMP.

**Performance Goals**: entrar en menos de 30 s desde la primera apertura (SC-001); arranque con
sesión sin destello del login.

**Constraints**: offline-first (una sesión guardada funciona sin red); nada sensible en el log;
contrato fijado en `d1857ad`; sin cambios en `DataError`.

**Scale/Scope**: 1 módulo nuevo (`feature/auth/presentation`), 2 pantallas, 1 menú, 4 roles,
1 migración de Room, unos 25 ficheros tocados.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluado contra la constitución **v1.1.2**.

| Principio | Puerta | Estado |
|---|---|---|
| **I** Arquitectura por módulos | Dependencias hacia dentro; patrón de feature | ✅ con matiz: `feature/auth` solo tiene `presentation`, porque la sesión ya vive en `core` (D7) |
| **II** commonMain por defecto | Lógica compartida | ✅ todo en `commonMain`; nada de plataforma nuevo |
| **III** Offline-first | La sesión no depende de la red | ✅ es el motivo de D4 y FR-020/FR-022 |
| **IV** Errores explícitos | `Result<D, E>`, `AppLogger`, `toUiText()` en presentation | ✅ `AuthError : Error` (D3); textos solo en `presentation` |
| **V** Secretos por entorno | Sin secretos nuevos | ✅ se retira `DEV_SESSION_ROLE` |
| **VI** El contrato manda | Nada inventado | ✅ todo sale de `docs/openapi.json` y de la documentación del backend en el mismo commit; ver [contracts/auth-api.md](./contracts/auth-api.md) |
| **VII** Credenciales seguras | Sesión en Keystore/Keychain | ✅ clave `v2` en el mismo almacén; la `v1` se borra |
| **VIII** Tests y CI | Tests reales; trinquete | ✅ primeros tests de producción (D11); el umbral sube al terminar |
| **IX** UI en español, accesible | Design system, recursos | ✅ las pantallas nuevas usan `core/designsystem` y `composeResources` propios: son las primeras sin cadenas en el código |
| **X** Feature documentada | Spec, plan, README | ✅ |
| **XI** Build en convention plugins | Módulo nuevo sin configuración ad hoc | ✅ `convention.cmp.feature`; `ktor-client-mock` en el catálogo |
| **XII** Paridad Android/iOS | Ambos compilan y se verifican | ✅ quickstart en emulador y simulador |

**Resultado: PASA.** El único matiz, `feature/auth` sin `domain`, `data` ni `database`, se
justifica en Complexity Tracking.

**Re-evaluación tras el diseño**: sin cambios. El diseño no añade dependencias de producción, no
toca `DataError` y no mueve nada fuera de `commonMain`.

## Project Structure

### Documentation (this feature)

```text
specs/004-login-roles/
├── plan.md              # Este fichero
├── research.md          # Fase 0 — 12 decisiones
├── data-model.md        # Fase 1 — sesión, roles, estados, errores, migración
├── quickstart.md        # Fase 1 — 12 escenarios en dispositivo
├── contracts/
│   └── auth-api.md      # Fase 1 — reacción de la app a cada respuesta
├── checklists/
│   └── requirements.md  # Calidad de la spec
└── tasks.md             # Fase 2 — lo genera /speckit-tasks
```

### Source Code (repository root)

```text
core/domain/src/commonMain/kotlin/com/granatum/core/domain/
├── auth/model/Session.kt                  # NUEVO (sustituye a AuthInfoModel + UserModel)
├── auth/model/UserRole.kt                 # MODIFICAR: REPRESENTANTE, DESCONOCIDO, permisos
├── auth/model/SessionState.kt             # NUEVO
├── auth/model/SignOutReason.kt            # NUEVO
├── auth/AuthError.kt                      # NUEVO
├── auth/repository/AuthRepository.kt      # MODIFICAR: Result<_, AuthError>
├── auth/repository/SessionStorage.kt      # MODIFICAR: Session
└── validation/PasswordValidator.kt        # MODIFICAR: política del backend (D9)

core/data/src/commonMain/kotlin/com/granatum/core/data/
├── auth/dto/                              # REESCRIBIR: ParTokensResponse, requests, ErrorBody
├── auth/token/AccessTokenClaims.kt        # NUEVO: decodificación sin firma (D1)
├── auth/AuthErrorMapper.kt                # NUEVO: respuesta → AuthError (D3)
├── auth/KtorAuthRepositoryImpl.kt         # REESCRIBIR
├── auth/SessionEvents.kt                  # NUEVO: motivo de cierre (D4)
├── auth/DevSessionSeeder.kt               # BORRAR (D10)
├── auth/storage/SecureSessionStorage.kt   # MODIFICAR: v2, borra v1
├── mappers/AuthInfoMapper.kt              # REESCRIBIR como SessionMapper
└── networking/HttpClientFactory.kt        # MODIFICAR: renovación (D4) y log (D5)

feature/auth/presentation/                 # NUEVO MÓDULO
├── build.gradle.kts                       # solo convention.cmp.feature + dependencias
└── src/commonMain/
    ├── composeResources/values/strings.xml
    └── kotlin/com/granatum/feature/auth/presentation/
        ├── login/                         # LoginScreen, LoginViewModel, estado y acciones
        ├── password/                      # ChangePasswordScreen (obligatorio y voluntario)
        ├── navigation/AuthGraphRoutes.kt
        └── di/AuthPresentationModule.kt

feature/clockin/database/                  # MODIFICAR: employeeId, versión 2, AutoMigration
feature/clockin/data/                      # MODIFICAR: sellar y filtrar por employeeId (D6)

composeApp/src/commonMain/kotlin/com/granatum/app/
├── App.kt                                 # MODIFICAR: sin seeder; limpia la sesión v1
└── navigation/NavigationRoot.kt           # MODIFICAR: puerta por SessionState, pestañas por rol, menú de cuenta

build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt  # MODIFICAR: sin DEV_SESSION_ROLE
gradle/libs.versions.toml                  # MODIFICAR: ktor-client-mock
settings.gradle.kts                        # MODIFICAR: include(":feature:auth:presentation")
```

**Structure Decision**: dominio y datos de auth se quedan en `core`, donde ya están, porque el
cliente HTTP compartido necesita la sesión. Solo la UI es una feature. Ningún
`build.gradle.kts` existente gana configuración de build: el módulo nuevo usa el mismo convention
plugin que los demás.

## Fases de implementación

### Fase 1 — El dominio y los datos hablan el idioma del servidor

`Session`, `UserRole` con cuatro roles, `AuthError`, `PasswordValidator` y la decodificación del
token, con sus tests primero (D11). DTOs nuevos, mapeador de errores y repositorio reescrito.
Almacén `v2`. **Verificación**: tests en verde en JVM y en iOS.

### Fase 2 — La renovación deja de expulsar

`HttpClientFactory`: criterio de renovación, clasificación de `/auth/refresh`, `SessionEvents`,
filtro del log. Tests con `MockEngine`, incluidas las diez peticiones simultáneas.

### Fase 3 — Las pantallas

Módulo `feature/auth/presentation`: login y cambio de contraseña, con textos en recursos.
`SessionState` y la puerta en `NavigationRoot`. Pestañas por rol y menú de cuenta.

### Fase 4 — Fichajes con dueño

Columna `employeeId` y migración. Los fichajes se sellan con la persona y se filtran por ella.
Aviso de pendientes al cerrar sesión.

### Fase 5 — Limpieza y verificación real

Retirar `DevSessionSeeder` y `DEV_SESSION_ROLE`. Levantar el backend en el 8090, crear las
cuentas de prueba y recorrer el quickstart en Android e iOS. Subir el trinquete de cobertura.
README.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El cuerpo del `401` no se puede leer dos veces en Ktor 3.2.3 | Test en la fase 2 (D4); alternativa: leerlo una vez y guardarlo en un atributo de la llamada |
| El `docker compose` del backend choca con el PostgreSQL de Squadfy en el 5432 | Levantar la base del backend en otro puerto; no tocar los contenedores de Squadfy (D12) |
| El acceso dura 15 minutos y el escenario 8 se vuelve lento | Arrancar el backend local con una caducidad de 1 minuto |
| Room no genera la migración automática en KMP | El esquema v1 ya está exportado; si falla, migración manual de una línea (`ALTER TABLE … ADD COLUMN`) |
| Al reescribir `UserModel` se rompen pantallas que lo usan | El compilador las señala; hoy solo `NavigationRoot` y el sembrador lo leen |

## Complexity Tracking

| Desviación | Por qué hace falta | Alternativa más simple descartada porque |
|---|---|---|
| `feature/auth` solo con `presentation` (principio I) | La sesión la necesita el cliente HTTP de `core/data` | Moverla a `feature/auth/{domain,data}` obligaría a que `core` dependa de una feature, lo que el mismo principio prohíbe; crear módulos vacíos sería forma sin contenido |
| `AuthError` además de `DataError` (principio IV) | Los errores de auth se distinguen por `code`, no por estado HTTP, y llevan datos (`Retry-After`, requisitos) | Ampliar `DataError` rompería su identidad con Squadfy, que la constitución exige |
