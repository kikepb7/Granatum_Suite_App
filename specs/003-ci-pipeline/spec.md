# Feature Specification: Pipeline de integración continua

**Feature Branch**: `ci-pipeline-feature`

**Created**: 2026-10-08

**Status**: Draft

**Input**: Cerrar las brechas 1 y 2 de la constitución: no hay integración continua, ni
análisis estático, ni medición de cobertura.

> **Por qué esta feature va antes que las demás.** Toda la verificación de las specs 1 y 2
> ha sido manual, y en las dos un build en verde convivió con un defecto real: en la spec 1,
> una configuración que iOS ignoraba en silencio; en la spec 2, un almacén seguro que no
> guardaba. Sin una red automática, cada cambio futuro depende de que alguien se acuerde de
> repetir esas comprobaciones a mano.

> **Estado verificado el 2026-10-08.** El repositorio es público, de modo que la ejecución
> automática en ambas plataformas no tiene coste. Hoy no existe ninguna automatización. El
> proyecto entero contiene **un único test**, y es el ejemplo de la plantilla: comprueba que
> 1 + 2 = 3. No hay tests de dispositivo. La cobertura real es prácticamente nula.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Nada llega a main sin compilar en las dos plataformas (Priority: P1)

Quien abre una propuesta de cambio sabe, sin hacer nada, si ese cambio rompe Android, iOS o
ambos. Quien revisa no tiene que descargarlo y compilarlo para averiguarlo.

**Why this priority**: Es la garantía mínima que exige el principio XII y la que más se ha
echado en falta. Las roturas de iOS son la regresión más frecuente en un proyecto
multiplataforma, porque el día a día ocurre en Android.

**Independent Test**: Abrir una propuesta de cambio que rompa deliberadamente la compilación
de iOS y comprobar que se marca como fallida sin intervención humana.

**Acceptance Scenarios**:

1. **Given** una propuesta de cambio contra main, **When** se abre o se actualiza, **Then** se
   comprueba automáticamente que compilan Android e iOS.
2. **Given** un cambio que rompe solo iOS, **When** se comprueba, **Then** el resultado es
   fallido aunque Android compile.
3. **Given** un cambio integrado en main, **When** se integra, **Then** se vuelve a comprobar
   sobre el resultado de la integración.

---

### User Story 2 - Un fallo es imposible de pasar por alto (Priority: P2)

Quien mira una propuesta de cambio ve de un vistazo si todo ha ido bien, y si no, qué etapa
falló, sin tener que abrir registros largos.

**Why this priority**: Una comprobación cuyo fallo queda enterrado equivale a no tenerla. El
principio VIII prohíbe integrar con la verificación en rojo, y eso solo funciona si el rojo
es evidente.

**Independent Test**: Provocar el fallo de una sola etapa y comprobar que el resultado global
es fallido y que el resumen señala exactamente esa etapa.

**Acceptance Scenarios**:

1. **Given** que todas las etapas pasan, **When** se mira el resultado, **Then** es un único
   indicador en verde.
2. **Given** que falla una sola etapa, **When** se mira el resultado, **Then** el indicador
   global es rojo y el resumen nombra la etapa que falló.
3. **Given** una etapa cancelada o interrumpida, **When** se mira el resultado, **Then** cuenta
   como fallo, no como éxito.

---

### User Story 3 - La calidad del código se ve sin tener que buscarla (Priority: P3)

Quien revisa encuentra, junto a cada propuesta de cambio, el resultado del análisis de estilo
y cuánto del código está cubierto por tests, junto al artefacto instalable de Android.

**Why this priority**: No bloquea nada hoy, pero es lo que permite que la cobertura suba con
el tiempo en vez de quedarse donde está. Lo que no se mide no mejora.

**Independent Test**: Revisar una propuesta de cambio y encontrar, sin buscarlos, el
resultado del análisis de estilo, la cifra de cobertura y el instalable descargable.

**Acceptance Scenarios**:

1. **Given** una propuesta de cambio comprobada, **When** se revisa, **Then** se ve el
   porcentaje de cobertura del código.
2. **Given** esa misma propuesta, **When** se revisa, **Then** se ve el resultado del análisis
   de estilo, aunque no impida integrar.
3. **Given** esa misma propuesta, **When** se revisa, **Then** el instalable de Android está
   disponible para descargar y probar.

---

### Edge Cases

- **Propuestas de cambio desde un fork.** Al ser un repositorio público, cualquiera puede
  proponer cambios. La comprobación no debe exponer ningún secreto a código ajeno.
- **Comprobaciones superpuestas.** Si alguien actualiza una propuesta varias veces seguidas,
  las comprobaciones antiguas deben cancelarse en lugar de acumularse.
- **Configuración local ausente.** La compilación exige un fichero de configuración local que
  nunca se versiona. La automatización debe poder compilar sin él y sin secretos reales.
- **Un test que no existe.** Una etapa de tests que no encuentra nada que ejecutar no debe
  confundirse con una etapa que pasa.
- **Ejecución que se cuelga.** Una etapa bloqueada debe abortar en un tiempo acotado en lugar
  de ocupar recursos indefinidamente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Cada propuesta de cambio contra main MUST comprobarse automáticamente al abrirse
  y en cada actualización.
- **FR-002**: Cada integración en main MUST comprobarse automáticamente sobre el resultado.
- **FR-003**: La comprobación MUST verificar que la aplicación compila para Android.
- **FR-004**: La comprobación MUST verificar que el código compartido compila para iOS.
- **FR-005**: La comprobación MUST ejecutar todos los tests existentes y fallar si alguno
  falla.
- **FR-006**: La comprobación MUST ejecutar el análisis de estilo del código y publicar su
  resultado. Ese resultado no bloquea la integración en esta versión.
- **FR-007**: La comprobación MUST medir la cobertura de los tests y publicar el porcentaje.
- **FR-008**: La cobertura MUST funcionar como un trinquete: la puerta se fija en la cobertura
  medida al activar la comprobación, y ningún cambio puede dejarla por debajo. Subir el umbral
  es libre; bajarlo exige enmienda de la constitución.
- **FR-009**: La etapa de tests de dispositivo MUST NOT existir mientras no haya ningún test de
  dispositivo que ejecutar. Se incorpora en la misma propuesta de cambio que añada el primero.
- **FR-010**: La comprobación MUST producir un único resultado global, que es fallido si
  cualquier etapa falla, se cancela o se interrumpe.
- **FR-011**: El resultado MUST incluir un resumen que nombre qué etapas pasaron y cuáles no.
- **FR-012**: El instalable de Android MUST quedar disponible para descargar desde cada
  comprobación.
- **FR-013**: Las comprobaciones antiguas de una misma propuesta MUST cancelarse al llegar una
  actualización.
- **FR-014**: La comprobación MUST compilar sin secretos reales y sin la configuración local de
  ningún desarrollador.
- **FR-015**: Las propuestas de cambio procedentes de terceros MUST poder comprobarse sin
  acceso a ningún secreto.
- **FR-016**: Cada etapa MUST tener un tiempo máximo de ejecución.
- **FR-017**: Una etapa de tests que no encuentre ningún test que ejecutar MUST distinguirse de
  una que los ejecuta y pasan.

### Key Entities

- **Comprobación**: la ejecución automática asociada a una propuesta de cambio o a una
  integración. Se compone de etapas y produce un resultado global.
- **Etapa**: una verificación concreta —compilar Android, compilar iOS, tests, estilo,
  cobertura—. Pasa, falla o se cancela. Unas bloquean la integración y otras solo informan.
- **Resultado global**: la conclusión de la comprobación entera. Solo es éxito si todas las
  etapas bloqueantes pasan.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las propuestas de cambio contra main se comprueban sin intervención
  humana.
- **SC-002**: Un cambio que rompe la compilación de cualquiera de las dos plataformas se marca
  como fallido el 100 % de las veces.
- **SC-003**: Quien revisa identifica qué etapa falló sin abrir ningún registro detallado.
- **SC-004**: La comprobación completa termina en menos de 30 minutos en el caso habitual.
- **SC-005**: El porcentaje de cobertura queda visible en cada propuesta de cambio desde la
  primera ejecución.
- **SC-006**: Ningún secreto queda expuesto en propuestas de cambio procedentes de terceros.

## Assumptions

- El repositorio es público, así que la ejecución automática en ambas plataformas, incluida
  la que exige un entorno de macOS para iOS, no tiene coste.
- Se usará el sistema de automatización que ofrece la propia plataforma donde vive el
  repositorio. Es el mismo que usa el proyecto hermano de referencia.
- El análisis de estilo arranca en modo informativo: el código es anterior a esa herramienta
  y activarlo como bloqueante obligaría a corregir todas sus violaciones dentro de esta
  feature. Hacerlo bloqueante es una decisión posterior, una vez saneado el código.
- La compilación usa valores de configuración ficticios, porque ningún secreto real hace falta
  para compilar ni para los tests actuales.
- La protección de la rama main —exigir que la comprobación pase antes de integrar— se
  configura en la plataforma por quien administra el repositorio. Esta feature deja la
  comprobación lista para ello, pero no puede activar esa protección por sí misma.

## Dependencies

- Ninguna sobre el backend.
- **FR-008 y FR-009 contradicen la constitución v1.0.0**, que exige una puerta de cobertura
  que arranca en el 20 % y una etapa obligatoria de tests de dispositivo en tres versiones de
  Android. Ambas decisiones las tomó el propietario del producto el 2026-10-08 y requieren
  enmendar la constitución antes de implementar esta feature, con su propio procedimiento.
  Sin esa enmienda, la feature incumpliría la constitución vigente.

## Out of Scope

- Escribir tests más allá de lo imprescindible para que la comprobación tenga algo real que
  ejecutar.
- Corregir las violaciones de estilo existentes.
- Firma y publicación de versiones de release.
- Despliegue a tiendas.
