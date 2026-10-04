# Implementation Plan: Identidad de app y conexión por entorno

**Branch**: `identity-feature` | **Date**: 2026-10-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-backend-env-config/spec.md`

## Summary

Sustituir las URLs fijas de `core/data` por configuración inyectada en el binario, con tres
entornos elegibles al construir y la garantía de que pruebas y producción solo resuelven
HTTPS. En paralelo, retirar los flujos de autenticación heredados del esqueleto que el
producto no usa, y reescribir el README para Granatum Suite.

El enfoque técnico se apoya en lo que ya existe: `BuildKonfigConventionPlugin` aplica desde
hace tiempo el patrón de leer `local.properties` y fallar rápido si falta una clave. La
feature lo extiende con flavors por entorno y configuración por target, en vez de introducir
un mecanismo nuevo. Ver [research.md](./research.md) para las nueve decisiones y sus
alternativas descartadas.

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 17

**Primary Dependencies**: BuildKonfig 0.17.1 (configuración en el binario), Ktor (cliente
HTTP), Koin (inyección), Compose Multiplatform (UI). AGP 8.11.2, KSP 2.2.20-2.0.4

**Storage**: N/A para esta feature. No introduce persistencia

**Testing**: `commonTest` con `kotlin-test`. Sin CI, sin ktlint y sin Kover en el
repositorio: verificación manual obligatoria de ambos targets

**Target Platform**: Android (minSdk 26, targetSdk 36) e iOS. No hay escritorio ni web

**Project Type**: Aplicación móvil multiplataforma, modular, con build-logic propio

**Performance Goals**: N/A. La feature no está en ninguna ruta crítica de rendimiento

**Constraints**: Pruebas y producción solo HTTPS. Sin literales de URL en código fuente.
Fallo en tiempo de construcción si falta configuración. Paridad Android/iOS obligatoria

**Scale/Scope**: 3 entornos, 2 plataformas, ~8 ficheros tocados, 5 métodos y 3 DTOs
retirados

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluado contra la constitución v1.0.0.

| Principio | Puerta | Estado |
|---|---|---|
| **II** commonMain por defecto | La diferencia emulador/simulador NO puede resolverse con un `expect/actual` nuevo | ✅ se resuelve en configuración de build (D2) |
| **V** Secretos por entorno | URLs y claves desde `local.properties`, fuera de git, producción solo HTTPS | ✅ es el núcleo de la feature; validación en el plugin (D3) |
| **VI** El contrato manda | No inventar endpoints | ✅ solo se usa la comprobación de salud; no depende de `openapi.json` |
| **IX** Textos en recursos | Nombre visible en recursos, español primero | ✅ `strings.xml` / `CFBundleDisplayName` (D9) |
| **XI** Build en convention plugins | Nada de configuración ad-hoc en módulos; versiones solo en el catálogo | ✅ se extiende `BuildKonfigConventionPlugin` |
| **XII** Paridad Android/iOS | Ambos targets compilan | ⚠️ exige verificación manual: no hay CI |

**Resultado: PASA.** Sin violaciones que justificar, luego la sección de Complexity Tracking
se omite.

La única señal de alerta es el principio XII. Las brechas 1 y 2 de la constitución —no hay
CI, ni ktlint, ni Kover— significan que nada automático impedirá fusionar con iOS roto. La
fase 4 del plan lo compensa con verificación manual explícita, pero es una mitigación, no
una solución.

## Project Structure

### Documentation (this feature)

```text
specs/001-backend-env-config/
├── plan.md              # Este fichero
├── research.md          # Fase 0 — 9 decisiones técnicas
├── data-model.md        # Fase 1 — entidad Entorno y sus reglas
├── quickstart.md        # Fase 1 — guía de validación
├── contracts/
│   └── build-config.md  # Fase 1 — claves de local.properties y campos generados
├── checklists/
│   └── requirements.md  # Calidad de la spec (16/16)
└── tasks.md             # Fase 2 — lo genera /speckit-tasks, NO este comando
```

### Source Code (repository root)

Ficheros que toca la feature, todos existentes salvo donde se indica:

```text
build-logic/convention/src/main/kotlin/
└── BuildKonfigConventionPlugin.kt        # MODIFICAR: flavors, targets y validación HTTPS

gradle/
└── libs.versions.toml                    # sin cambios previstos (BuildKonfig 0.17.1 ya está)

core/data/
├── build.gradle.kts                      # ya aplica convention.buildkonfig
└── src/commonMain/kotlin/com/granatum/core/data/
    ├── networking/
    │   ├── UrlConstants.kt               # MODIFICAR: envoltorio sobre BuildKonfig (D10)
    │   ├── HttpClientExt.kt              # SIN CAMBIOS gracias al envoltorio
    │   └── HttpClientFactory.kt          # revisar: usa RefreshRequestDTO, se conserva
    └── auth/
        ├── KtorAuthRepositoryImpl.kt     # MODIFICAR: retirar 5 implementaciones
        ├── provider/AuthRoutes.kt        # MODIFICAR: retirar 5 rutas
        └── dto/request/
            ├── RegisterRequestDTO.kt     # ELIMINAR
            ├── ResetPasswordRequestDTO.kt# ELIMINAR
            └── EmailRequestDTO.kt        # ELIMINAR

core/domain/src/commonMain/kotlin/com/granatum/core/domain/auth/repository/
└── AuthRepository.kt                     # MODIFICAR: retirar 5 métodos de la interfaz

composeApp/src/androidMain/res/values/
└── strings.xml                           # nombre visible "Granatum Suite"

iosApp/
└── Info.plist                            # CFBundleDisplayName

local.properties.example                  # MODIFICAR: documentar las claves por entorno
README.md                                 # REESCRIBIR: conservar Spec Kit y Project structure
```

**Structure Decision**: no se crean módulos nuevos. La feature es transversal y vive en
`build-logic` (el mecanismo) y en `core/{domain,data}` (los consumidores). Se respeta la
estructura modular existente del principio I sin tocar ninguna `feature/`.

## Fases de implementación

Orden deliberado: el bloque 2 va primero porque reduce la superficie que el bloque 1 tiene
que migrar.

### Fase 1 — Limpieza de autenticación

Independiente del resto y verificable sola. Retirar de `AuthRepository` los cinco métodos
de D4, sus implementaciones en `KtorAuthRepositoryImpl`, las cinco rutas de `AuthRoutes` y
los tres DTOs de D5. **Conservar** `login`, `changePassword`, `logout` y `RefreshRequestDTO`.

No hay pantallas ni tests que tocar (D6, D7). Cuidado con los tres falsos positivos de D7:
`registerNetworkCallback`, `registeredTypeIdentifiers` y el `register` de Gradle no son auth.

**Verificación**: compila en ambos targets y no queda ninguna referencia a los símbolos
retirados.

### Fase 2 — Configuración por entorno

Extender `BuildKonfigConventionPlugin` siguiendo el patrón de Squadfy_KMM: la cadena de
resolución `resolve(key, default)` —propiedad de Gradle, luego `local.properties`, luego
valor por defecto— más tres flavors (`local`, `staging`, `prod`) y la validación de HTTPS
para staging y producción (D3). Mantener el patrón de fallo rápido que ya usa `API_KEY`.

Añadir el `targetConfigs` para iOS que Squadfy no tiene (D2): es lo que hace que el
simulador alcance el backend local sin override manual.

Convertir `UrlConstants.kt` en envoltorio sobre `BuildKonfig`, al estilo Squadfy (D10). Su
consumidor `HttpClientExt.kt` **no se toca**. `BASE_URL_WS` desaparece sin sustituto (D8).

Actualizar `local.properties.example` con las claves nuevas y su significado, sin valores
reales.

**Verificación**: construir con cada flavor y comprobar a qué servidor apunta cada binario;
intentar construir producción con una URL en claro y comprobar que falla.

### Fase 3 — Identidad visible

Nombre «Granatum Suite» en `strings.xml` y en `CFBundleDisplayName` (D9). Icono provisional
con la inicial. Comprobar en dispositivo si iOS recorta el nombre y, solo entonces, aplicar
el nombre corto del caso límite.

### Fase 4 — README y verificación final

Reescribir el README conservando «Spec-driven development (Spec Kit)» y «Project structure».

Verificación manual obligatoria, porque no hay CI que la respalde:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :core:data:compileKotlinIosSimulatorArm64
```

Más el arranque real en emulador de Android y simulador de iOS contra el backend local, con
la comprobación de salud respondiendo en ambos. Es el criterio SC-002 y no se puede dar por
bueno sin ejecutarlo.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Sin CI, iOS puede romperse sin que nadie se entere | Verificación manual explícita en la fase 4; cerrar la brecha 1 de la constitución cuanto antes |
| `HttpClientFactory` usa `RefreshRequestDTO` y no es obvio | Documentado en D5; no borrar ese DTO |
| iOS contra backend local probablemente nunca funcionó | La fase 2 lo arregla; la fase 4 lo verifica por primera vez |
| El inventario de huérfanos podría tener más casos | Se rastreó DTO a DTO; tras la fase 1, compilar es la red de seguridad |
