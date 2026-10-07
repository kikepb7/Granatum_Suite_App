# Quickstart — Validación de la feature

**Feature**: `003-ci-pipeline` · **Fecha**: 2026-10-08

Escenarios que demuestran que la CI hace lo que promete. Las puertas están en
[contracts/ci-workflow.md](./contracts/ci-workflow.md).

## Antes de subir: en local

### Escenario 1 — Compila sin `local.properties`

```bash
mv local.properties /tmp/local.properties.bak
./gradlew :composeApp:assembleDebug -PAPI_KEY=ci-placeholder
mv /tmp/local.properties.bak local.properties
```

**Esperado**: compila. Sin `-PAPI_KEY` debe fallar nombrando la clave que falta (FR-014).

### Escenario 2 — ktlint y Kover en todos los módulos

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest koverXmlReport koverVerify -PAPI_KEY=ci-placeholder
```

**Esperado**: ktlint publica informes y no falla aunque haya violaciones. Kover genera el
informe agregado y la verificación pasa con el umbral de `gradle.properties`.

### Escenario 3 — iOS completo

```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test -PAPI_KEY=ci-placeholder
```

**Esperado**: enlaza el framework y el test pasa en el simulador.

## Después de subir: en GitHub Actions

### Escenario 4 — PR en verde

Abrir la PR de esta rama contra `main`.

**Esperado**: los cinco jobs terminan; `summary` en verde; el resumen muestra tests ejecutados
y cobertura; el APK se puede descargar (SC-001, SC-003, SC-005).

### Escenario 5 — Romper iOS deja la CI en rojo

En una rama de prueba, introducir un error que solo afecte a `iosMain` y abrir una PR.

**Esperado**: `build-ios` falla, `summary` falla, y el resumen señala el job (SC-002). Android
sigue en verde, lo que demuestra que la CI no se conforma con una sola plataforma.

### Escenario 6 — Sin tests no es verde

En una rama de prueba, borrar el único test.

**Esperado**: `unit-tests` falla por haber ejecutado cero tests (FR-017), no pasa en silencio.

### Escenario 7 — Bajar la cobertura falla

Cuando haya tests reales y el umbral sea mayor que cero: una PR que reduzca la cobertura por
debajo de `granatum.coverage.minLine` debe fallar. Con el umbral inicial de 0 % este escenario
**no es verificable todavía en una PR real**, y conviene dejarlo dicho en lugar de darlo por
probado: con 0 % de cobertura, nada puede bajar.

Lo que **sí** se verificó en local el 2026-10-08 es el mecanismo: con el umbral en 0 la
verificación pasa, y forzándolo a 1 falla con *«Rule violated: lines covered percentage is
0.000000, but expected minimum is 1»*. La puerta existe y muerde; solo falta cobertura que
proteger.

### Escenario 8 — Cancelación

Hacer dos pushes seguidos a la rama de la PR.

**Esperado**: la primera ejecución se cancela al llegar la segunda (FR-013).

## Paso manual del administrador

La CI informa, pero solo **bloquea** fusiones si se exige en los ajustes del repositorio:

> Settings → Branches → Branch protection rules → `main` → *Require status checks to pass
> before merging* → marcar **`summary`**.

Sin este paso, el principio VIII es una convención, no una garantía.

## Lista de verificación final

| | Escenario | Criterio |
|---|---|---|
| ☐ | Compila sin configuración local | FR-014 |
| ☐ | ktlint y Kover en todos los módulos | FR-006, FR-007 |
| ☐ | iOS enlaza y pasa tests | FR-004 |
| ☐ | PR en verde con resumen y APK | SC-001, SC-003, SC-005 |
| ☐ | Romper iOS deja la CI en rojo | SC-002 |
| ☐ | Sin tests no es verde | FR-017 |
| ☐ | Bajar cobertura falla | FR-008 — no verificable con umbral 0 % |
| ☐ | Cancelación de ejecuciones antiguas | FR-013 |
| ☐ | Protección de rama activada | paso manual |
