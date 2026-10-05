# Tasks: Identidad de app y conexión por entorno

**Input**: Documentos de diseño en `/specs/001-backend-env-config/`

**Prerequisites**: plan.md, spec.md, research.md (10 decisiones), data-model.md, contracts/build-config.md, quickstart.md

**Tests**: No se generan tareas de test. La spec no los pidió y el inventario (D7) confirmó que no existe ningún test que ejercite lo que se toca. La verificación es manual y está en la fase final.

**Organization**: Agrupado por historia de usuario. **El orden de las fases no sigue la prioridad estricta**: US3 va antes que US1 y US2 porque es completamente independiente, de riesgo nulo y reduce el número de ficheros de `core/data` en juego antes de entrar en la parte delicada. Decisión del plan.

> **Nota honesta sobre ese orden.** La justificación original era que la limpieza «reduce la superficie que la migración tiene que tocar». Tras la decisión D10 —conservar `UrlConstants` como envoltorio— ese solapamiento casi desaparece: la migración ya no reescribe consumidores. El orden se mantiene porque US3 sigue siendo independiente y sin riesgo, no porque desbloquee nada.

## Estado: 33/35 completadas (2026-10-05)

**T019 verificada**: la app arranca en el simulador de iOS y alcanza el backend en
`localhost:8080`. Con el literal `10.0.2.2` anterior no habría llegado nada. Android hace lo
propio por `10.0.2.2` contra el mismo servidor. Es la primera vez que ambas plataformas
hablan con un backend local en este proyecto.

Dos salvedades sobre cómo se verificó, para que nadie lea más de lo que hay:

- El backend real **no está terminado**, así que se levantó un stub HTTP mínimo en `:8080`
  que responde `200` a `/actuator/health`. Eso prueba la cadena completa
  —BuildKonfig → UrlConstants → sonda → red— y que cada plataforma resuelve la dirección
  correcta. No prueba nada sobre la API real.
- Xcode 26.0.1 **sí está instalado**, pero `xcode-select` apunta a las Command Line Tools.
  Se sorteó con `DEVELOPER_DIR` para esta sesión. Arreglo permanente, que requiere
  contraseña: `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`.

**T031 resuelta sin necesidad de mitigación**: «Granatum Suite» se muestra completo en el
springboard del iPhone 17 Pro, sin elipsis. La preocupación por los 14 caracteres no se
materializó, así que no se añade nombre corto para iOS.

**T030 completa en ambas plataformas.** La captura del springboard destapó que Granatum Suite
y Squadfy_App compartían icono placeholder y eran indistinguibles en el lanzador. Resuelto
con la inicial de la marca sobre granate `#8C1D3F`:

- **Android**: `VectorDrawable` en un adaptive icon. Al ser `minSdk 26`, lo ven todos los
  dispositivos soportados y los mipmap heredados dejan de mostrarse.
- **iOS**: PNG de 1024×1024 sin alfa, generado con CoreGraphics desde un script de Swift
  (`scratchpad/MakeIcon.swift`), porque el asset catalog exige imagen y no hay PIL en la
  máquina. El formato moderno solo pide esa resolución; Xcode deriva el resto.

Quedan 2 tareas, ambas humanas:

| Tarea | Por qué |
|---|---|
| T027 | Validación de onboarding con una persona ajena al proyecto |
| T035 | 7 de 8 escenarios verificados; el que falta es T027 |

## Format: `[ID] [P?] [Story] Description`

- **[P]**: puede ejecutarse en paralelo (fichero distinto, sin dependencias pendientes)
- **[US#]**: historia de usuario a la que pertenece

## Path Conventions

Rutas relativas a la raíz del repositorio. Paquete `com.granatum`. Los caminos largos de Kotlin se abrevian con `…` donde no hay ambigüedad.

---

## Phase 1: Setup

**Purpose**: Establecer una línea base verificada antes de tocar nada. Sin CI, es la única forma de saber que una rotura posterior la hemos causado nosotros.

- [X] T001 Verificar que la línea base compila en Android ejecutando `./gradlew :composeApp:assembleDebug` y anotar el resultado
- [X] T002 Verificar que la línea base compila en iOS ejecutando `./gradlew :core:data:compileKotlinIosSimulatorArm64` y anotar el resultado

**Checkpoint**: línea base conocida. Cualquier fallo a partir de aquí es nuestro.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Fontanería compartida que necesitan US1 y US2. Hasta que esto exista, ninguna de las dos puede avanzar.

**⚠️ CRITICAL**: US1 y US2 dependen de esta fase.

- [X] T003 Añadir la función `resolve(key, default)` a `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`, con la cadena de Squadfy_KMM: `providers.gradleProperty(key).orNull ?: localProperties.getProperty(key) ?: default`. Extraer `gradleLocalProperties(rootDir, rootProject.providers)` a una variable local reutilizable, como hace Squadfy, en vez de llamarlo dentro de `defaultConfigs`
- [X] T004 Exponer el campo `BASE_URL_HTTP` en `defaultConfigs { }` de `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt` mediante `buildConfigField(FieldSpec.Type.STRING, "BASE_URL_HTTP", resolve("BASE_URL_HTTP", DEFAULT_BASE_URL_HTTP))`, con `DEFAULT_BASE_URL_HTTP = "http://10.0.2.2:8080/api"` como constante privada del fichero. Conservar intacto el campo `API_KEY` y su fallo rápido
- [X] T005 Convertir `core/data/src/commonMain/kotlin/com/granatum/core/data/networking/UrlConstants.kt` en envoltorio sobre BuildKonfig (D10): `object UrlConstants { val BASE_URL_HTTP = BuildKonfig.BASE_URL_HTTP }`. Cambiar `const val` por `val`, eliminar `BASE_URL_WS` (D8: sin consumidores en Granatum) y añadir KDoc explicando que el valor llega inyectado en construcción

**Checkpoint**: la configuración ya viaja inyectada. `HttpClientExt.kt` sigue sin tocarse.

---

## Phase 3: User Story 3 - La app no ofrece caminos que no existen (Priority: P2)

**Goal**: Retirar los flujos de autenticación heredados del esqueleto que Granatum no usa, sin dejar restos.

**Independent Test**: buscar los símbolos retirados en `core/` y no encontrar ninguno, comprobando a la vez que `login`, `changePassword` y `logout` siguen funcionando.

### Implementation for User Story 3

- [X] T006 [US3] Retirar los **cinco** métodos de la interfaz en `core/domain/src/commonMain/kotlin/com/granatum/core/domain/auth/repository/AuthRepository.kt`: `register`, `resendVerificationEmail`, `verifyEmail`, `forgotPassword` y `resetPassword`. **Conservar** `login`, `changePassword` y `logout`. Ojo: `resendVerificationEmail` no figuraba en el enunciado original, lo encontró el inventario (D4)
- [X] T007 [US3] Retirar las implementaciones de esos cinco métodos en `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/KtorAuthRepositoryImpl.kt`, junto con los `import` que queden sin uso
- [X] T008 [US3] Retirar las **cinco** rutas de `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/provider/AuthRoutes.kt`: `REGISTER_ROUTE`, `VERIFY_EMAIL_ROUTE`, `RESEND_VERIFICATION_ROUTE`, `FORGOT_PASSWORD_ROUTE` y `RESET_PASSWORD_ROUTE`. **Conservar** `LOGIN_ROUTE` y el `baseUrl = "/auth"`
- [X] T009 [P] [US3] Eliminar `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/dto/request/RegisterRequestDTO.kt`
- [X] T010 [P] [US3] Eliminar `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/dto/request/ResetPasswordRequestDTO.kt`
- [X] T011 [P] [US3] Eliminar `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/dto/request/EmailRequestDTO.kt`
- [X] T012 [US3] **NO BORRAR** `core/data/src/commonMain/kotlin/com/granatum/core/data/auth/dto/request/RefreshRequestDTO.kt`. Verificar que sigue existiendo y que `core/data/src/commonMain/kotlin/com/granatum/core/data/networking/HttpClientFactory.kt` lo sigue usando. Parece huérfano porque `AuthRepository` no declara ningún `refresh`, pero sostiene la renovación de token: borrarlo rompe la sesión (D5)
- [X] T013 [US3] Verificar que los tres falsos positivos de D7 **siguen intactos**: `registerNetworkCallback` en `feature/clockin/data/src/androidMain/.../sync/ConnectivityObserver.android.kt`, `registeredTypeIdentifiers` en `core/presentation/src/iosMain/.../mediapicker/rememberImagePickerLauncher.ios.kt` y el `register` de Gradle en `build-logic/convention/build.gradle.kts`. Ninguno tiene relación con autenticación

**Checkpoint**: US3 completa y verificable sola. Compila en ambos targets.

---

## Phase 4: User Story 1 - Compilar apuntando a un entorno concreto (Priority: P1) 🎯 MVP

**Goal**: Tres entornos elegibles al construir, y —lo más importante— que iOS alcance por fin el backend local.

**Independent Test**: construir dos binarios seguidos con flavors distintos y comprobar que apuntan a servidores distintos sin haber editado código.

### Implementation for User Story 1

- [X] T014 [US1] Añadir los tres flavors en `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt` con `defaultConfigs("local") { }`, `defaultConfigs("staging") { }` y `defaultConfigs("prod") { }`, cada uno resolviendo su `BASE_URL_HTTP` con las claves de `contracts/build-config.md`: `BASE_URL_HTTP_STAGING` y `BASE_URL_HTTP_PROD`. El flavor `local` no exige ninguna clave: trae valor por defecto
- [X] T015 [US1] **Corrección de iOS.** Añadir `targetConfigs { create("ios") { … } }` en `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt` para que el target de iOS resuelva `BASE_URL_HTTP` a `http://localhost:8080/api` en el flavor `local`, mientras Android conserva `http://10.0.2.2:8080/api`. Admitir override por `BASE_URL_HTTP_IOS`. Verificado en el jar de BuildKonfig 0.17.1: `BuildKonfigExtension` expone `targetConfigs(Action)` y `targetConfigs(String, Action)`. **Esto es exactamente lo que Squadfy_KMM no tiene**: su valor por defecto es `10.0.2.2` para todos los targets y su KDoc reconoce que iOS exige override manual
- [X] T016 [US1] Verificar que `core/data/src/commonMain/kotlin/com/granatum/core/data/networking/HttpClientExt.kt` **no ha sido modificado**. Debe seguir importando `UrlConstants.BASE_URL_HTTP` y su construcción de rutas (líneas 134-136) debe quedar igual. Si ha habido que tocarlo, el envoltorio de T005 está mal hecho
- [X] T017 [P] [US1] Actualizar `local.properties.example` con las claves nuevas y su significado, sin valores reales, según la tabla de `contracts/build-config.md`: `BASE_URL_HTTP`, `BASE_URL_HTTP_IOS`, `BASE_URL_HTTP_STAGING` y `BASE_URL_HTTP_PROD`. Conservar la documentación existente de `API_KEY` y `sdk.dir`, y dejar claro que el flavor `local` funciona sin configurar nada
- [X] T018 [US1] Ejecutar `./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=local`, instalar en el emulador de Android y comprobar que la comprobación de salud del backend local responde (quickstart escenario 1)
- [X] T019 [US1] 🔥 **Arrancar en el simulador de iOS contra el backend local.** Ejecutar `./gradlew :core:data:compileKotlinIosSimulatorArm64`, abrir `iosApp/iosApp.xcodeproj` en Xcode, ejecutar en simulador y comprobar que alcanza el **mismo** backend que Android (quickstart escenario 2). Es la tarea de mayor valor de la feature: como el literal `10.0.2.2` vivía en `commonMain`, es muy probable que esto no haya funcionado nunca. Si pasa, es la primera vez
- [X] T020 [US1] Comprobar que cambiar de entorno no toca código: anotar `git status --porcelain`, construir con flavor `local` y luego con `prod`, y verificar que el resultado de `git status --porcelain` es idéntico (quickstart escenario 3, criterio SC-001)

**Checkpoint**: US1 completa. Ambas plataformas alcanzan el backend local y el entorno se cambia sin editar nada.

---

## Phase 5: User Story 2 - Producción nunca habla en claro (Priority: P2)

**Goal**: Hacer imposible publicar un binario que hable sin cifrar.

**Independent Test**: configurar producción con una URL en claro y comprobar que la construcción se detiene en lugar de producir un instalable.

### Implementation for User Story 2

- [X] T021 [US2] Añadir la validación de cifrado en `build-logic/convention/src/main/kotlin/BuildKonfigConventionPlugin.kt`: si el flavor es `staging` o `prod` y el `BASE_URL_HTTP` resuelto no empieza por `https://`, lanzar `IllegalStateException` con un mensaje que nombre el flavor y explique que esos entornos exigen conexiones cifradas (regla V2 de data-model.md)
- [X] T022 [US2] Añadir la validación de clave ausente en el mismo fichero: si falta la clave obligatoria del flavor seleccionado, fallar nombrando la clave y el flavor, sin recurrir a ningún valor por defecto (regla V1, FR-007). Seguir el patrón de fallo rápido que ya usa `API_KEY`
- [X] T023 [US2] Añadir la validación de flavor desconocido en el mismo fichero: un `-Pbuildkonfig.flavor` que no sea `local`, `staging` ni `prod` detiene la construcción enumerando los tres válidos (regla V5)
- [X] T024 [US2] Prueba negativa de cifrado: poner temporalmente `BASE_URL_HTTP_PROD=http://api.ejemplo/api` en `local.properties`, ejecutar `./gradlew :composeApp:assembleRelease -Pbuildkonfig.flavor=prod` y comprobar que **falla**. Si produce un APK, la historia no está cumplida. Restaurar el valor `https://` al terminar (quickstart escenario 4)
- [X] T025 [US2] Prueba negativa de configuración ausente: comentar `BASE_URL_HTTP_STAGING` en `local.properties`, construir con flavor `staging` y comprobar que falla nombrando la clave; repetir con `-Pbuildkonfig.flavor=preproduccion` y comprobar que enumera los tres flavors válidos (quickstart escenario 5)

**Checkpoint**: US1 y US2 funcionan de forma independiente. Publicar en claro es ya imposible por descuido.

---

## Phase 6: User Story 4 - Un desarrollador nuevo arranca la app sin ayuda (Priority: P3)

**Goal**: Que el README sirva para arrancar el proyecto sin preguntar a nadie.

**Independent Test**: alguien ajeno sigue solo el README y consigue la app corriendo contra el backend local.

### Implementation for User Story 4

- [X] T026 [US4] Reescribir `README.md` para Granatum Suite: qué es la app, requisitos, cómo rellenar `local.properties`, cómo elegir entorno con `-Pbuildkonfig.flavor` y cómo arrancar en Android e iOS contra el backend local. Retirar el título «KMM Skeleton» y los pasos de clonar la plantilla y renombrar el paquete, que ya no aplican. **Conservar** las secciones «Spec-driven development (Spec Kit)» y «Project structure», actualizando esta última si procede
- [ ] T027 [US4] Validar el onboarding con alguien ajeno al proyecto: que siga **solo** el README, cronometrar y anotar dónde se atasca. Objetivo SC-006: menos de 30 minutos. Lo que le atasque es lo que hay que corregir en el README (quickstart escenario 8)

**Checkpoint**: las cuatro historias completas.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Identidad visible y verificación final. Sin CI, ktlint ni Kover (brechas 1 y 2 de la constitución), **nada de esto ocurre solo**: es trabajo explícito.

- [X] T028 [P] Fijar el nombre visible «Granatum Suite» en `composeApp/src/androidMain/res/values/strings.xml` (FR-013). Si el recurso no existe, crearlo
- [X] T029 [P] Fijar `CFBundleDisplayName` a «Granatum Suite» en el `Info.plist` de `iosApp/` (FR-013)
- [X] T030 [P] Crear el icono provisional —inicial de la marca sobre color plano— para Android en `composeApp/src/androidMain/res/` y para iOS en el asset catalog de `iosApp/` (FR-015)
- [X] T031 **Condicional.** Instalar en el simulador de iOS y mirar el lanzador. «Granatum Suite» son 14 caracteres e iOS recorta en torno a 12. **Solo si se ve recortado**, fijar un nombre corto exclusivo de iOS vía `CFBundleName`, sin tocar Android (caso límite de la spec)
- [X] T032 Verificar que no queda ningún camino muerto de autenticación: `grep -rn "register\|verifyEmail\|resendVerification\|forgotPassword\|resetPassword" --include="*.kt" core/ composeApp/ feature/ | grep -v "/build/"` no debe devolver nada en `core/`. Los aciertos en `ConnectivityObserver.android.kt` y `rememberImagePickerLauncher.ios.kt` son los falsos positivos de D7 y **no deben tocarse** (quickstart escenario 6, criterio SC-005)
- [X] T033 Verificación final de Android: `./gradlew :composeApp:assembleDebug` sin errores
- [X] T034 Verificación final de iOS: `./gradlew :core:data:compileKotlinIosSimulatorArm64` sin errores. Principio XII: la feature no está terminada hasta que ambos targets compilan
- [ ] T035 Recorrer la lista de verificación final de `quickstart.md` y marcar los ocho escenarios. Ninguno es automático

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (Setup)
   ↓
Phase 2 (Foundational) ──────┐
   ↓                         │
Phase 3 (US3) independiente  │
                             ↓
                        Phase 4 (US1)
                             ↓
                        Phase 5 (US2)
                             ↓
                        Phase 6 (US4)
                             ↓
                        Phase 7 (Polish)
```

### User Story Dependencies

- **US3** (limpieza de auth): independiente de todo. Podría ir en cualquier momento; va primero por ser de riesgo nulo
- **US1** (entornos): depende de la fase 2
- **US2** (HTTPS): depende de US1, porque valida los flavors que US1 crea
- **US4** (README): depende de US1 y US2, porque documenta lo que hacen

### Within Each User Story

Las tareas de un mismo fichero van en serie. `BuildKonfigConventionPlugin.kt` concentra T003, T004, T014, T015, T021, T022 y T023: **ninguna de ellas es paralelizable entre sí**.

### Parallel Opportunities

- **T009, T010, T011**: los tres borrados de DTO son ficheros distintos
- **T017**: `local.properties.example` es independiente del plugin
- **T028, T029, T030**: identidad en Android, iOS y los iconos no se pisan

---

## Parallel Example: User Story 3

```text
# Los tres borrados de DTO, a la vez:
T009  RegisterRequestDTO.kt
T010  ResetPasswordRequestDTO.kt
T011  EmailRequestDTO.kt
```

---

## Implementation Strategy

### MVP

**La fase 4 (US1) es el MVP**, con la fase 2 como requisito. Y dentro de ella, **T015 y T019 son el corazón de la feature**: el `targetConfigs` que arregla iOS y la verificación de que el simulador alcanza por fin el backend local.

### Entrega incremental

1. **Fases 1-2** → la configuración viaja inyectada
2. **Fase 3** → código muerto fuera, ya commiteable
3. **Fase 4** → MVP: ambas plataformas contra el backend local
4. **Fase 5** → red de seguridad para producción
5. **Fases 6-7** → documentación, identidad y verificación

Cada fase deja el repositorio en estado commiteable. Dado que no hay CI, conviene ejecutar T033 y T034 al cerrar cada fase, no solo al final.
