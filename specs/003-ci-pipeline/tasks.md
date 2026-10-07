# Tasks: Pipeline de integración continua

**Input**: Documentos de diseño en `/specs/003-ci-pipeline/`

**Prerequisites**: plan.md, spec.md, research.md (D1-D12), data-model.md, contracts/ci-workflow.md, quickstart.md

**Tests**: Sin tareas de test propias. La feature **es** la infraestructura que ejecuta los tests; lo que se prueba es la propia CI, con los escenarios de `quickstart.md`.

**Organization**: Agrupado por historia de usuario. Las fases 1 y 2 son el build que la CI necesita para poder existir; las historias son el workflow.

> **Push y PRs.** Las tareas de verificación real en GitHub Actions (T027-T031) exigen subir ramas a un repositorio **público**. Requieren confirmación explícita del propietario antes de ejecutarse, o las ejecuta él.

## Estado: 28/33 completadas (2026-10-08)

Todos los comandos del workflow verificados en local **en condiciones de CI** —sin
`local.properties`, con `ORG_GRADLE_PROJECT_API_KEY`—: ktlint, tests unitarios, informe y
trinquete de cobertura, build de Android, y enlazado del framework de iOS más ejecución del
test en el simulador. `actionlint` sin errores.

Hallazgos de la implementación:

- **El único test del proyecto nunca había compilado.** `CmpApplicationConventionPlugin` no
  añadía `kotlin-test` a `commonTest`, a diferencia de `KmpLibraryConventionPlugin`, así que el
  test de `composeApp` fallaba con *«Unresolved reference 'test'»*. Corregido en el plugin. Ahora
  se ejecuta en JVM y en el simulador de iOS.
- **ktlint encontró 1.464 violaciones**, lo que confirma que hacerlo bloqueante de entrada no
  era viable.
- **Cobertura de líneas medida: 0 / 4.843 = 0,00 %.** El trinquete arranca en 0, y se comprobó
  que falla si se fuerza por encima.
- Versiones de D5 compatibles con Gradle 8.14.3; no hizo falta bajar a las de Squadfy.
- La agregación quedó en la variante total de Kover; el intento con la variante `debug` falló.
  Detalle en `research.md` D7.

Quedan 5 tareas:

| Tarea | Por qué |
|---|---|
| T027-T030 | Exigen subir ramas al repositorio público y abrir PRs: requieren confirmación del propietario |
| T031 | Depende de T028 y T029: no hay ramas de prueba que borrar todavía |

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (fichero distinto, sin dependencias pendientes)
- **[US#]**: historia de usuario a la que pertenece

---

## Phase 1: Setup

- [X] T001 Verificar la línea base: `./gradlew :composeApp:assembleDebug` y `./gradlew :core:data:compileKotlinIosSimulatorArm64` en verde antes de tocar nada
- [X] T002 [P] Confirmar que existen en el portal de plugins las versiones de D5: ktlint-gradle `14.2.0` (`org.jlleitschuh.gradle.ktlint`) y Kover `0.9.11` (`org.jetbrains.kotlinx.kover`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: que el build pueda configurarse en la CI y que ktlint y Kover lleguen a todos los módulos.

**⚠️ CRITICAL**: ningún job del workflow puede pasar sin esta fase.

### Configurar sin `local.properties`

- [X] T003 En `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`, leer `API_KEY` con la función `resolve()` existente en lugar de `localProperties.getProperty("API_KEY")`, conservando el `IllegalStateException` si no aparece por ninguna vía (D3)
- [X] T004 Verificar que compila **sin** `local.properties` con `-PAPI_KEY=ci-placeholder`, y que **sin** esa propiedad falla nombrando `API_KEY` (quickstart escenario 1, FR-014). Restaurar `local.properties` al terminar

### ktlint y Kover en todos los módulos

- [X] T005 Añadir a `gradle/libs.versions.toml` las versiones `ktlint-gradle = "14.2.0"` y `kover = "0.9.11"`, los alias de plugin `ktlint` y `kover`, sus coordenadas como librerías para `build-logic`, y el alias `convention-quality`
- [X] T006 Añadir las dependencias de ktlint y Kover al classpath de `build-logic/convention/build.gradle.kts` y registrar el plugin `convention.quality` apuntando a `QualityConventionPlugin`
- [X] T007 Crear `build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt`: aplica ktlint con `ignoreFailures = true` (informativo, D2) y Kover. KDoc explicando por qué ktlint no bloquea y cuándo dejará de hacerlo
- [X] T008 [P] Aplicar `convention.quality` desde `build-logic/convention/src/main/kotlin/KmpLibraryConventionPlugin.kt`
- [X] T009 [P] Aplicar `convention.quality` desde `build-logic/convention/src/main/kotlin/CmpApplicationConventionPlugin.kt`
- [X] T010 Aplicar Kover en el `build.gradle.kts` raíz y agregar los informes de todos los subproyectos (D7)
- [X] T011 Verificar que **ningún** `build.gradle.kts` de módulo se ha modificado (principio XI): `git diff --stat -- '*/build.gradle.kts'` solo puede mostrar la raíz y `build-logic`
- [X] T012 Verificar `./gradlew ktlintCheck`: se ejecuta en todos los módulos y termina en verde aunque haya violaciones. Anotar cuántas encuentra
- [X] T013 Verificar si el informe total de Kover (`koverXmlReport` en la raíz) incluye los tests unitarios de Android de los módulos KMP; si no, pasar a la variante `debug` y anotar la decisión en `research.md` D7

### El trinquete

- [X] T014 Medir la cobertura de líneas agregada ejecutando `./gradlew testDebugUnitTest koverXmlReport -PAPI_KEY=ci-placeholder` y leyendo el informe XML de la raíz
- [X] T015 Escribir en `gradle.properties` la línea `granatum.coverage.minLine=<valor medido redondeado hacia abajo>` con un comentario que diga que es un trinquete del principio VIII: subir es libre, bajar exige enmienda (D6). Valor esperado: `0`
- [X] T016 Hacer que `QualityConventionPlugin` o el build raíz lea `granatum.coverage.minLine` y lo use como regla de verificación de Kover; comprobar con `./gradlew koverVerify -PAPI_KEY=ci-placeholder` que pasa con el valor fijado y **falla** si se pone temporalmente por encima de la cobertura real

**Checkpoint**: el build se configura sin secretos, ktlint y Kover funcionan en todo el proyecto y el trinquete está fijado.

---

## Phase 3: User Story 1 - Nada llega a main sin compilar en las dos plataformas (Priority: P1) 🎯 MVP

**Goal**: cada PR contra `main` compila Android e iOS y ejecuta los tests sin intervención humana.

**Independent Test**: una PR que rompa solo iOS queda marcada como fallida.

### Implementation for User Story 1

- [X] T017 [US1] Crear `.github/workflows/ci.yml` con los disparadores de D1 —`pull_request` contra `main`, `push` a `main`, `workflow_dispatch`; **nunca** `pull_request_target`—, `permissions: contents: read`, y concurrencia `group: ci-${{ github.ref }}` con `cancel-in-progress: true` (FR-001, FR-002, FR-013, FR-015)
- [X] T018 [US1] Job `build-android` en `.github/workflows/ci.yml`: `ubuntu-latest`, JDK 17 temurin, `gradle/actions/setup-gradle@v4`, `./gradlew :composeApp:assembleDebug -PAPI_KEY=ci-placeholder`, y subida de `composeApp/build/outputs/apk/debug/composeApp-debug.apk` como artefacto (FR-003, FR-012)
- [X] T019 [US1] Job `build-ios` en `.github/workflows/ci.yml`: `macos-latest`, JDK 17, caché de `~/.konan`, `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test -PAPI_KEY=ci-placeholder` (FR-004, D8). Enlazar el framework compila **todos** los módulos compartidos, no solo `core:data`
- [X] T020 [US1] Job `unit-tests` en `.github/workflows/ci.yml`: `ubuntu-latest`, `./gradlew testDebugUnitTest -PAPI_KEY=ci-placeholder`, seguido de un paso que cuente los `<testcase>` en `**/build/test-results/**/*.xml` y **falle si son cero** (FR-005, FR-017, D9)

**Checkpoint**: US1 completa. Ninguna rotura de compilación pasa desapercibida.

---

## Phase 4: User Story 2 - Un fallo es imposible de pasar por alto (Priority: P2)

**Goal**: un único indicador global que es rojo si cualquier etapa falla, se cancela o se agota.

**Independent Test**: provocar el fallo de una sola etapa y ver que el resumen la señala.

### Implementation for User Story 2

- [X] T021 [US2] Job `summary` en `.github/workflows/ci.yml`: `needs` todos los jobs, `if: always()`, escribe en `$GITHUB_STEP_SUMMARY` una tabla con el estado de cada job y **falla** si alguno bloqueante terminó en `failure` o `cancelled` (FR-010, FR-011)
- [X] T022 [US2] Añadir `timeout-minutes` a cada job de `.github/workflows/ci.yml`: análisis estático 15, tests 20, Android 20, iOS 45, resumen 5 (FR-016, D11)

**Checkpoint**: US1 y US2 completas. Un rojo es evidente y atribuible.

---

## Phase 5: User Story 3 - La calidad del código se ve sin tener que buscarla (Priority: P3)

**Goal**: estilo, cobertura y número de tests visibles en cada PR.

**Independent Test**: abrir una PR y encontrar sin buscar el resultado de ktlint, la cobertura y el APK.

### Implementation for User Story 3

- [X] T023 [P] [US3] Job `static-analysis` en `.github/workflows/ci.yml`: `./gradlew ktlintCheck`, subida de los informes como artefacto, y recuento de violaciones en `$GITHUB_STEP_SUMMARY`. **No bloqueante** (FR-006)
- [X] T024 [US3] En el job `unit-tests`, generar `koverXmlReport` y ejecutar `koverVerify` contra el trinquete, publicar el porcentaje de cobertura de líneas y el número de tests en `$GITHUB_STEP_SUMMARY`, y subir los informes como artefacto (FR-007, FR-008)

**Checkpoint**: las tres historias completas.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T025 [P] Añadir a `README.md` una sección de CI: qué jobs hay, qué bloquea y qué no, cómo subir el trinquete de cobertura, y el paso del administrador para exigir el job `summary` en la protección de `main` (D12)
- [X] T026 Validar en local los escenarios 1, 2 y 3 de `quickstart.md`, y validar la sintaxis de `.github/workflows/ci.yml`
- [ ] T027 ⚠️ **Requiere confirmación.** Subir la rama y abrir la PR contra `main`; comprobar en GitHub Actions que los cinco jobs terminan, `summary` en verde, cobertura y tests visibles y APK descargable (quickstart escenario 4)
- [ ] T028 ⚠️ **Requiere confirmación.** En una rama de prueba, romper solo `iosMain` y comprobar que `build-ios` y `summary` fallan mientras Android sigue en verde (quickstart escenario 5, SC-002)
- [ ] T029 ⚠️ **Requiere confirmación.** En una rama de prueba, borrar el único test y comprobar que `unit-tests` falla por cero tests (quickstart escenario 6, FR-017)
- [ ] T030 ⚠️ **Requiere confirmación.** Dos pushes seguidos a la rama de la PR: la primera ejecución se cancela (quickstart escenario 8)
- [ ] T031 Borrar las ramas de prueba de T028 y T029, local y remotamente
- [X] T032 Retirar el *Sync Impact Report* de la cabecera de `.specify/memory/constitution.md` antes de commitear la enmienda, para no repetir el error de la v1.0.0
- [X] T033 Dejar anotado en `quickstart.md` que el escenario 7 —bajar la cobertura falla— no es verificable mientras el umbral sea 0 %

---

## Dependencies & Execution Order

```text
Phase 1 (Setup)
   ↓
Phase 2 (Foundational) ── T003-T004 primero: sin API_KEY no configura nada
   ↓
Phase 3 (US1) ◄── MVP
   ↓
Phase 4 (US2)  necesita los jobs de US1 para resumirlos
   ↓
Phase 5 (US3)  amplía el job de tests de US1
   ↓
Phase 6 (Polish) ── T027-T030 exigen push
```

### Dentro de cada fase

`.github/workflows/ci.yml` concentra T017-T024: **no son paralelizables entre sí**, salvo T023, que añade un job independiente. En `build-logic`, T008 y T009 tocan ficheros distintos.

### Parallel Opportunities

- **T008 y T009**: los dos plugins base
- **T023 y T025**: el job de estilo y el README no se pisan

---

## Implementation Strategy

### MVP

**Fases 2 y 3**: con el build configurable sin secretos y los jobs de Android, iOS y tests, la CI ya cumple su promesa central —nada llega a `main` sin compilar en las dos plataformas—. Todo lo demás la hace más legible.

### Entrega incremental

1. **Fase 2** → el proyecto ya compila en una máquina sin configuración local
2. **Fase 3** → la CI existe y protege `main`
3. **Fase 4** → el rojo es evidente
4. **Fase 5** → la calidad se ve
5. **Fase 6** → verificada de verdad en GitHub, no solo en local
