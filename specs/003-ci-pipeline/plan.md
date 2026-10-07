# Implementation Plan: Pipeline de integración continua

**Branch**: `ci-pipeline-feature` | **Date**: 2026-10-08 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-ci-pipeline/spec.md`

## Summary

Un workflow de GitHub Actions que, en cada PR contra `main` y en cada push a `main`, compila
Android e iOS, ejecuta los tests en ambas plataformas, mide la cobertura con un trinquete,
publica el análisis de estilo y resume el resultado en un único indicador. Cierra las brechas
1 y 2 de la constitución.

Tres cambios en el build lo hacen posible: un convention plugin nuevo que aplica ktlint y
Kover a todos los módulos (principio XI), el umbral del trinquete en una línea de
`gradle.properties`, y que `API_KEY` admita propiedad de Gradle para que la CI compile sin
`local.properties`. Ver [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20, JDK 17, Gradle 8.14.3

**Primary Dependencies**: GitHub Actions; ktlint-gradle 14.2.0 y Kover 0.9.11, nuevos en el
catálogo; `actions/checkout@v4`, `actions/setup-java@v4`, `gradle/actions/setup-gradle@v4`,
`actions/upload-artifact@v4`

**Storage**: N/A

**Testing**: el único test existente (placeholder de `composeApp/commonTest`), ejecutado en
JVM y en el simulador de iOS

**Target Platform**: runners `ubuntu-latest` y `macos-latest`

**Project Type**: aplicación móvil KMP; la feature es infraestructura de build

**Performance Goals**: pipeline completo en menos de 30 minutos con caché (SC-004)

**Constraints**: sin secretos; seguro para PRs desde forks; cero configuración local

**Scale/Scope**: 1 workflow, 5 jobs, 13 módulos, 1 convention plugin nuevo

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Evaluado contra la constitución **v1.1.0**, enmendada el 2026-10-08 precisamente para esta
feature.

| Principio | Puerta | Estado |
|---|---|---|
| **V** Secretos fuera de git | La CI no usa ni expone secretos | ✅ `API_KEY` ficticia por propiedad de Gradle (D3) |
| **VIII** Tests y CI en verde | Cobertura en trinquete | ✅ umbral desde la cifra real (D6) |
| **XI** Build en convention plugins | ktlint y Kover sin tocar módulos | ✅ plugin `convention.quality` (D4) |
| **XII** Paridad Android/iOS | iOS compila en la CI | ✅ y además se ejecutan los tests en iOS (D8) |
| **Flujo de desarrollo** | Jobs exigidos | ✅ los cinco; instrumentados condicionales por v1.1.0 |

**Resultado: PASA.** Sin violaciones; se omite Complexity Tracking.

Habría fallado contra la v1.0.0, que exigía una puerta del 20 % y tests instrumentados
obligatorios. Por eso la enmienda se tramitó antes de este plan y no después.

## Project Structure

### Documentation (this feature)

```text
specs/003-ci-pipeline/
├── plan.md              # Este fichero
├── research.md          # Fase 0 — 12 decisiones
├── data-model.md        # Fase 1 — comprobación, etapa, resultado
├── quickstart.md        # Fase 1 — guía de validación
├── contracts/
│   └── ci-workflow.md   # Fase 1 — disparadores, jobs, puertas
├── checklists/
│   └── requirements.md  # Calidad de la spec (16/16)
└── tasks.md             # Fase 2 — lo genera /speckit-tasks
```

### Source Code (repository root)

```text
.github/workflows/
└── ci.yml                                        # NUEVO: el workflow

build-logic/convention/
├── build.gradle.kts                              # MODIFICAR: registrar convention.quality
└── src/main/kotlin/
    ├── QualityConventionPlugin.kt                # NUEVO: ktlint + Kover (D4)
    ├── KmpLibraryConventionPlugin.kt             # MODIFICAR: aplicar convention.quality
    ├── CmpApplicationConventionPlugin.kt         # MODIFICAR: aplicar convention.quality
    └── BuildKonfigConventionPlugin.kt            # MODIFICAR: API_KEY por resolve() (D3)

gradle/libs.versions.toml                         # MODIFICAR: ktlint, Kover, convention.quality
gradle.properties                                 # MODIFICAR: granatum.coverage.minLine (D6)
build.gradle.kts                                  # MODIFICAR: Kover en la raíz para agregar (D7)
README.md                                         # MODIFICAR: sección de CI y protección de rama
```

**Structure Decision**: ningún `build.gradle.kts` de módulo se toca. Toda la configuración nueva
vive en `build-logic`, el catálogo, `gradle.properties` y el build raíz, que es el único sitio
desde el que se pueden agregar informes de todos los módulos.

## Fases de implementación

### Fase 1 — Que la CI pueda compilar

`API_KEY` por `resolve()` en `BuildKonfigConventionPlugin`. Verificación local: compilar
**sin** `local.properties` pasando `-PAPI_KEY=ci-placeholder`. Es el prerrequisito de todo lo
demás: sin él, ningún job pasa de la configuración.

### Fase 2 — ktlint y Kover en todos los módulos

Versiones en el catálogo, plugin `convention.quality`, aplicado desde los dos plugins base.
ktlint con `ignoreFailures = true`. Kover agregado en la raíz con la regla de verificación
leyendo `granatum.coverage.minLine`.

Verificar en esta fase que las versiones de D5 funcionan con Gradle 8.14.3 y si el informe
total de Kover incluye los tests unitarios de Android (D7).

### Fase 3 — Medir y fijar el trinquete

Ejecutar los tests y el informe de Kover en local, leer la cobertura de líneas, redondear
hacia abajo y escribirla en `gradle.properties`. Esperado: 0 %.

### Fase 4 — El workflow

`.github/workflows/ci.yml` con los cinco jobs de D2, concurrencia, permisos `contents: read`,
tiempos máximos, caché de Gradle y `~/.konan`, el conteo de tests de D9 y el resumen en
`$GITHUB_STEP_SUMMARY`.

### Fase 5 — Verificación real y documentación

Push de la rama y comprobación de la ejecución en GitHub Actions, incluida una prueba
negativa: un commit que rompa iOS debe dejar la CI en rojo. README con la sección de CI y el
paso de protección de rama para el administrador.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| ktlint-gradle 14.x o Kover 0.9.11 incompatibles con Gradle 8.14.3 | Verificar en la fase 2; volver a las versiones de Squadfy si hace falta |
| El informe total de Kover no incluye los tests de Android | Verificar en la fase 2; usar el informe de la variante `debug` |
| La primera ejecución en macOS es lenta por Kotlin/Native en frío | Caché de `~/.konan`; tiempo máximo de 45 minutos |
| ktlint destapa cientos de violaciones | Es informativo; se publica y no bloquea. Sanearlo es otra feature |
| Nadie activa la protección de rama y la CI queda como sugerencia | Documentado en README y quickstart como paso del administrador |
