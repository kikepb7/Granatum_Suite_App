# Quickstart — Validación de la feature

**Feature**: `001-backend-env-config` · **Fecha**: 2026-10-05

Escenarios ejecutables que demuestran que la feature funciona. Cada uno mapea a un criterio
de éxito de [spec.md](./spec.md). Las claves y el comportamiento de fallo están en
[contracts/build-config.md](./contracts/build-config.md).

> **Sin red de seguridad automática.** Este repositorio no tiene CI, ni ktlint, ni Kover
> (brechas 1 y 2 de la constitución). Nada ejecutará esto por ti: si no se recorre a mano,
> no está verificado.

## Requisitos previos

- JDK 17, Xcode con un simulador de iOS disponible, un emulador de Android con API ≥ 26.
- Backend de Granatum corriendo en local en el puerto 8080.
- `local.properties` con las claves del flavor `local` (ver el contrato).

## Escenario 1 — Arranque en Android contra backend local

Cubre **SC-002** y el escenario 1 de la historia 1.

```bash
./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=local
```

Instalar en el emulador y abrir. **Esperado**: la app arranca y la comprobación de salud
responde correctamente contra el backend local.

## Escenario 2 — Arranque en iOS contra el mismo backend

Cubre **SC-002** y el escenario 2 de la historia 1. **Es el escenario más valioso de todos.**

```bash
./gradlew :core:data:compileKotlinIosSimulatorArm64
```

Abrir `iosApp/iosApp.xcodeproj` en Xcode, ejecutar en el simulador.

**Esperado**: la app alcanza el mismo backend que Android, pese a resolver una dirección
distinta.

> Atención: hasta esta feature, la URL local era el literal `10.0.2.2` en `commonMain`, un
> alias que **solo** entiende el emulador de Android. Es muy probable que iOS contra backend
> local no haya funcionado nunca. Si este escenario pasa, es la primera vez.

## Escenario 3 — Cambiar de entorno sin tocar código

Cubre **SC-001** y el escenario 3 de la historia 1.

```bash
git status --porcelain          # anotar el resultado
./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=local
./gradlew :composeApp:assembleRelease -Pbuildkonfig.flavor=prod
git status --porcelain          # debe ser idéntico al anterior
```

**Esperado**: dos binarios apuntando a servidores distintos, sin un solo fichero de código
modificado entre ambos.

## Escenario 4 — Producción rechaza tráfico en claro

Cubre **SC-003** y la historia 2. Es una prueba **negativa**: debe fallar.

En `local.properties`, poner temporalmente:

```properties
API_BASE_URL_PROD=http://api.ejemplo/api
```

```bash
./gradlew :composeApp:assembleRelease -Pbuildkonfig.flavor=prod
```

**Esperado**: la construcción **falla** con un mensaje que explica que producción exige
conexiones cifradas. Si produce un APK, la feature no cumple su propósito.

Restaurar el valor `https://` al terminar.

## Escenario 5 — Falta configuración

Cubre **SC-004** y el caso límite «falta un valor de configuración».

Comentar `API_BASE_URL_STAGING` en `local.properties`:

```bash
./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=staging
```

**Esperado**: falla nombrando la clave que falta y el flavor afectado. No debe recurrir a
ningún valor por defecto.

Repetir con un flavor inventado:

```bash
./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=preproduccion
```

**Esperado**: falla enumerando los tres flavors válidos.

## Escenario 6 — Sin caminos muertos de autenticación

Cubre **SC-005** y el escenario 2 de la historia 3.

```bash
grep -rn "register\|verifyEmail\|resendVerification\|forgotPassword\|resetPassword" \
  --include="*.kt" core/ composeApp/ feature/ | grep -v "/build/"
```

**Esperado**: ni un solo resultado en `core/`. Si aparecen `registerNetworkCallback` o
`registeredTypeIdentifiers`, son los falsos positivos documentados en D7 —conectividad de
Android y selector de fotos de iOS—, no tienen relación con autenticación y **no deben
tocarse**.

Comprobar además que `login`, `changePassword` y `logout` siguen en `AuthRepository`, y que
`RefreshRequestDTO` sigue existiendo: lo usa `HttpClientFactory` para renovar el token y
borrarlo rompería la sesión.

## Escenario 7 — Identidad visible

Cubre FR-013 y FR-015.

Instalar en ambas plataformas y mirar el lanzador. **Esperado**: «Granatum Suite» y el icono
provisional.

Si en iOS aparece recortado —son 14 caracteres y iOS recorta en torno a 12—, aplicar el
nombre corto solo para iOS descrito en el caso límite de la spec, sin tocar Android.

## Escenario 8 — Onboarding desde cero

Cubre **SC-006**.

Pedir a alguien ajeno al proyecto que clone el repositorio y siga **solo** el README, sin
preguntar. Cronometrar.

**Esperado**: app funcionando contra el backend local en menos de 30 minutos. Anotar dónde
se atasca: eso es lo que hay que arreglar en el README.

## Lista de verificación final

| | Escenario | Criterio |
|---|---|---|
| ☐ | Android contra local | SC-002 |
| ☐ | iOS contra local | SC-002 |
| ☐ | Cambio de entorno sin tocar código | SC-001 |
| ☐ | Producción en claro falla | SC-003 |
| ☐ | Configuración ausente falla | SC-004 |
| ☐ | Sin caminos muertos de auth | SC-005 |
| ☐ | Nombre e icono correctos | FR-013, FR-015 |
| ☐ | Onboarding en menos de 30 min | SC-006 |
