# Quickstart — Validación de la feature

**Feature**: `002-secure-session-storage` · **Fecha**: 2026-10-05

Ocho escenarios. Cada uno mapea a un criterio de éxito de [spec.md](./spec.md). El contrato de
fallo está en [contracts/secure-store.md](./contracts/secure-store.md).

> **Compilar no demuestra nada aquí.** A diferencia de la spec 1, la mitad de esta feature es
> código de plataforma cuyo comportamiento solo se observa en el dispositivo. Un
> `BUILD SUCCESSFUL` es compatible con un Keychain que no guarda nada. Y no hay CI que lo
> atrape (brechas 1 y 2 de la constitución).

## Requisitos previos

- Emulador Android con API ≥ 26 y simulador de iOS arrancados.
- Backend en perfil de desarrollo, solo para los escenarios 5 a 7.

## Escenario 1 — La credencial no es legible en Android

Cubre **SC-001**. El escenario central de la feature.

Con una sesión guardada, volcar el almacenamiento de la app y buscar fragmentos reconocibles
de la credencial.

**Esperado**: ninguna coincidencia. Si aparece el DataStore antiguo con `KEY_AUTH_INFO`
legible, la fase 2 no está terminada.

## Escenario 2 — La credencial no es legible en iOS

Cubre **SC-001**.

Con una sesión guardada, inspeccionar el contenedor de datos de la app en el simulador.

**Esperado**: la credencial no aparece en texto plano en el contenedor. Vive en el Keychain,
que es un almacén aparte.

## Escenario 3 — La sesión sobrevive al reinicio

Cubre **SC-002**.

Guardar sesión, cerrar la app por completo —no suspender— y reabrir, en ambas plataformas.

**Esperado**: la sesión sigue ahí y la app se comporta igual que antes de cerrarla.

## Escenario 4 — Reinstalar en iOS no hereda la sesión

Cubre **SC-001** y el caso límite de desinstalación. **Es el escenario más fácil de pasar por
alto y el único que falla en silencio.**

```
1. Guardar una sesión.
2. Desinstalar la app del simulador.
3. Reinstalarla y abrirla.
```

**Esperado**: arranca **sin sesión**.

En iOS los elementos del Keychain **sobreviven a la desinstalación**. Sin la purga del primer
arranque (D3), quien reinstale hereda la sesión de quien usara el dispositivo antes. El código
parece correcto y la prueba de los escenarios 1 a 3 pasa igualmente: solo este lo detecta.

## Escenario 5 — Rol de persona empleada

Cubre **SC-003** y la historia 2.

Compilar sembrando el rol de empleada y recorrer la app.

**Esperado**: solo fichaje e historial. **No** debe haber forma de alcanzar inventario ni el
panel de equipo.

## Escenario 6 — Rol de encargada

Cubre **SC-003**. Compilar con el rol de encargada.

**Esperado**: además de lo anterior, inventario y panel de equipo.

## Escenario 7 — Rol de administradora

Cubre **SC-003**. Compilar con el rol de administradora.

**Esperado**: lo mismo que encargada.

Antes de dar por buenos los escenarios 5 a 7, comprobar que el rol que llega en la sesión es
el que se pidió: si el token de desarrollo no trajera rol utilizable, los tres escenarios
darían el mismo resultado y parecerían correctos.

## Escenario 8 — El almacén no está disponible

Cubre **SC-004** y el caso límite correspondiente. Prueba **negativa**.

Provocar el fallo del almacén —por ejemplo retirando el bloqueo de pantalla del dispositivo, o
forzando el error en la implementación de plataforma— y abrir la app.

**Esperado**: arranca y es usable, comportándose como si no hubiera sesión. **No** se cierra ni
muestra un error que impida continuar. En el registro aparece la traza de `AppLogger`.

Repetir con una credencial corrupta: escribir basura en el almacén y abrir.

**Esperado**: mismo comportamiento. La credencial corrupta se descarta.

## Escenario 9 — La sesión heredada en claro se descarta

Cubre **SC-005** y la historia 3.

```
1. Instalar la versión anterior (antes de esta feature) y guardar una sesión.
2. Actualizar a la nueva sin desinstalar.
3. Abrir.
```

**Esperado**: arranca sin sesión, y la clave antigua ya no existe en el almacenamiento.

## Lista de verificación final

| | Escenario | Criterio |
|---|---|---|
| ☐ | Credencial ilegible en Android | SC-001 |
| ☐ | Credencial ilegible en iOS | SC-001 |
| ☐ | Sobrevive al reinicio | SC-002 |
| ☐ | Reinstalar en iOS no hereda sesión | SC-001 |
| ☐ | Rol empleada | SC-003 |
| ☐ | Rol encargada | SC-003 |
| ☐ | Rol administradora | SC-003 |
| ☐ | Almacén no disponible y credencial corrupta | SC-004 |
| ☐ | Sesión heredada descartada | SC-005 |

Y las dos compilaciones que exige el principio XII:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :core:data:compileKotlinIosSimulatorArm64
```
