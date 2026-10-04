# Phase 1 — Modelo de datos

**Feature**: `001-backend-env-config` · **Fecha**: 2026-10-05

Esta feature no introduce persistencia ni entidades de dominio. Su único concepto con
estructura es el **Entorno**, que se resuelve en tiempo de construcción y queda congelado en
el binario.

## Entidad: Entorno

El destino con el que habla la aplicación. No es un objeto en memoria que se pueda cambiar:
se elige al construir y es inmutable durante toda la vida del binario.

### Campos

| Campo | Tipo | Origen | Descripción |
|---|---|---|---|
| `flavor` | enumerado | propiedad de Gradle | `local`, `staging` o `prod` |
| `API_BASE_URL` | texto | `local.properties` | Raíz de la API. En `local` se resuelve por plataforma |
| `API_KEY` | texto | `local.properties` | Ya existe hoy. Viaja como cabecera `x-api-key` |

### Reglas de validación

Se aplican en tiempo de configuración de Gradle, no en ejecución. Un incumplimiento detiene
la construcción.

| # | Regla | Requisito |
|---|---|---|
| V1 | `API_BASE_URL` debe estar definida para el flavor seleccionado | FR-007 |
| V2 | Con flavor `staging` o `prod`, `API_BASE_URL` debe empezar por `https://` | FR-003 |
| V3 | Solo el flavor `local` admite `http://`, y únicamente hacia la máquina anfitriona | FR-004 |
| V4 | `API_KEY` debe estar definida en todos los flavors | ya vigente |
| V5 | Un `flavor` no reconocido detiene la construcción | FR-007 |

V2 es la regla que convierte SC-003 en algo imposible de incumplir por descuido, en lugar de
algo que haya que recordar revisar antes de publicar.

### Valores por entorno

| Flavor | Android | iOS | Cifrado |
|---|---|---|---|
| `local` | `http://10.0.2.2:8080/api` | `http://localhost:8080/api` | no exigido |
| `staging` | igual en ambos | igual en ambos | **obligatorio** |
| `prod` | igual en ambos | igual en ambos | **obligatorio** |

La fila `local` es la razón de ser de la decisión D2: `10.0.2.2` es un alias que solo
entiende el emulador de Android. Hoy ese literal está en `commonMain` y, por tanto, iOS
contra backend local no puede haber funcionado nunca.

Las direcciones de `staging` y `prod` las facilita el equipo de backend. La app no las
inventa (principio VI).

### Transiciones de estado

Ninguna. El entorno se fija al construir y no cambia en ejecución. Deliberadamente: permitir
cambiarlo en caliente abriría la puerta a repuntar un binario de producción, que es
justamente lo que la historia de usuario 2 quiere impedir.

## Lo que esta feature NO modela

- **Sesión y credenciales**: pertenecen a la spec 2, incluido el paso al almacén seguro del
  sistema.
- **Entidades de negocio**: materiales y fichajes son de las specs 3 y 4.
- **WebSocket**: `BASE_URL_WS` se elimina sin sustituto porque no lo consume nadie (D8).
