# Contrato — Almacén seguro y comportamiento ante fallos

**Feature**: `002-secure-session-storage` · **Fecha**: 2026-10-05

Esta feature no expone ninguna API de red. Sus contratos son dos: el que **no** cambia, hacia
el resto de la aplicación, y el nuevo hacia cada plataforma.

## 1. Contrato que NO cambia: `SessionStorage`

El interfaz de `core/domain` se queda exactamente como está (FR-003):

```kotlin
interface SessionStorage {
    fun observeAuthInfo(): Flow<AuthInfoModel?>
    suspend fun set(info: AuthInfoModel?)
}
```

Consumidores, verificados por rastreo (D9): `HttpClientFactory` —autenticación Bearer y
renovación— y `NavigationRoot` —rol para decidir pestañas—. **Ninguno se modifica.** Si al
implementar hace falta tocarlos, el diseño está mal.

Lo que sí cambia es la garantía detrás del contrato: `set(null)` ya no solo olvida el valor,
sino que lo elimina del almacén seguro sin rastro recuperable.

## 2. Contrato nuevo: almacén seguro por plataforma

Tres operaciones. Todo lo demás —serializar, mapear, observar— vive en común (D4).

| Operación | Entrada | Salida | Semántica |
|---|---|---|---|
| Leer | clave | la cadena, o nada | «Nada» cubre tanto ausencia como fallo |
| Escribir | clave, cadena | — | Sustituye lo que hubiera. Atómica |
| Borrar | clave | — | Idempotente: borrar lo que no existe no es un error |

### Implementación por plataforma

| | Android | iOS |
|---|---|---|
| Custodia | Clave AES-GCM en Keystore; texto cifrado en DataStore | Keychain |
| Accesibilidad | — | `AfterFirstUnlockThisDeviceOnly` |
| Dependencias nuevas | ninguna | ninguna |
| Particularidad | — | **Purga en el primer arranque tras reinstalar** (D3) |

`AfterFirstUnlock` no es un capricho: permite que la sincronización en segundo plano del
fichaje —spec 4— lea el token con la pantalla bloqueada. `ThisDeviceOnly` impide que la
credencial viaje en una copia de seguridad a otro dispositivo.

## 3. Contrato de fallo

El principio más importante de esta feature: **ningún fallo del almacén sale de `core/data`**.

| Situación | Comportamiento | Requisito |
|---|---|---|
| Almacén no disponible | «No hay sesión». La app arranca igual | FR-006 |
| Credencial ilegible o corrupta | «No hay sesión». Se descarta | FR-007 |
| Fallo al escribir | Se registra; no se lanza al llamante | principio IV |
| Fallo al borrar | Se registra; se considera borrada | FR-005 |
| Sesión heredada en claro | Se borra sin leerse | FR-008 |

Todas las situaciones se registran mediante `AppLogger`. **Nunca `println` ni
`android.util.Log`** (principio IV).

Ninguna excepción cruza hacia `core/domain` ni hacia la presentación. El contrato de
`SessionStorage` no devuelve `Result`, así que el único sitio donde un fallo puede manejarse
sin cambiar la firma es dentro de la implementación — y cambiarla está prohibido por FR-003.

## 4. Andamio de verificación: sembrado de sesiones

**No forma parte del producto.** Existe solo para poder comprobar los tres roles sin pantalla
de inicio de sesión.

| | |
|---|---|
| Activación | Solo cuando el entorno de compilación es `local` |
| Selección de rol | Campo de configuración de build, vacío por defecto |
| Origen del token | Extremo de desarrollo del backend, en su perfil `dev` |
| Presencia en la UI | **Ninguna** |

En cualquier build que no sea `local` el campo no existe y el sembrador no se registra, igual
que la sonda de salud de la spec 1. Verificar los tres roles cuesta tres compilaciones.

Esto **no** es una vía de acceso ni un contrato de producción: el inicio de sesión real llega
con el módulo de empleados del backend.
