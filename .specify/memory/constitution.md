# Constitución de Granatum Suite App

Granatum Suite App es una aplicación Kotlin Multiplatform (Android + iOS) con UI
en Compose Multiplatform. Esta constitución fija las reglas técnicas no
negociables. Toma como arquitectura de referencia el proyecto hermano
[Squadfy_KMM](https://github.com/kikepb7/Squadfy_KMM), con el que comparte linaje
(KMM-Skeleton), los mismos convention plugins y una capa `core/domain`
(`Result`, `DataError`, `Error`) idéntica salvo el package.

## Core Principles

### I. Arquitectura limpia por módulos

La estructura modular es `core/{domain, data, designsystem, presentation}` y
`feature/<nombre>/{domain, database, data, presentation}`, siguiendo el patrón de
`feature/example`. Toda feature nueva MUST replicar esa forma.

Las dependencias apuntan hacia dentro: `presentation → domain ← data`. Un módulo
`domain` MUST ser Kotlin puro y NO PUEDE depender de Android, Ktor, Room ni
Compose. Esa prohibición no es estilística: es lo que permite que el dominio se
teste en `commonTest` sin emulador y que iOS compile sin arrastrar el SDK de
Android.

Un módulo `data` depende de su `domain` y de `core/data`, nunca de otro
`feature`. La comunicación entre features pasa por `core`.

### II. commonMain por defecto

El código vive en `commonMain` salvo que exista una razón de plataforma. Escribir
en `androidMain` o `iosMain` lo que podría estar en `commonMain` es una
regresión.

`expect/actual` se reserva a cinco áreas: geolocalización, permisos,
almacenamiento seguro, conectividad y compartición de ficheros. Cualquier sexto
uso MUST justificarse en la PR.

Cuando un `actual` sea idéntico en Android e iOS, va en un source set
intermedio (`mobileMain`) declarado como grupo de la plantilla de jerarquía por
defecto. NO se permiten llamadas `dependsOn()` manuales: sacan a Kotlin de la
plantilla y rompen el cableado automático de los source sets.

### III. Offline-first

La UI lee siempre de Room; la red sincroniza por detrás. Ningún `ViewModel`
observa directamente una llamada HTTP.

El fichaje MUST funcionar sin conexión y **un fichaje nunca se pierde**. Toda
marca se escribe primero en local con estado de sincronización y se encola. El
envío respeta el orden del evento y para en el primer fallo, en lugar de saltarse
eventos: una secuencia de fichajes solo es válida en orden.

Cada evento lleva un identificador generado en cliente, y el backend MUST hacer
upsert por ese id. Sin idempotencia, un reintento tras una respuesta perdida
duplica la marca.

Los esquemas de Room se exportan y se commitean. Todo cambio de esquema viaja
con su migración en la misma PR; nunca se recurre a borrar la base en destino.

### IV. Errores explícitos y observabilidad

Los errores cruzan capas como `Result<D, E>` con `DataError`, nunca como
excepciones. Una excepción que escapa de `data` hacia `domain` o `presentation`
es un bug, no un mecanismo de control de flujo.

El mapeo a texto de usuario ocurre en `presentation` (`toUiText()`), jamás en
`domain`: el dominio no conoce idiomas ni pantallas.

Las trazas pasan por la abstracción `AppLogger` de `core/domain`. `println` y
`android.util.Log` están prohibidos en código de producción — rompen la
multiplataforma y escapan a cualquier control de nivel o redacción.

### V. Secretos y configuración por entorno

Secretos y URLs se inyectan vía BuildKonfig desde `local.properties`. NUNCA se
commitean. `local.properties.example` documenta las claves necesarias sin valores
reales.

Producción MUST ser HTTPS exclusivamente. Tráfico en claro solo se admite contra
`localhost`/emulador en builds de debug.

Las credenciales reales de Firebase (`google-services.json`,
`GoogleService-Info.plist`) quedan fuera del repositorio.

### VI. El contrato de API manda

El contrato es `docs/openapi.json` del backend. La app NO inventa endpoints,
campos ni códigos de estado.

Si una feature necesita algo que el contrato no ofrece, la PR se bloquea hasta
que el contrato se actualice. Adivinar la forma de la respuesta y "ya lo
arreglaremos" es lo que convierte un fallo de integración en un fallo en
producción.

### VII. Credenciales en almacenamiento seguro

Los tokens de sesión MUST residir en el almacén seguro del sistema: Keychain en
iOS, Keystore en Android. No en `DataStore` plano, no en `SharedPreferences`, no
en la base de datos.

El interfaz `SessionStorage` de `core/domain` es el único punto de acceso. Ningún
otro módulo lee ni escribe tokens directamente.

Lo que la regla prohíbe es el texto **legible**, no un medio concreto de
almacenamiento. En Android, guardar en `DataStore` el texto cifrado con una clave que
custodia el Keystore cumple el principio: la clave nunca sale del almacén seguro, y
sin ella lo almacenado no se puede leer.

### VIII. Tests y CI en verde

El dominio y los `ViewModel` se testean en `commonTest`, no en `androidUnitTest`:
un test que solo corre en JVM no protege iOS.

Nada se fusiona con la CI en rojo. Sin excepciones, sin "lo arreglo después del
merge".

Las puertas mínimas de la CI se definen en «Flujo de desarrollo y puertas de
calidad». La puerta de cobertura funciona como **trinquete**: ningún cambio puede
dejarla por debajo de su valor vigente. Subirla es libre y conviene hacerlo en cada
feature que añada tests; bajarla requiere enmienda a esta constitución, no una
decisión de PR.

Un umbral de cobertura que el proyecto no alcanza no protege nada: deja la CI en
rojo y convierte la regla anterior —nada se fusiona en rojo— en un bloqueo total.
Por eso la puerta parte de la cobertura real y sube desde ahí, en lugar de partir
de una cifra que el código todavía no cumple.

### IX. UI por design system, accesible y en español

Todo componente visual sale de `core/designsystem`. Colores, tipografías y
espaciados literales en pantallas de feature están prohibidos.

Los textos viven en recursos, con **español como idioma primario**. Nada de
cadenas incrustadas en composables.

Los objetivos táctiles respetan los mínimos de accesibilidad (48dp en Android,
44pt en iOS) y todo elemento interactivo expone semántica para lectores de
pantalla.

### X. Toda feature documentada

Cada feature queda documentada en `specs/` siguiendo el flujo de Spec Kit
(`spec.md` → `plan.md` → `tasks.md`) y se refleja en el README.

Código sin spec es deuda: nadie puede revisar contra una intención que no está
escrita.

### XI. El build vive en los convention plugins

Toda configuración de build reside en los convention plugins de `build-logic`:
`android-application`, `android-application-compose`, `cmp-application`,
`cmp-library`, `cmp-feature`, `kmp-library`, `room`, `buildkonfig`.

Los `build.gradle.kts` de módulo declaran dependencias y nada más. Configuración
Gradle ad-hoc en un módulo es una desviación que MUST corregirse subiéndola al
convention plugin correspondiente.

Las versiones salen exclusivamente de `gradle/libs.versions.toml`, fuente única
de verdad. Las versiones del toolchain —Kotlin, KSP, AGP, Compose— se mueven
juntas: un KSP desalineado con Kotlin rompió el build el 2026-10-04.

Higiene de deprecaciones: las que señalan una migración incompatible aguas arriba
se atienden, no se silencian. Un `typealias` deprecado hoy es un error de
compilación en la próxima subida de versión.

### XII. Paridad Android / iOS

Un cambio no está terminado hasta que compilan **ambos** targets. Verificar solo
la variante de Android no es verificar.

Toda PR que toque código compartido MUST acreditar `:composeApp:assembleDebug` y
la compilación de iOS (`compileKotlinIosSimulatorArm64` del módulo afectado).

Las roturas de iOS son la regresión más frecuente en KMP porque el ciclo de
desarrollo diario ocurre en Android. Esta regla existe precisamente por eso.

## Stack y restricciones técnicas

Targets: Android e iOS. No hay desktop ni web, y añadir uno requiere enmienda.

Package raíz: `com.granatum`.

Stack fijo:

| Área | Tecnología |
|---|---|
| DI | Koin |
| Red | Ktor |
| Persistencia | Room (en `commonMain`, driver bundled) |
| UI | Compose Multiplatform |
| Navegación | Navigation con rutas type-safe (nada de rutas por string) |
| Secretos | BuildKonfig |
| Fecha/hora | kotlinx-datetime + `kotlin.time` |
| Serialización | kotlinx-serialization |

Sustituir cualquiera de estas piezas es una enmienda MAJOR. Añadir una librería
nueva requiere justificarla en la PR frente a lo que ya existe en el stack.

JDK 17 es el mínimo para compilar.

## Flujo de desarrollo y puertas de calidad

La CI toma como modelo la de Squadfy_KMM, adaptada a la rama `main` y al estado real
de los tests de Granatum. Se ejecuta en cada PR contra `main` y en cada push a `main`.

Jobs:

1. **Análisis estático** — ktlint, en modo **informativo**: publica sus resultados
   pero no bloquea. El código es anterior a la herramienta, y hacerlo bloqueante de
   entrada obligaría a corregir todas las violaciones existentes de golpe. Pasa a
   bloqueante por enmienda una vez saneado el código.
2. **Tests unitarios + cobertura** — puerta de línea vía Kover en modo trinquete
   (principio VIII): se fija en la cobertura medida al activar la CI y solo sube.
3. **Assemble debug** — `:composeApp:assembleDebug`, publicando el APK como
   artefacto.
4. **Compilación de iOS** — `compileKotlinIosSimulatorArm64` en un runner de macOS
   (principio XII). Al ser el repositorio público, no tiene coste.
5. **Tests instrumentados** — **condicional**: no existe mientras no haya tests de
   dispositivo, y se incorpora en la misma PR que añada el primero. Su matriz de
   referencia es API 26, 30 y 34. Un job que arranca emuladores para no ejecutar nada
   da una falsa sensación de cobertura.
6. **Resumen** — job final que falla el run si cualquier job anterior falló o se
   canceló, para que un fallo no quede enterrado en la matriz.

Requisitos de PR:

- La rama parte de `main` y la CI está verde antes del merge.
- Las desviaciones de esta constitución se declaran explícitamente en la
  descripción de la PR, con su justificación.
- Un cambio de esquema de Room incluye migración y esquema exportado.
- Un cambio en código compartido acredita que iOS compila.

## Governance

Esta constitución prevalece sobre cualquier otra práctica o costumbre del
equipo. Ante conflicto entre este documento y un hábito establecido, manda el
documento.

**Procedimiento de enmienda.** Toda modificación se propone en una PR que toca
este fichero, explica el motivo y actualiza la versión. Una enmienda que invalide
código existente MUST incluir el plan de migración.

**Versionado.** Semántico:

- **MAJOR** — se elimina o redefine un principio de forma incompatible.
- **MINOR** — se añade un principio o se amplía materialmente una guía.
- **PATCH** — aclaraciones, redacción, correcciones sin cambio semántico.

**Revisión de cumplimiento.** Cada PR verifica el cumplimiento. La complejidad
que se aparte de estos principios se justifica o se revierte.

### Brechas conocidas

Este documento describe el estado objetivo. Las reglas que todavía no se cumplen se
listan de forma explícita para que nadie las dé por satisfechas.

| # | Brecha | Principio | Estado |
|---|---|---|---|
| 1 | No existía pipeline de CI (`.github/workflows` ausente) | VIII | **Cerrada** por `003-ci-pipeline` |
| 2 | ktlint y Kover no estaban configurados en el build | VIII | **Cerrada** por `003-ci-pipeline` |
| 3 | Los tokens se guardaban en claro en `DataStoreSessionStorage` | VII | **Cerrada** por `002-secure-session-storage` |
| 4 | `docs/openapi.json` no existe; `docs/` está vacío | VI | Abierta — bloquea las specs de inventario y panel de encargado |
| 5 | `specs/` no existía | X | **Cerrada** — existen 001, 002 y 003 |

Las brechas 1 y 2 se cerraron con un workflow de GitHub Actions que compila Android
e iOS, ejecuta los tests en JVM y en el simulador de iOS, mide la cobertura con el
trinquete de `granatum.coverage.minLine` y publica ktlint en modo informativo. El
principio VIII es exigible desde entonces. Que la CI sea una puerta y no una
sugerencia depende de la protección de la rama `main`, que configura quien
administra el repositorio.

La brecha 3 se cerró con las credenciales en el Android Keystore y el iOS Keychain,
verificado en dispositivo, incluida la purga del Keychain al reinstalar. Fue una
mejora sobre el proyecto de referencia, no una paridad: Squadfy_KMM tampoco usa
almacén seguro.

**Version**: 1.1.1 | **Ratified**: 2026-10-04 | **Last Amended**: 2026-10-08
