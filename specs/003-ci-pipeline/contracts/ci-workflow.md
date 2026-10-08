# Contrato — Workflow de CI

**Feature**: `003-ci-pipeline` · **Fecha**: 2026-10-08

Lo que el workflow promete a quien abre o revisa una PR.

## 1. Disparadores

| Evento | Rama | Notas |
|---|---|---|
| `pull_request` | contra `main` | también desde forks; **nunca** `pull_request_target` |
| `push` | `main` | verifica el resultado de cada fusión |
| `workflow_dispatch` | cualquiera | lanzamiento manual |

Concurrencia: `group: ci-${{ github.ref }}`, `cancel-in-progress: true`.

## 2. Permisos y secretos

```yaml
permissions:
  contents: read
```

Ningún secreto. `API_KEY` se pasa como `-PAPI_KEY=ci-placeholder`, un valor ficticio: el build
lo exige para configurar, pero ningún test lo usa contra un servidor real.

## 3. Jobs

| Job | Runner | Comandos | Timeout | Artefactos |
|---|---|---|---|---|
| `static-analysis` | ubuntu-latest | `ktlintCheck` | 15 min | informes de ktlint |
| `unit-tests` | ubuntu-latest | `testDebugUnitTest` + informe y verificación de Kover | 20 min | informes de tests y cobertura |
| `build-android` | ubuntu-latest | `:composeApp:assembleDebug` | 20 min | `composeApp-debug.apk` |
| `build-ios` | macos-latest | `:composeApp:linkDebugFrameworkIosSimulatorArm64`, `iosSimulatorArm64Test` | 45 min | informes de tests de iOS |
| `summary` | ubuntu-latest | evalúa `needs.*.result` | 5 min | — |

Todos con JDK 17 (temurin) y `gradle/actions/setup-gradle@v5` (D14). `build-ios` cachea además
`~/.konan`.

## 4. Puertas

| Puerta | Falla cuando… |
|---|---|
| Tests | algún test falla |
| Tests ejecutados | el número de casos en los informes JUnit es **cero** (FR-017) |
| Cobertura | la cobertura de líneas agregada queda por debajo de `granatum.coverage.minLine` |
| Android | el APK no se genera |
| iOS | el framework no enlaza o falla algún test en el simulador |
| Resumen | cualquier job bloqueante termina en `failure` o `cancelled` |

ktlint **no** es puerta: su job publica resultados y siempre termina en verde. Tampoco compila
nada: analiza solo las fuentes escritas a mano (D13).

## 5. Salida visible

El job `summary` escribe en `$GITHUB_STEP_SUMMARY` una tabla con el estado de cada job, el
número de tests ejecutados y el porcentaje de cobertura. Funciona también en PRs desde forks,
que no tienen permiso para publicar comentarios ni *checks*.

## 6. Lo que este contrato NO incluye

- **Tests instrumentados**: el job no existe hasta que haya el primer test de dispositivo
  (constitución v1.1.0, FR-009).
- **Protección de rama**: la configura el administrador del repositorio (ver quickstart).
- **Builds de release, firma y despliegue**: fuera de alcance.
