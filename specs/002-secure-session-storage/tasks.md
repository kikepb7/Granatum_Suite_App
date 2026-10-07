# Tasks: Sesión en almacén seguro y navegación por rol

**Input**: Documentos de diseño en `/specs/002-secure-session-storage/`

**Prerequisites**: plan.md, spec.md, research.md (D1-D9), data-model.md, contracts/secure-store.md, quickstart.md

**Tests**: Sin tareas de test automatizado. La spec no los pidió, y lo que esta feature cambia —que un dato acabe en el Keychain o cifrado por el Keystore— no se puede comprobar en `commonTest`: exige dispositivo. La verificación es manual y está en la fase final.

> **Compilar aquí no demuestra nada.** En la spec 1 un `BUILD SUCCESSFUL` cubría bastante terreno. En esta es perfectamente compatible con un almacén que no guarda, un Keychain que devuelve siempre vacío o un cifrado que nunca se aplica. Las dos compilaciones siguen siendo obligatorias por el principio XII, pero **no sustituyen a ejecutar en dispositivo**. Y no hay CI que lo atrape (brechas 1 y 2).

## Estado: 35/39 completadas (2026-10-05)

**La brecha 3 de la constitución queda cerrada.** Verificado en dispositivo, no solo compilado:

- **Android**: el DataStore contiene la clave `granatum.session.secure.v1` con un blob Base64
  cifrado. Se sembró la sesión con tokens centinela y **ninguna coincidencia** aparece en
  todo el almacenamiento de la app. La clave heredada `KEY_AUTH_INFO` ya no existe.
- **Persistencia**: con el backend caído —de modo que el sembrador falla— la app reinicia y
  conserva la sesión. Vino del almacén cifrado, no de una resiembra.
- **iOS**: misma sesión alcanzable, y **la purga de reinstalación funciona**: desinstalar y
  reinstalar con el backend caído deja la app sin sesión. Sin T008 el Keychain habría
  sobrevivido y nadie se habría enterado.
- **Los tres roles**: EMPLEADO ve 2 pestañas; ENCARGADO y ADMIN ven 4, con inventario y
  equipo. Sin sesión se aplica el repliegue restringido.

Dos defectos propios corregidos sobre la marcha, ambos en el almacén de iOS: `read()` no
capturaba excepciones pese a que el contrato lo exige —una excepción mataba la cadena de
arranque sin tumbar la app, de forma invisible—, y los diccionarios de consulta se creaban
con callbacks nulos, lo que hace que CoreFoundation compare claves por identidad de puntero
en vez de por `CFEqual` y las constantes de Security dejen de casar.

La verificación usó un stub HTTP que responde al health check y al extremo de desarrollo de
tokens, porque el backend real no tiene módulo de autenticación. Eso prueba la cadena de
almacenamiento y el reparto por rol; **no prueba nada sobre la API real**.

Quedan 4 tareas:

| Tarea | Por qué |
|---|---|
| T032 | Exige instalar una build anterior a esta feature para el ciclo de actualización. El borrado de la clave heredada sí está verificado: ya no aparece en el almacenamiento |
| T033 | Almacén no disponible: provocarlo exige manipular el bloqueo de pantalla del dispositivo o forzar el error en el código |
| T034 | Credencial corrupta: exige escribir basura dentro del almacén cifrado |
| T039 | Depende de T032 a T034 |

## Format: `[ID] [P?] [Story] Description`

- **[P]**: paralelizable (fichero distinto, sin dependencias pendientes)
- **[US#]**: historia de usuario a la que pertenece

## Path Conventions

Rutas relativas a la raíz. Paquete `com.granatum`. Los caminos largos de Kotlin se abrevian con `…` donde no hay ambigüedad.

---

## Phase 1: Setup

**Purpose**: línea base verificada antes de tocar nada. Sin CI, es la única forma de saber que una rotura posterior la hemos causado nosotros.

- [X] T001 Verificar la línea base en Android con `./gradlew :composeApp:assembleDebug` y anotar el resultado
- [X] T002 [P] Verificar la línea base en iOS con `./gradlew :core:data:compileKotlinIosSimulatorArm64` y anotar el resultado
- [X] T003 Inventariar los consumidores de `SessionStorage` en todo el repositorio y confirmar que son solo `core/data/src/commonMain/.../networking/HttpClientFactory.kt`, `composeApp/src/commonMain/.../navigation/NavigationRoot.kt` y el registro de Koin en `core/data/src/commonMain/.../di/CoreDataModule.kt` (D9). Si aparece alguno más, anotarlo antes de seguir

**Checkpoint**: línea base conocida y superficie de cambio confirmada.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: el contrato del almacén, del que dependen todas las historias.

**⚠️ CRITICAL**: US1 y US3 no pueden empezar hasta que esto exista.

- [X] T004 Definir el `expect` del almacén seguro en `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/storage/SecureStore.kt` con **exactamente tres operaciones**: leer una cadena por clave devolviendo `null` cuando no hay valor **o cuando falla**, escribir una cadena sustituyendo lo que hubiera de forma atómica, y borrar por clave de forma **idempotente** —borrar lo que no existe no es un error— (D4, contracts/secure-store.md §2)
- [X] T005 Definir en el mismo fichero la constante de clave de la sesión, **distinta de `"KEY_AUTH_INFO"`**, que es la clave heredada en claro. Si coincidieran, el limpiador de US3 borraría la sesión recién escrita (D7)

**Checkpoint**: contrato listo. Las implementaciones de plataforma pueden escribirse en paralelo.

---

## Phase 3: User Story 1 - Las credenciales no quedan a la vista (Priority: P1) 🎯 MVP

**Goal**: que las credenciales dejen de estar en texto plano y pasen al almacén protegido de cada plataforma, sin que ningún consumidor se entere.

**Independent Test**: inspeccionar el almacenamiento de la app en ambas plataformas tras guardar una sesión y no encontrar ningún valor legible, comprobando además que la sesión sobrevive al reinicio.

### Implementation for User Story 1

- [X] T006 [P] [US1] Implementar el `actual` de Android en `core/data/src/androidMain/kotlin/com/granatum/core/data/auth/storage/SecureStore.android.kt`: clave AES-GCM generada y custodiada por el **Android Keystore**, texto cifrado escrito en el `DataStore<Preferences>` que ya registra `platformCoreDataModule`. **NO añadir `androidx.security:security-crypto`**: está deprecada desde 1.1.0-beta01 (junio 2025) con todas sus APIs marcadas a favor de usar el Keystore directamente, y añadirla contradiría el principio XI (D1). Cero dependencias nuevas
- [X] T007 [P] [US1] Implementar el `actual` de iOS en `core/data/src/iosMain/kotlin/com/granatum/core/data/auth/storage/SecureStore.ios.kt`: Keychain vía interoperabilidad con `Security.framework` (`SecItemAdd`, `SecItemCopyMatching`, `SecItemUpdate`, `SecItemDelete`) con accesibilidad `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. `AfterFirstUnlock` permite que la sincronización en segundo plano de la spec 4 lea el token con la pantalla bloqueada; `ThisDeviceOnly` impide que viaje en una copia de seguridad (D2)
- [X] T008 [US1] 🔥 **Purga de reinstalación en iOS**, en el mismo `SecureStore.ios.kt`: en el primer arranque tras una instalación, borrar la entrada del Keychain **antes** de leerla. Detectarlo con un marcador en el almacenamiento ordinario de la app, que el sistema **sí** borra al desinstalar: marcador ausente + entrada presente ⇒ instalación nueva sobre restos de la anterior (D3). **En iOS el Keychain sobrevive a la desinstalación**, así que sin esto quien reinstale hereda la sesión de quien usara el dispositivo antes. Es el único defecto de la feature que no se manifiesta en ninguna otra prueba
- [X] T009 [US1] Implementar `SecureSessionStorage` en `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/storage/SecureSessionStorage.kt` como **única** implementación de `SessionStorage` (D4): serializar con el `AuthInfoSerializableDTO` y los mappers `toDto`/`toDomain` que ya existen, reutilizando el `Json { ignoreUnknownKeys = true }` del actual `DataStoreSessionStorage`
- [X] T010 [US1] En ese mismo fichero, exponer `observeAuthInfo()` sobre un `MutableStateFlow` sembrado con la primera lectura del almacén y actualizado en cada escritura (D5). El Keychain **no notifica cambios**, así que no cabe construir un `Flow` reactivo sobre él como se hace hoy con `dataStore.data`
- [X] T011 [US1] En ese mismo fichero, capturar **todos** los fallos del almacén, registrarlos con `AppLogger` de `core/domain` y tratarlos como ausencia de sesión (D6, reglas V5 y V6). Prohibido `println` y `android.util.Log` (principio IV). Ninguna excepción puede salir hacia `core/domain` ni hacia presentación
- [X] T012 [US1] Cambiar el binding en `core/data/src/commonMain/kotlin/com/granatum/core/data/di/CoreDataModule.kt` de `DataStoreSessionStorage` a `SecureSessionStorage`, y registrar el `actual` del almacén en `core/data/src/androidMain/.../di/CoreDataModule.android.kt` y en `core/data/src/iosMain/.../di/CoreDataModule.ios.kt`
- [X] T013 [US1] Eliminar `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/storage/DataStoreSessionStorage.kt`
- [X] T014 [US1] Verificar que **no se han modificado** `core/domain/src/commonMain/.../auth/repository/SessionStorage.kt` (FR-003), `core/data/src/commonMain/.../networking/HttpClientFactory.kt` ni `composeApp/src/commonMain/.../navigation/NavigationRoot.kt`. Si ha habido que tocar alguno, el diseño está mal y hay que revisarlo antes de continuar
- [X] T015 [US1] Compilar ambos targets: `./gradlew :composeApp:assembleDebug` y `./gradlew :core:data:compileKotlinIosSimulatorArm64`
- [X] T016 [US1] Verificar en **Android**, con una sesión guardada, que el almacenamiento de la app no contiene ningún fragmento legible de la credencial, y que ya no queda rastro del DataStore antiguo con `KEY_AUTH_INFO` en claro (quickstart escenario 1)
- [X] T017 [US1] Verificar en **iOS**, con una sesión guardada, que el contenedor de datos de la app no contiene la credencial en texto plano (quickstart escenario 2)
- [X] T018 [US1] Verificar en ambas plataformas que la sesión sobrevive al cierre completo y reapertura de la app (quickstart escenario 3)
- [X] T019 [US1] 🔥 Verificar la purga de reinstalación **en iOS**: guardar sesión, desinstalar la app del simulador, reinstalar y abrir. Debe arrancar **sin sesión** (quickstart escenario 4). Los escenarios 1 a 3 pasan igualmente aunque T008 esté mal, así que este es el único que lo detecta

**Checkpoint**: US1 completa. La brecha 3 de la constitución queda cerrada.

---

## Phase 4: User Story 2 - Cada rol ve lo que le corresponde (Priority: P2)

**Goal**: comprobar por primera vez, con sesiones reales, que la restricción de acceso por rol funciona.

**Independent Test**: cargar sucesivamente una sesión de cada rol y recorrer la app comprobando qué áreas aparecen.

### Implementation for User Story 2

- [X] T020 [US2] Añadir el campo `DEV_SESSION_ROLE` —cadena, **vacío por defecto**— a `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`, resolviéndolo con la cadena `resolve()` existente y declarándolo **solo** cuando el flavor seleccionado es `local` (D8)
- [X] T021 [US2] Implementar el sembrador de sesión en `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/DevSessionSeeder.kt`: si `BuildKonfig.ENVIRONMENT` no es `"local"` o `DEV_SESSION_ROLE` está vacío, no hace nada. En caso contrario pide el token a `POST /api/dev/token` del backend —acepta `subject` y `role` opcionales, devuelve un mapa con `subject`, `role`, `accessToken` y `refreshToken`— y escribe la sesión mediante `SessionStorage`. Gatear **dentro** de la clase, igual que `BackendHealthProbe` de la spec 1, para que ningún build de staging o producción pueda emitirlo
- [X] T022 [US2] Registrar el sembrador en `core/data/src/commonMain/kotlin/com/granatum/core/data/di/CoreDataModule.kt` e invocarlo al arranque junto a la sonda de salud existente en `composeApp/src/commonMain/kotlin/com/granatum/app/App.kt`. **Cero presencia en la UI**: ni pantalla, ni botón, ni entrada de menú
- [X] T023 [US2] Compilar ambos targets tras los cambios del convention plugin
- [X] T024 [US2] ⚠️ **Antes de dar por buenos los escenarios de rol**: sembrar un rol y comprobar que el que llega en la sesión es realmente el pedido. Si el token de desarrollo no trajera un rol utilizable, los tres escenarios siguientes darían el mismo resultado y **parecerían correctos**
- [X] T025 [US2] Verificar el rol de **persona empleada**: compilar con `-Pbuildkonfig.flavor=local -PDEV_SESSION_ROLE=EMPLEADO`, recorrer la app y confirmar que solo alcanza fichaje e historial, y que **no** hay forma de llegar a inventario ni al panel de equipo (quickstart escenario 5, regla V8)
- [X] T026 [US2] Verificar el rol de **encargada** con `-PDEV_SESSION_ROLE=ENCARGADO`: además de lo anterior, inventario y panel de equipo (quickstart escenario 6)
- [X] T027 [US2] Verificar el rol de **administradora** con `-PDEV_SESSION_ROLE=ADMIN`: mismo resultado que encargada (quickstart escenario 7)
- [X] T028 [US2] Verificar que **sin sesión** la app aplica el nivel más restringido, no el más permisivo (regla V9, FR-011)

**Checkpoint**: US1 y US2 funcionan de forma independiente.

---

## Phase 5: User Story 3 - Actualizar no deja a nadie atrapado (Priority: P3)

**Goal**: que una sesión heredada en claro se descarte limpiamente al actualizar.

**Independent Test**: instalar la versión anterior, guardar sesión, actualizar y abrir.

### Implementation for User Story 3

- [X] T029 [US3] Implementar `LegacySessionCleaner` en `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/storage/LegacySessionCleaner.kt`: borra del DataStore la clave `stringPreferencesKey("KEY_AUTH_INFO")` **incondicionalmente y sin leer su contenido** (D7, FR-008). No leerla evita reintroducir en memoria una credencial que ya se considera comprometida
- [X] T030 [US3] Registrarlo en `core/data/src/commonMain/kotlin/com/granatum/core/data/di/CoreDataModule.kt` e invocarlo una sola vez al arranque, **antes** de la primera lectura de la sesión
- [X] T031 [US3] Verificar que la clave nueva de T005 y la heredada `"KEY_AUTH_INFO"` son **distintas**, y que tras ejecutar el limpiador la sesión recién escrita en el almacén seguro sigue intacta
- [ ] T032 [US3] Verificar el ciclo completo de actualización: instalar la versión anterior a esta feature, guardar sesión, actualizar sin desinstalar y abrir. Debe arrancar **sin sesión**, sin bloquearse, y la clave antigua ya no debe existir (quickstart escenario 9, FR-009)

**Checkpoint**: las tres historias completas.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: los casos límite y el cierre. Nada de esto ocurre solo.

- [ ] T033 Verificar el comportamiento con el **almacén no disponible**: provocar el fallo —retirando el bloqueo de pantalla del dispositivo, o forzando el error en la implementación de plataforma— y abrir la app. Debe arrancar y ser usable comportándose como si no hubiera sesión, **nunca cerrarse**, y dejar su traza en `AppLogger` (quickstart escenario 8, FR-006, SC-004)
- [ ] T034 Verificar el comportamiento con **credencial corrupta**: escribir basura en el almacén y abrir. Mismo resultado que T033; la credencial se descarta (FR-007, regla V6)
- [X] T035 [P] Revisar que no se ha introducido ningún `println` ni `android.util.Log` en los ficheros nuevos de `core/data`, y que toda traza pasa por `AppLogger` (principio IV)
- [X] T036 [P] Revisar que no se ha añadido ninguna dependencia a `gradle/libs.versions.toml` ni a ningún `build.gradle.kts` de módulo: esta feature debe cerrarse con cero dependencias nuevas (D1, D2)
- [X] T037 Verificación final de Android: `./gradlew :composeApp:assembleDebug`
- [X] T038 Verificación final de iOS: `./gradlew :core:data:compileKotlinIosSimulatorArm64`
- [ ] T039 Recorrer la lista de verificación final de `quickstart.md` y marcar los nueve escenarios. Recordar que ninguno es automático y que compilar no cubre ninguno de ellos

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (Setup)
   ↓
Phase 2 (Foundational: contrato del almacén)
   ↓
Phase 3 (US1) ◄── MVP, cierra la brecha 3
   ↓
   ├─► Phase 4 (US2)  necesita sesiones que guardar
   └─► Phase 5 (US3)  necesita la clave nueva de T005
   ↓
Phase 6 (Polish)
```

### User Story Dependencies

- **US1**: depende de la fase 2. Es el MVP y lo que de verdad justifica la feature
- **US2**: depende de US1 —no se puede sembrar una sesión sin almacén donde guardarla— y necesita el backend en perfil `dev`
- **US3**: depende de T005, para garantizar que las claves no colisionan. Independiente de US2

### Within Each User Story

`SecureSessionStorage.kt` concentra T009, T010 y T011: **no son paralelizables entre sí**. Lo mismo para `SecureStore.ios.kt` con T007 y T008, y para `CoreDataModule.kt` con T012, T022 y T030.

### Parallel Opportunities

- **T006 y T007**: las dos implementaciones de plataforma son ficheros distintos y no se pisan
- **T001 y T002**: las dos compilaciones de línea base
- **T035 y T036**: revisiones independientes

---

## Parallel Example: User Story 1

```text
# Las dos plataformas, a la vez, una vez existe el contrato de T004:
T006  SecureStore.android.kt   (Keystore + DataStore)
T007  SecureStore.ios.kt       (Keychain)
```

T008 va después de T007: mismo fichero.

---

## Implementation Strategy

### MVP

**La fase 3 (US1) es el MVP**, con la 2 como requisito. Dentro de ella, **T008 y T019 son lo que más fácilmente se queda sin hacer**: la purga de reinstalación en iOS no se manifiesta en ninguna otra prueba, y un almacén aparentemente correcto convive con ella rota.

### Entrega incremental

1. **Fases 1-2** → contrato listo, nada roto
2. **Fase 3** → brecha 3 cerrada; ya es commiteable y entregable por sí solo
3. **Fase 4** → la restricción por rol deja de ser una suposición
4. **Fase 5** → la actualización desde la versión anterior es segura
5. **Fase 6** → casos límite y cierre

Cada fase deja el repositorio en estado commiteable. Como no hay CI, conviene ejecutar T037 y T038 al cerrar cada fase, no solo al final — y recordar que pasan aunque el almacén no funcione.
