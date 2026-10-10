<!--
Rellena cada sección y borra lo que no aplique. Título: "PROJECT | <qué hace, en inglés>".
Rama de origen: `<funcionalidad>-feature`, creada desde `main`.
-->

## Resumen

<!-- Qué cambia y por qué, en dos o tres frases. Para quien no ha visto el código. -->

## Tipo de cambio

- [ ] Nueva funcionalidad
- [ ] Corrección de un error
- [ ] Refactorización (sin cambio de comportamiento)
- [ ] Contrato de API (`docs/openapi.json` / `docs/openapi.pin`)
- [ ] Build, CI o configuración
- [ ] Documentación o specs

## Spec relacionada

<!-- Enlace a specs/<NNN-nombre>/ y tareas que cubre (p. ej. T001–T012). "No aplica" si no hay spec. -->

- Spec:
- Tareas:

## Cambios principales

<!-- Lista por módulo o capa: domain, data, presentation, composeApp, core/*, build-logic… -->

-

## Contrato con el backend

- [ ] No cambia ninguna ruta, campo ni código de error usado por la app
- [ ] Usa rutas nuevas o cambiadas: el contrato está fijado al commit del backend `______` con `scripts/sync-openapi.sh`

<!-- Si el backend tiene alguna carencia o regla que afecte a esta PR, descríbela aquí. -->

## Cómo probarlo

<!-- Pasos concretos y resultado esperado. Indica el entorno: local (backend en tu máquina), demo
(-Pbuildkonfig.flavor=demo) o staging. Enlaza el quickstart de la spec si existe. -->

1.

## Capturas o vídeo

<!-- Obligatorio si cambia la interfaz. Android e iOS, tema claro y oscuro si aplica. -->

| Android | iOS |
|---------|-----|
|         |     |

## Checklist

**Calidad**

- [ ] CI completa en local en verde: `./gradlew ktlintCheck testDebugUnitTest koverVerify :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64 iosSimulatorArm64Test`
- [ ] Tests nuevos o actualizados para lo que cambia (dominio, datos con `MockEngine`, ViewModels)
- [ ] Trinquete de cobertura (`granatum.coverage.minLine`) subido si la cobertura medida sube

**Arquitectura y UI**

- [ ] El código compartido está en `commonMain`; lo específico de plataforma, en `androidMain`/`iosMain`
- [ ] Funciona igual en Android e iOS (paridad)
- [ ] Sin textos visibles en el código: todo en `composeResources`
- [ ] Los errores del servidor se muestran con mensajes comprensibles

**Seguridad y privacidad**

- [ ] Sin secretos, credenciales ni `local.properties` en el diff
- [ ] Ningún dato personal o sensible (tokens, contraseñas, DNI, datos fiscales) llega a los logs
- [ ] Si se añaden permisos de plataforma (cámara, fotos…), están justificados en el manifiesto o en `Info.plist`

**Documentación**

- [ ] README y specs actualizados (tareas marcadas en `tasks.md`)

## Riesgos y pendientes

<!-- Qué podría romperse, migraciones de base de datos, decisiones abiertas, lo que queda fuera
de esta PR y la verificación en dispositivo que falte. -->

-
