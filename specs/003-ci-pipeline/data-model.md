# Phase 1 — Modelo de datos

**Feature**: `003-ci-pipeline` · **Fecha**: 2026-10-08

La feature no persiste datos de producto. Sus entidades son las de la propia comprobación.

## Comprobación

Una ejecución del workflow, asociada a una PR contra `main`, a un push a `main` o a un
lanzamiento manual.

| Campo | Valores |
|---|---|
| Disparador | PR contra `main` · push a `main` · manual |
| Etapas | las cinco de la tabla siguiente |
| Resultado global | éxito · fallo |

**Regla de concurrencia**: una comprobación nueva sobre la misma rama cancela la anterior
(FR-013).

## Etapa

| Etapa | Bloquea | Pasa si… |
|---|---|---|
| Análisis estático | **no** | siempre; publica las violaciones encontradas |
| Tests + cobertura | sí | todos los tests pasan, se ejecutó al menos uno y la cobertura ≥ trinquete |
| Build Android | sí | el APK de debug se genera y se publica |
| Build + tests iOS | sí | el framework enlaza y los tests pasan en el simulador |
| Resumen | sí | ninguna etapa bloqueante falló ni se canceló |

### Estados

```text
pendiente → en curso → éxito
                    ↘ fallo
                    ↘ cancelada   (cuenta como fallo, FR-010)
                    ↘ tiempo agotado (cuenta como fallo, FR-016)
```

## Resultado global

Éxito **solo** si todas las etapas bloqueantes terminan en éxito. Cancelada y tiempo agotado
son fallo, no éxito ni neutro: un fallo silencioso es exactamente lo que la historia de
usuario 2 quiere impedir.

## Umbral de cobertura (trinquete)

| | |
|---|---|
| Dónde vive | una línea de `gradle.properties`: `granatum.coverage.minLine` |
| Valor inicial | cobertura de líneas medida al activar la CI, redondeada hacia abajo |
| Puede subir | sí, en cualquier PR |
| Puede bajar | solo con enmienda de la constitución (principio VIII) |
