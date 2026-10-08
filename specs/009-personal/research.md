# Phase 0 — Investigación: alta del propietario y personal

**Feature**: `009-personal` · **Fecha**: 2026-10-09

Verificado contra el backend `2ec33d0` (`develop`): `specs/009-owner-onboarding`, `AltaPersonaController`,
`RegistroDtos`, `AuthExceptionHandler`, `EmpleadoController` y `SecurityConfig`.

## D1 — Contrato

Se fija `docs/openapi.json` a `2ec33d0` con `scripts/sync-openapi.sh`. Cambios: `codigoArranque`
obligatorio y `201` en el registro; fuera `/api/auth/registros*` y `REGISTRO_PENDIENTE`; nueva
`POST /api/auth/altas`. Ninguna ruta usada por la app cambia (comprobado rutas y esquemas).

## D2 — Registro del propietario en el módulo de auth

`AuthRepository.registerOwner(...)` hace `POST /api/auth/registro` y, si va bien, el mismo login de
siempre: el registro no devuelve tokens. Errores nuevos en `AuthError`: `InvalidBootstrapCode`
(`403 CODIGO_ARRANQUE_INVALIDO`, que el servidor usa igual para código erróneo, no configurado o
ADMIN existente), `EmailTaken`, `AccountExists`, `InvalidDocument`. La pantalla vive en
`feature/auth/presentation`, y la puerta de sesión alterna entre login y registro.

## D3 — Personal en un módulo nuevo, solo con conexión

`feature/staff/{domain,data,presentation}` contra `/api/empleados`, `/api/auth/altas` y
`/api/auth/cuentas`. Sin caché: datos personales (DNI) que solo usa la administración en la
oficina, como facturación. La lista no pagina en el servidor: búsqueda y filtro en local.

## D4 — Contraseña provisional

Solo en el estado del ViewModel mientras se muestra; se borra al cerrar la hoja. Se copia con el
portapapeles del sistema. Las rutas `/auth/` ya están fuera del log (spec 004); se añade
`/empleados` (D6).

## D5 — Saber si una ficha tiene cuenta

`EmpleadoDto` no lo dice y no hay ruta para listar cuentas. «Restablecer» se intenta y, ante
`404 CUENTA_NO_ENCONTRADA`, la app ofrece «Dar acceso» (`POST /api/auth/cuentas`).

## D6 — Log de red

El filtro de logging excluye también `/empleados/` y `/empleados` (nombres y DNI).

## D7 — Navegación

ADMIN ya tiene cinco pestañas. Su pestaña **Equipo** abre el personal, con un acceso a la jornada
del equipo (la pantalla actual); la encargada sigue con la jornada. `UserRole.canManageStaff`
(solo ADMIN).

## D8 — Desactivar

`PATCH /api/empleados/{id}/activo`. El servidor no revoca las sesiones: el token sigue valiendo
hasta caducar (15 min) y la renovación falla con `EMPLEADO_INACTIVO`. La app lo explica al
confirmar. No se ofrece sobre la ficha propia (el id de la sesión).

## D9 — Errores

Personal: `EMPLEADO_NOT_FOUND` (404), `DOCUMENTO_DUPLICADO` (409), `VALORES_INCOHERENTES` (422),
`VALIDACION` (400). Altas y cuentas: `DOCUMENTO_INVALIDO` (422), `EMAIL_YA_REGISTRADO` (409),
`CUENTA_YA_EXISTE` (409), `EMPLEADO_NO_ENCONTRADO` y `CUENTA_NO_ENCONTRADA` (404). Comunes:
`FORBIDDEN` (403), `DEMASIADAS_PETICIONES` (429).

## D10 — Tests

Datos con `MockEngine` (campos del alta, del registro y de la edición, sin documento en el `PUT`;
mapeo de errores) y ViewModels (registro y login encadenado, alta con credenciales, lista con
filtros, edición, baja, restablecer con caída a «dar acceso»).
