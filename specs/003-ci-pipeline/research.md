# Phase 0 — Investigación: pipeline de integración continua

**Feature**: `003-ci-pipeline` · **Fecha**: 2026-10-08

Verificado contra el repositorio, el portal de plugins de Gradle y la CI de Squadfy_KMM.

## D1 — Plataforma y disparadores

**Decisión**: GitHub Actions, con un único workflow `.github/workflows/ci.yml`. Se dispara en
`pull_request` contra `main`, en `push` a `main` y manualmente con `workflow_dispatch`.
Concurrencia por rama con `cancel-in-progress: true` (FR-013).

**Justificación**: el repositorio vive en GitHub y es **público**, así que los minutos son
gratuitos, incluidos los runners de macOS que exige iOS. Es la misma plataforma que la CI de
referencia de Squadfy_KMM.

**Alternativa descartada**: varios workflows, uno por plataforma. Complica tener un resultado
global único (FR-010) y obliga a coordinar estados entre ficheros.

## D2 — Jobs

**Decisión**: cinco jobs, ninguno de tests instrumentados.

| Job | Runner | Bloquea | Requisito |
|---|---|---|---|
| Análisis estático | ubuntu | no (informativo) | FR-006 |
| Tests + cobertura | ubuntu | sí | FR-005, FR-007, FR-008, FR-017 |
| Build Android | ubuntu | sí | FR-003, FR-012 |
| Build + tests iOS | macOS | sí | FR-004 |
| Resumen | ubuntu | sí, falla si algo falla o se cancela | FR-010, FR-011 |

**Justificación**: sigue la sección «Flujo de desarrollo» de la constitución v1.1.0. El job
de tests instrumentados no existe por FR-009: no hay ningún test de dispositivo.

## D3 — `API_KEY` sin `local.properties`

**Decisión**: `BuildKonfigConventionPlugin` lee `API_KEY` mediante la función `resolve()` que
ya usa para `BASE_URL_HTTP` —propiedad de Gradle, luego `local.properties`—, y conserva el
fallo rápido si no aparece por ninguna vía. La CI la pasa como `-PAPI_KEY=ci-placeholder`.

**Justificación**: hoy el plugin la lee **solo** de `local.properties` y lanza
`IllegalStateException` si falta. Ese fichero está gitignorado y no existe en la CI, así que
el build no pasaría la configuración. Es un cambio de una línea, alinea `API_KEY` con el resto
de claves del mismo fichero, y permite inyectar valores desde CI sin escribir ficheros
(FR-014). Para el desarrollo local no cambia nada: `local.properties` sigue funcionando igual.

**Alternativa descartada**: generar un `local.properties` en cada job. Funciona, pero añade un
paso con ficheros temporales en cada job y deja `API_KEY` como la única clave que no admite
propiedad de Gradle.

## D4 — ktlint y Kover en un convention plugin nuevo

**Decisión**: un plugin `convention.quality` en `build-logic` que aplica ktlint y Kover. Lo
aplican a su vez `KmpLibraryConventionPlugin` y `CmpApplicationConventionPlugin`, de modo que
**todos** los módulos lo reciben sin tocar ningún `build.gradle.kts` de módulo.

**Justificación**: el principio XI prohíbe configuración ad hoc en los módulos. Squadfy lo
aplica directamente en `composeApp/build.gradle.kts` y solo ahí, lo que en Granatum dejaría
fuera los 12 módulos restantes y contradiría la constitución.

**Alternativa descartada**: añadir ktlint y Kover a cada plugin existente. Duplica la
configuración en varios sitios que tendrían que mantenerse sincronizados.

## D5 — Versiones

**Decisión**: **ktlint-gradle 14.2.0** y **Kover 0.9.11**, las últimas publicadas en el portal
de plugins de Gradle a 2026-10-08, declaradas en `gradle/libs.versions.toml`.

**Justificación**: Squadfy usa 12.1.1 y 0.9.1. ktlint-gradle 12.x trae un motor de ktlint
antiguo con riesgo de no entender sintaxis de Kotlin 2.2. Se verifica en la primera tarea de
implementación; si alguna fuera incompatible con Gradle 8.14.3, se baja a la versión de
Squadfy, que sí está probada en un proyecto KMP equivalente.

## D6 — El trinquete de cobertura

**Decisión**: el umbral mínimo de cobertura de líneas vive en **una sola línea** de
`gradle.properties` (`granatum.coverage.minLine`) y el convention plugin lo convierte en la
regla de verificación de Kover. Su valor inicial es la cobertura medida al activar la CI,
**redondeada hacia abajo** al entero.

**Justificación**: un único sitio, visible en el diff de cualquier PR que lo toque. Subirlo es
cambiar un número; bajarlo deja rastro evidente y, por el principio VIII, exige enmienda.
Redondear hacia abajo evita que una oscilación de décimas tumbe la CI sin cambio real.

**Valor esperado**: **0 %**. El único test es el placeholder de plantilla, que no ejecuta
código de producción. Es honesto: el trinquete arranca donde está el proyecto, y la primera
feature que añada tests reales podrá subirlo.

**Alternativa descartada**: fijarlo dentro del convention plugin. Es la cifra que más va a
cambiar del build, y esconderla en código Kotlin dificulta subirla.

## D7 — Agregación de cobertura

**Decisión**: Kover se aplica en todos los módulos (D4) y el proyecto raíz agrega sus
informes. La verificación del trinquete corre sobre el informe agregado.

**Justificación**: la cobertura de un solo módulo engaña; lo que el trinquete debe vigilar es
el proyecto entero.

**Verificado en implementación (2026-10-08)**: el informe total (`koverXmlReport`) **sí**
incluye los tests unitarios de Android de los 13 módulos, pero arrastra **las dos variantes**,
`testDebugUnitTest` y `testReleaseUnitTest`. Es el mismo `commonTest` compilado dos veces:
duplica el tiempo de la etapa sin aportar ni una línea de cobertura.

Se intentó agregar solo la variante `debug` creando una variante `debug` en la raíz para
fusionarla con la de cada módulo. **Falla**: `:composeApp` es un módulo de *aplicación* Android
y su variante no se resuelve contra una variante personalizada de la raíz. Se mantiene por tanto
la variante **total** (`koverXmlReport`, `koverVerify`), que agrega los 13 módulos —81 paquetes
en el informe— aceptando el doble de ejecuciones. Con un único test el coste es nulo; queda como
deuda que revisar cuando haya tests reales.

**Cobertura medida al activar la CI**: líneas **0 / 4.843 = 0,00 %**.

**Agregación en la raíz, no en un convention plugin**: aplicar un plugin de `build-logic` en el
proyecto raíz mete todo `build-logic` en el classpath raíz, y entonces la petición
`alias(libs.plugins.convention.*)` de cada módulo falla con *«already on the classpath with an
unknown version»*. Por eso ktlint y Kover son `compileOnly` en `build-logic` y la raíz los
carga con `apply false` —el mismo arreglo que el build ya usa para AGP y KGP— y la agregación y
el trinquete viven en el `build.gradle.kts` raíz.

## D8 — iOS: compilar todo y ejecutar los tests en iOS

**Decisión**: el job de macOS ejecuta `:composeApp:linkDebugFrameworkIosSimulatorArm64` y
`iosSimulatorArm64Test`.

**Justificación**: `:core:data:compileKotlinIosSimulatorArm64`, que es lo que se ha usado a
mano en las specs 1 y 2, solo compila un módulo. Enlazar el framework de `composeApp` compila
**todos** los módulos compartidos de los que depende, y además detecta errores de enlazado y
`actual` ausentes entre módulos, que la compilación aislada no ve.

`iosSimulatorArm64Test` ejecuta los `commonTest` **en iOS**, que es exactamente lo que pide
el principio VIII: un test que solo corre en JVM no protege iOS. Como el runner de macOS ya
está levantado, el coste añadido es mínimo.

**Caché**: Gradle mediante `gradle/actions/setup-gradle@v4`, y el toolchain de Kotlin/Native
cacheando `~/.konan`, que se descarga entero en cada ejecución si no.

## D9 — «No hay tests» no es «pasan los tests»

**Decisión**: tras ejecutar los tests, un paso cuenta los casos en los informes JUnit XML y
**falla si son cero** (FR-017).

**Justificación**: Gradle 8.14.3 da por buena una tarea de test que no encuentra nada que
ejecutar. Sin esta comprobación, borrar el único test dejaría la CI en verde.

## D10 — Seguridad en un repositorio público

**Decisión**: evento `pull_request`, nunca `pull_request_target`. Permisos del workflow
reducidos a `contents: read`. Ningún secreto.

**Justificación**: cualquiera puede abrir una PR desde un fork. Con `pull_request`, el código
ajeno corre sin acceso a secretos y con un token de solo lectura. Como la CI no necesita
ningún secreto (D3), no hay nada que exponer (FR-015, SC-006).

**Consecuencia**: no se usa `EnricoMi/publish-unit-test-result-action`, que Squadfy sí usa,
porque necesita permisos de escritura que un fork no tiene. Los resultados van al resumen
del job mediante `$GITHUB_STEP_SUMMARY`, que funciona en cualquier PR, y los informes completos
como artefactos.

## D11 — Tiempos máximos

**Decisión**: análisis estático 15 min, tests 20, Android 20, iOS 45, resumen 5 (FR-016).

**Justificación**: iOS es el más lento por la descarga y compilación de Kotlin/Native en frío.
El conjunto cabe en los 30 minutos de SC-004 en el caso habitual, con caché.

## D12 — La protección de `main` no es de esta feature

**Decisión**: la feature deja el job de resumen listo para usarse como comprobación
obligatoria, pero **no** activa la protección de rama.

**Justificación**: exigir que la CI pase antes de fusionar se configura en los ajustes del
repositorio y requiere permisos de administración. El quickstart documenta el paso para que lo
haga quien administra el repositorio.

## D13 — ktlint no debe compilar nada (corrección tras la primera ejecución real)

**Hallazgo (2026-10-08)**: la primera ejecución en `main` terminó en rojo por el job de
ktlint, con **cero** errores de estilo implicados. Reproducido en un contenedor Linux x86_64:
`ktlintCheck` ejecutaba KSP y las compilaciones de iOS de los módulos `database`. KSP registra
sus directorios de salida como fuentes *junto con* la tarea que los produce; el filtro de
ktlint descarta esos ficheros, pero no la dependencia de la tarea. En un runner Linux KSP no
genera el `actual` de Room para iOS, y la compilación falla con *«Expected
AppClockInDatabaseConstructor has no actual declaration»*. En macOS compila, por eso en local
pasaba.

**Decisión**: `QualityConventionPlugin` apunta cada tarea de ktlint a los directorios escritos a
mano de su source set (como ficheros simples, sin `build/`), en un `afterEvaluate` para que se
aplique después de la configuración del propio ktlint-gradle, que si no la vuelve a pisar.

**Verificado**: `ktlintCheck --dry-run` ya no incluye ninguna tarea `compile*` ni `ksp*`; el
recuento base sigue siendo exactamente 1.464 violaciones y ningún fichero de `build/` aparece en
los informes; en el contenedor Linux el job termina en verde. Efecto secundario: ktlint deja de
compilar y es mucho más rápido.

## D14 — Actions en Node 24

**Hallazgo**: GitHub avisa de que Node 20 está obsoleto en los runners y de que
`actions/setup-java@v4` ya no recibe actualizaciones.

**Decisión**: `actions/checkout@v5`, `actions/setup-java@v5`, `actions/upload-artifact@v6`,
`actions/cache@v5` y `gradle/actions/setup-gradle@v5`. Son las primeras majors que declaran
`node24` en su `action.yml` (comprobado), con meses de publicación; no se salta a las más
recientes para no mezclar cambios de comportamiento con esta corrección.

## Resumen

Ninguna incógnita queda abierta. Las dos decisiones de gobernanza se cerraron en la spec y la
constitución se enmendó a v1.1.0 antes de este plan.
