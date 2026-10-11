# Tasks: Entornos PRE y PRO, y usuarios demo en PRE

- [X] T001 `GranatumBuild` en build-logic (propiedad, `local.properties`, por defecto `pre`)
- [X] T002 `BuildKonfigConventionPlugin`: `BUILD`, `BASE_URL_LOCAL/PRE/PRO`; PRO solo con la URL de producción HTTPS
- [X] T003 Android: `applicationIdSuffix .pre`, nombre, tráfico sin cifrar y configuración de red por build
- [X] T004 iOS: `GRANATUM_BUILD`, bundle id y nombre en `Config.xcconfig`; la fase de Xcode lo pasa a Gradle
- [X] T005 Dominio: `AppEnvironment`, `EnvironmentRepository`, `EnvironmentScopedData`, `EnvironmentError`
- [X] T006 Datos: `ActiveEnvironment`, `EnvironmentRepositoryImpl` (carga al arrancar, cambio seguro), `SwitchingEngine`, `DemoGateway`; `constructRoute` sigue al entorno activo
- [X] T007 Fichaje e inventario registran sus datos por entorno (DAO de mantenimiento, sin cambiar esquema)
- [X] T008 `:core:demo` con el backend simulado y las cuentas demo; `composeApp` solo lo enlaza en PRE (`src/environment/pre|pro`)
- [X] T009 UI: selector en login y cuenta, cuentas demo en el login, franja de entorno, splash al arrancar en Demo
- [X] T010 PRO endurecido: sin log de red, sin tráfico sin cifrar
- [X] T011 Tests: entorno y cambio, motor conmutable, login, backend demo; CI comprueba que PRO no lleva demo
- [X] T012 README, `local.properties.example`, trinquete a 30
- [ ] T013 Verificación en dispositivo (al final)
