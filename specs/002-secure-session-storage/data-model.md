# Phase 1 — Modelo de datos

**Feature**: `002-secure-session-storage` · **Fecha**: 2026-10-05

Esta feature **no introduce entidades nuevas**. Cambia dónde vive una que ya existe y cómo se
comporta cuando algo va mal.

## Entidad: Sesión

Lo que acredita a una persona frente al backend y determina qué puede ver. Ya existe en el
dominio; aquí solo cambia su custodia.

| Campo | Sensible | Notas |
|---|---|---|
| Credencial de acceso | **sí** | Es lo que esta feature protege |
| Credencial de renovación | **sí** | Igual de sensible: permite obtener nuevas |
| Identidad de la persona | sí | Dato personal |
| Rol | no en sí mismo | Pero viaja dentro de la sesión y no puede falsificarse localmente |

**Presencia frente a contenido**: que exista sesión o no, no es secreto —la app lo refleja en
la propia navegación—. Lo secreto es su contenido. Por eso el marcador de instalación de D3
puede vivir en el almacenamiento ordinario sin comprometer nada.

### Reglas

| # | Regla | Requisito |
|---|---|---|
| V1 | Se guarda entera, como una unidad, nunca campo a campo | FR-001 |
| V2 | Ningún campo sensible queda legible en el almacenamiento de la app | FR-002 |
| V3 | Sobrevive al cierre y reinicio de la aplicación | FR-004 |
| V4 | Borrarla la elimina del almacén sin rastro recuperable | FR-005 |
| V5 | Si el almacén no está disponible, equivale a «no hay sesión» | FR-006 |
| V6 | Si es ilegible o corrupta, equivale a «no hay sesión» y se descarta | FR-007 |
| V7 | Una sesión heredada en claro se descarta sin leerse | FR-008, FR-009 |

V5 y V6 convergen deliberadamente en el mismo comportamiento: desde fuera, un almacén caído y
una credencial corrupta son indistinguibles, y en ambos casos lo correcto es arrancar sin
sesión en lugar de interrumpir.

### Estados

```text
        ┌──────────────┐  guardar   ┌───────────────┐
        │  Sin sesión  │ ─────────► │  Con sesión   │
        └──────────────┘            └───────────────┘
               ▲                            │
               └──────── borrar ────────────┘
               ▲
               │  almacén no disponible · credencial corrupta ·
               │  sesión heredada en claro · reinstalación en iOS
```

Solo dos estados observables. Todo lo que puede salir mal desemboca en «sin sesión»: nunca
hay un tercer estado de error que el resto de la app tenga que entender.

## Entidad: Rol

El nivel de acceso de la persona identificada. **Ya existe y no cambia**; se incluye porque
esta feature lo verifica por primera vez.

| Valor | Fichaje e historial | Inventario y equipo |
|---|---|---|
| Empleada | sí | **no** |
| Encargada | sí | sí |
| Administradora | sí | sí |

| # | Regla | Requisito |
|---|---|---|
| V8 | Inventario y equipo exigen encargada o administradora | FR-010 |
| V9 | Sin sesión se aplica el nivel más restringido | FR-011 |
| V10 | El rol llega dentro de la sesión; la app no lo decide ni lo deduce | principio VI |

V9 importa más de lo que parece: ante la duda, la app debe equivocarse hacia restringir de
más, nunca hacia permitir de más.

## Lo que esta feature NO modela

- **Inicio de sesión**: no hay backend. Ver «Deferred» en la spec.
- **Caducidad y renovación de credenciales**: los extremos no existen.
- **Datos de negocio**: materiales y fichajes son de las specs 3 y 4. Esta feature cubre
  credenciales, no la base de datos local.
