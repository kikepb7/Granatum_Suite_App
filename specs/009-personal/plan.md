# Implementation Plan: Alta del propietario y gestión del personal

**Branch**: `personal-feature` | **Date**: 2026-10-09 | **Spec**: [spec.md](./spec.md)

## Summary

Igualar la app al backend `2ec33d0`: registro solo del propietario (con código de arranque) en el
módulo de auth, y un módulo nuevo `feature/staff` para dar de alta personas en un paso, editar
fichas, desactivarlas y restablecer o dar acceso. Ver [research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20 · **Dependencies**: Ktor, Koin, Compose Multiplatform; sin
dependencias nuevas · **Storage**: ninguno · **Testing**: `commonTest` con `MockEngine` ·
**Platform**: Android e iOS · **Constraints**: contrato en `2ec33d0`; solo con conexión.

## Constitution Check

Evaluado contra la constitución **v1.1.2**.

| Principio | Estado |
|---|---|
| I · II | ✅ registro en `core` + `feature/auth`; personal en `feature/staff/{domain,data,presentation}` |
| III Offline-first | ✅ con matiz justificado (D3): datos personales sin caché |
| IV Errores | ✅ `AuthError` ampliado y `StaffError` en el dominio |
| VI Contrato | ✅ pin a `2ec33d0`; [contracts/personal-api.md](./contracts/personal-api.md) |
| VIII Tests | ✅ D10; el trinquete sube |
| IX UI | ✅ textos en `composeResources` |
| XI Build | ✅ sin dependencias nuevas |
| XII Paridad | ✅ |

**Resultado: PASA.**

## Fases

1. Contrato a `2ec33d0` y documentación.
2. Registro del propietario (datos, ViewModel, pantalla, puerta de sesión).
3. Módulo de personal: dominio, datos y tests.
4. Pantallas: lista, alta con credenciales, ficha con edición, baja y acceso.
5. Navegación, log de red, trinquete, README y CI en local.

## Complexity Tracking

Sin desviaciones.
