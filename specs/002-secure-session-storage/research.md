# Phase 0 — Investigación: sesión en almacén seguro

**Feature**: `002-secure-session-storage` · **Fecha**: 2026-10-05

Verificado contra el código y contra la documentación de Android, no inferido.

## D1 — Android: Keystore directo, no EncryptedSharedPreferences

**Decisión**: cifrar con una clave AES-GCM generada y custodiada por el **Android Keystore**, y
guardar el texto cifrado en el DataStore que el módulo ya usa. No se añade
`androidx.security:security-crypto`.

**Justificación**: esa librería **está deprecada**. Las notas de versión de Jetpack lo dicen
desde 1.1.0-beta01 (4 de junio de 2025): *«Deprecated all APIs in favour of existing platform
APIs and direct use of Android Keystore»*. La 1.1.0 estable de julio de 2025 sale ya con todo
deprecado. Incorporarla ahora sería añadir una dependencia muerta y contradecir la higiene de
deprecaciones del principio XI, que esta misma sesión aplicó al migrar `kotlinx.datetime.Clock`.

Reaprovechar DataStore tiene además dos ventajas concretas: no añade ninguna dependencia —ya
está en el catálogo (1.1.7) y cableado en `platformCoreDataModule`— y conserva el `Flow` que
`observeAuthInfo()` necesita. Lo único que cambia es que lo escrito deja de ser legible.

**Alternativas descartadas**:

- *`EncryptedSharedPreferences`*: deprecada, y además sin API reactiva, lo que obligaría a
  inventar la observación por separado.
- *Un fichero cifrado propio*: reimplementa lo que DataStore ya resuelve, incluida la
  atomicidad de la escritura.

## D2 — iOS: Keychain vía Security.framework

**Decisión**: acceso directo al Keychain a través de la interoperabilidad de Kotlin/Native con
`Security.framework` (`SecItemAdd`, `SecItemCopyMatching`, `SecItemUpdate`, `SecItemDelete`),
con la clase de accesibilidad `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.

**Justificación**: es el almacén que el sistema ofrece y el que el principio VII nombra.
`ThisDeviceOnly` evita que la credencial viaje en una copia de seguridad a otro dispositivo, y
`AfterFirstUnlock` permite que la sincronización en segundo plano del fichaje —spec 4— pueda
leer el token con la pantalla bloqueada, que es justo lo que una app de fichaje necesita.

**Alternativas descartadas**:

- *`kSecAttrAccessibleWhenUnlocked`*: más estricto, pero rompería la sincronización en segundo
  plano que la spec 4 dará por supuesta.
- *Una librería KMP de terceros para Keychain*: añade dependencia y capa de indirección para
  cuatro llamadas de sistema.

## D3 — El Keychain sobrevive a la desinstalación

**Decisión**: en el primer arranque tras una instalación, **purgar** la entrada del Keychain
antes de leerla. La detección se apoya en un marcador guardado en el almacenamiento ordinario
de la app, que el sistema **sí** borra al desinstalar: si el marcador no está pero la entrada
del Keychain sí, estamos ante una instalación nueva sobre restos de la anterior.

**Justificación**: en iOS los elementos del Keychain **persisten tras eliminar la aplicación**.
Sin esto, FR-005 y el caso límite de desinstalación quedarían incumplidos en iOS aunque el
código pareciese correcto: quien reinstalase heredaría la sesión de quien usara el dispositivo
antes. Es el tipo de fallo que no aparece en ninguna prueba normal.

**Alternativas descartadas**:

- *Ignorarlo*: incumple un requisito explícito.
- *Marcar la entrada como no persistente*: el Keychain no ofrece esa semántica.

## D4 — Una sola implementación común sobre un almacén por plataforma

**Decisión**: `SessionStorage` tiene **una única implementación en `commonMain`**. Debajo, un
`expect` de almacén seguro clave-valor con tres operaciones —leer, escribir y borrar una
cadena— con su `actual` por plataforma. La serialización a JSON, el mapeo a dominio y la
exposición del `Flow` viven una sola vez, en común.

**Justificación**: la parte que de verdad difiere entre plataformas son tres llamadas de
sistema. Duplicar `SessionStorage` entero significaría mantener dos veces la serialización, el
mapeo y el manejo de errores, con el riesgo de que diverjan. El principio II pide que el código
viva en `commonMain` por defecto y reserva `expect/actual` a lo estrictamente de plataforma:
esto lo cumple al pie de la letra.

**Alternativas descartadas**:

- *`expect class SessionStorageImpl` completa por plataforma*: máxima libertad, máxima
  duplicación.
- *Un `expect` de cifrador y almacenamiento común*: encaja en Android, pero no en iOS, donde el
  Keychain custodia el dato entero y no expone un cifrador.

## D5 — Observación sin notificaciones de plataforma

**Decisión**: la implementación común mantiene un `MutableStateFlow`, lo siembra con la primera
lectura del almacén y lo actualiza en cada escritura. `observeAuthInfo()` expone ese flujo.

**Justificación**: el Keychain **no notifica cambios**, así que no se puede construir un `Flow`
reactivo directamente sobre él como sí se hace hoy con `dataStore.data`. La app es un único
proceso con un único escritor de sesión, de modo que el estado en memoria no puede divergir del
almacén. Y evita leer del Keychain en cada recomposición, que es caro.

**Consecuencia**: si algún día otro proceso escribiera la sesión —una extensión, un widget—,
esta suposición dejaría de valer. Queda anotado.

## D6 — Los fallos no cruzan capas

**Decisión**: la implementación **captura** cualquier fallo del almacén, lo registra con
`AppLogger` y lo trata como ausencia de sesión. No se propaga ninguna excepción y el contrato
de `SessionStorage` no cambia.

**Justificación**: el principio IV prohíbe que las excepciones crucen capas, y FR-003 prohíbe
tocar el contrato. `SessionStorage` no devuelve `Result` —es `Flow<AuthInfoModel?>` y
`suspend fun set(...): Unit`—, así que el único punto donde un fallo puede manejarse sin
cambiar la firma es dentro. Encaja además con FR-006 y FR-007: almacén no disponible o
credencial corrupta se comportan igual, como «no hay sesión», y la app arranca.

El mensaje va por `AppLogger`, nunca por `println` ni `android.util.Log`, según el principio IV.

## D7 — Descartar la sesión heredada en claro

**Decisión**: al arrancar, borrar incondicionalmente la clave `KEY_AUTH_INFO` del DataStore en
claro. Una sola vez, sin leer su contenido.

**Justificación**: FR-008 ya está decidido —se descarta, no se migra— y el criterio es que una
credencial que estuvo en texto legible deja de ser fiable. No leerla siquiera evita
reintroducir el valor en memoria. La clave antigua y la nueva deben ser **distintas**, para que
el borrado no se lleve por delante lo recién escrito.

## D8 — Sembrar sesiones de prueba sin tocar producción

**Decisión**: un sembrador de sesión que solo existe cuando `BuildKonfig.ENVIRONMENT` es
`local`. Toma el rol de un campo de BuildKonfig —vacío por defecto—, pide un token al extremo
de desarrollo del backend y escribe la sesión resultante. Se activa al construir:
`-Pbuildkonfig.flavor=local -PDEV_SESSION_ROLE=EMPLEADO`.

**Justificación**: no añade nada a ninguna pantalla, es el mismo patrón de compuerta que ya usa
`BackendHealthProbe` de la spec 1, y en cualquier build que no sea `local` el campo no existe y
el sembrador no se registra. Verificar los tres roles cuesta tres compilaciones, que para un
andamio de verificación es asumible.

**Alternativas descartadas**:

- *Un enlace profundo de desarrollo*: más cómodo —un comando por rol, sin recompilar—, pero el
  esquema de URI del proyecto sigue siendo el del esqueleto (`yourapp`), así que habría que
  arreglarlo primero y eso es alcance de otra feature.
- *Escribir la sesión desde fuera con herramientas del sistema*: imposible por diseño, que es
  precisamente el objetivo de esta feature.
- *Una pantalla oculta de desarrollo*: la spec lo prohíbe explícitamente.

## D9 — Inventario de consumidores de la sesión

**Decisión**: los consumidores no se tocan.

**Justificación**: se rastreó `SessionStorage` en todo el repositorio. Lo consumen
`HttpClientFactory` —autenticación Bearer y renovación— y `NavigationRoot` —rol para decidir
pestañas—, más el registro en Koin de `CoreDataModule`. Los tres dependen solo del interfaz,
que no cambia, así que el cambio queda confinado a `core/data`. Es la misma razón por la que en
la spec 1 conservar `UrlConstants` como envoltorio evitó tocar `HttpClientExt`.

## Resumen

Ninguna incógnita queda abierta. La decisión de producto sobre la sesión heredada se cerró en
la spec antes de llegar aquí.
