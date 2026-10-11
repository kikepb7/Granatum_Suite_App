# Implementation Plan: Entornos PRE y PRO, y usuarios demo en PRE

**Branch**: `entornos-feature` | **Date**: 2026-10-11 | **Spec**: [spec.md](./spec.md)

## Summary

Dos apps elegidas al compilar (`granatum.build=pre|pro`) con identificador y nombre propios; en
PRE, selector de entorno (Local, PRE, PRO, Demo) que cierra sesión y borra datos locales al
cambiar; usuarios demo en el módulo `:core:demo`, enlazado solo en PRE. Ver [research.md](./research.md).

## Constitution Check (v1.1.2)

| Principio | Estado |
|---|---|
| I · II | ✅ dominio (`AppEnvironment`, `EnvironmentRepository`) en `core/domain`; datos en `core/data/environment`; demo en `core/demo` |
| III Offline-first | ✅ los fichajes pendientes bloquean el cambio de entorno (SC-003) |
| VI Contrato | ✅ el backend demo respeta `docs/openapi.json` |
| VIII Tests | ✅ entorno, motor conmutable, login con entornos, backend demo; la CI verifica que PRO no lleva demo |
| IX UI | ✅ textos en `composeResources` |
| XI Build | ✅ sin dependencias nuevas; `:core:demo` reutiliza `ktor-client-mock` del catálogo |
| XII Paridad | ✅ Android (`applicationIdSuffix`, placeholders) e iOS (`Config.xcconfig`) |

**Resultado: PASA.** La regla anterior «ningún binario puede repuntarse» se mantiene para PRO; PRE
es interno.
