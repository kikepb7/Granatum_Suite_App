# Implementation Plan: Sesión en almacén seguro y navegación por rol

**Branch**: `identity-feature` | **Date**: 2026-10-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-secure-session-storage/spec.md`

## Summary

Sacar las credenciales de sesión del texto plano y llevarlas al almacén seguro de cada
plataforma, sin que ningún consumidor se entere, y comprobar por primera vez que la navegación
por rol se comporta como debe.

El enfoque es una única implementación de `SessionStorage` en `commonMain` sobre un `expect` de
almacén seguro clave-valor: Keystore + DataStore en Android, Keychain en iOS. La parte que
difiere entre plataformas son tres llamadas de sistema; todo lo demás se escribe una vez.

Dos hallazgos de la investigación cambian lo que parecía obvio: `androidx.security:security-crypto`
**está deprecada** desde junio de 2025 y no se usa, y el **Keychain de iOS sobrevive a la
desinstalación**, lo que obliga a purgarlo en el primer arranque. Ver [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 17

**Primary Dependencies**: DataStore 1.1.7 (ya en el catálogo), Android Keystore y
Security.framework de iOS vía plataforma. **Ninguna dependencia nueva**

**Storage**: credenciales de sesión. Ni Room ni datos de negocio entran aquí

**Testing**: `commonTest` para la lógica común; el almacén en sí exige dispositivo. Sin CI,
ktlint ni Kover: verificación manual

**Target Platform**: Android (minSdk 26) e iOS

**Project Type**: aplicación móvil multiplataforma modular

**Performance Goals**: la lectura de sesión no debe estar en la ruta de arranque de la UI más
de lo que ya está. El flujo en memoria de D5 evita leer del almacén en cada consulta

**Constraints**: el contrato `SessionStorage` no cambia (FR-003). Ningún fallo del almacén
puede cerrar la app (FR-006). Paridad Android/iOS obligatoria

**Scale/Scope**: 1 credencial, 2 plataformas, 3 roles a verificar, ~6 ficheros nuevos o
modificados en `core/data`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluado contra la constitución v1.0.0.

| Principio | Puerta | Estado |
|---|---|---|
| **I** Dependencias hacia dentro | `core/domain` sin Android ni frameworks | ✅ el contrato no cambia; todo lo de plataforma vive en `core/data` |
| **II** commonMain por defecto | `expect/actual` solo en las cinco áreas | ✅ almacenamiento seguro **es** una de ellas; además D4 reduce el `expect` a tres operaciones |
| **IV** Errores y observabilidad | Nada de excepciones entre capas; trazas por `AppLogger` | ✅ D6 captura dentro y registra |
| **VII** Credenciales seguras | Keychain / Keystore | ✅ **es la razón de ser de la feature**: cierra la brecha 3 |
| **XI** Higiene de deprecaciones | No arrastrar deprecaciones aguas arriba | ✅ D1 rechaza `security-crypto` por estar deprecada |
| **XII** Paridad Android/iOS | Ambos targets compilan | ⚠️ verificación manual: no hay CI |

**Resultado: PASA.** Sin violaciones que justificar; se omite Complexity Tracking.

La señal de alerta sigue siendo el principio XII, por las brechas 1 y 2. Aquí pesa más que en
la spec 1: la mitad del trabajo es código de plataforma que **solo** se puede comprobar
ejecutando en cada dispositivo. Compilar no demuestra nada sobre si el Keychain guarda.

## Project Structure

### Documentation (this feature)

```text
specs/002-secure-session-storage/
├── plan.md              # Este fichero
├── research.md          # Fase 0 — 9 decisiones
├── data-model.md        # Fase 1 — Sesión, Rol y sus reglas
├── quickstart.md        # Fase 1 — guía de validación
├── contracts/
│   └── secure-store.md  # Fase 1 — contrato del almacén y de fallo
├── checklists/
│   └── requirements.md  # Calidad de la spec (16/16)
└── tasks.md             # Fase 2 — lo genera /speckit-tasks
```

### Source Code (repository root)

```text
core/domain/src/commonMain/.../auth/repository/
└── SessionStorage.kt                     # SIN CAMBIOS — es el contrato (FR-003)

core/data/src/
├── commonMain/kotlin/com/granatum/core/data/auth/storage/
│   ├── DataStoreSessionStorage.kt        # ELIMINAR tras migrar
│   ├── SecureSessionStorage.kt           # NUEVO: única implementación (D4, D5, D6)
│   ├── SecureStore.kt                    # NUEVO: expect — leer/escribir/borrar
│   └── LegacySessionCleaner.kt           # NUEVO: descarta la sesión en claro (D7)
├── androidMain/kotlin/com/granatum/core/data/auth/storage/
│   └── SecureStore.android.kt            # NUEVO: Keystore AES-GCM + DataStore
├── iosMain/kotlin/com/granatum/core/data/auth/storage/
│   └── SecureStore.ios.kt                # NUEVO: Keychain + purga de reinstalación (D3)
├── commonMain/.../di/CoreDataModule.kt   # MODIFICAR: cambiar el binding
├── androidMain/.../di/CoreDataModule.android.kt  # MODIFICAR: registrar el actual
└── iosMain/.../di/CoreDataModule.ios.kt          # MODIFICAR: registrar el actual

build-logic/convention/src/main/kotlin/
└── BuildKonfigConventionPlugin.kt        # MODIFICAR: campo DEV_SESSION_ROLE (D8)

composeApp/src/commonMain/kotlin/com/granatum/app/
└── navigation/NavigationRoot.kt          # SIN CAMBIOS previstos — solo se verifica
```

**Structure Decision**: no se crean módulos. El cambio vive en `core/data`, que ya tiene los
source sets `androidMain` e `iosMain` y sus `platformCoreDataModule` de Koin: la estructura
`expect/actual` **ya existe** y solo se extiende. `core/domain` y `composeApp` no se tocan.

## Fases de implementación

### Fase 1 — El almacén seguro por plataforma

Definir el `expect` de D4 —leer, escribir, borrar una cadena— y sus dos `actual`.

Android: clave AES-GCM en el Keystore, texto cifrado al DataStore ya cableado. Sin
dependencias nuevas, y sin `security-crypto` (D1).

iOS: Keychain con `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` (D2), **incluida la purga
del primer arranque tras reinstalar** (D3). Sin esa purga, iOS incumple FR-005 aunque el código
parezca correcto.

**Verificación**: compila en ambos targets.

### Fase 2 — La implementación común

`SecureSessionStorage` en `commonMain`: serialización, mapeo a dominio, `MutableStateFlow`
sembrado en la primera lectura (D5) y captura de fallos con registro por `AppLogger` (D6).
Cambiar el binding de Koin y eliminar `DataStoreSessionStorage`.

Añadir el borrado de la sesión heredada en claro (D7), con clave **distinta** de la nueva para
no borrar lo recién escrito.

**Verificación**: la app arranca, guarda y recupera sesión en ambas plataformas; el
almacenamiento no revela nada legible.

### Fase 3 — Sembrador de sesiones de prueba

Campo `DEV_SESSION_ROLE` en el convention plugin y sembrador registrado **solo** cuando el
entorno es `local` (D8). Pide el token al extremo de desarrollo del backend y escribe la sesión.

### Fase 4 — Verificación por rol y cierre

Los tres roles, uno por compilación, comprobando qué áreas alcanza cada uno. Más los casos
límite: almacén no disponible, credencial corrupta, reinstalación en iOS.

```bash
./gradlew :composeApp:assembleDebug
./gradlew :core:data:compileKotlinIosSimulatorArm64
```

## Riesgos

| Riesgo | Mitigación |
|---|---|
| El Keychain sobrevive a la desinstalación y nadie lo nota | D3 lo resuelve y el quickstart lo prueba explícitamente reinstalando |
| Compilar no demuestra que el almacén funcione | La verificación es en dispositivo, no en el build. Es lo que más pesa al no haber CI |
| El borrado de la clave heredada se lleva la nueva | Claves distintas, comprobado en la fase 2 |
| El backend debe estar en perfil `dev` para sembrar roles | Solo afecta a la fase 3; las fases 1 y 2 no necesitan backend |
| `AuthInfoModel` podría no traer rol real en los tokens de desarrollo | Se verifica al sembrar el primer rol, antes de dar por buena la fase 4 |
