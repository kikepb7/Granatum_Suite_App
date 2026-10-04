# Feature Specification: Identidad de app y conexión por entorno

**Feature Branch**: `001-backend-env-config`

**Created**: 2026-10-05

**Status**: Draft

**Input**: Convertir el esqueleto en la app Granatum Suite, conectable a tres entornos de
backend elegibles al compilar, con producción solo por HTTPS, retirando los flujos de
autenticación heredados que el producto no usa y con un README propio.

> **Nota de alcance.** La redacción original de esta spec asumía un esqueleto sin renombrar.
> Verificado contra el código el 2026-10-05, la parte de identidad **ya está hecha**:
> `rootProject.name = GranatumSuite`, `applicationId` / bundle id / namespace = `com.granatum.app`,
> cero paquetes `com/template/*`, y minSdk 26 / targetSdk 36 / versionName 1.0 fijados en el
> catálogo de versiones. `feature/example` tampoco existe. Lo que queda es lo descrito aquí.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Compilar apuntando a un entorno concreto (Priority: P1)

Quien construye la app elige, en el momento de compilar, contra qué backend hablará el
binario: el que corre en su propia máquina, el de pruebas compartido o el de producción.
No toca código para cambiar de uno a otro.

**Why this priority**: Sin esto no hay forma de probar la app contra el backend local ni de
publicar una versión que apunte a producción. Todo lo demás depende de poder conectar.

**Independent Test**: Compilar dos binarios seguidos, uno local y otro de producción, y
comprobar que cada uno llama al backend que le corresponde, sin haber editado ningún fichero
de código fuente entre ambos.

**Acceptance Scenarios**:

1. **Given** el backend corriendo en la máquina del desarrollador, **When** se compila e
   instala la app en el emulador de Android, **Then** la app alcanza ese backend y su
   comprobación de salud responde correctamente.
2. **Given** el mismo backend local, **When** se compila y arranca en el simulador de iOS,
   **Then** la app alcanza ese mismo backend, pese a que la dirección que debe usar cada
   plataforma para referirse a la máquina anfitriona es distinta.
3. **Given** un binario compilado para producción, **When** se inspecciona a qué servidor
   apunta, **Then** apunta al de producción y no al local ni al de pruebas.

---

### User Story 2 - Producción nunca habla en claro (Priority: P2)

Una persona usuaria de la app publicada tiene la garantía de que sus credenciales y sus
fichajes viajan cifrados, sin depender de que nadie se acuerde de revisar una constante
antes de publicar.

**Why this priority**: El producto maneja credenciales laborales y registros de jornada.
Una publicación accidental contra un servidor sin cifrar expondría datos personales. Es
barato de garantizar ahora y caro de descubrir después.

**Independent Test**: Intentar construir un binario de producción configurado con una
dirección sin cifrar y comprobar que la construcción se detiene en lugar de producir un
binario instalable.

**Acceptance Scenarios**:

1. **Given** una configuración de producción con una dirección sin cifrar, **When** se
   intenta construir, **Then** la construcción falla con un mensaje que explica el motivo.
2. **Given** un binario de producción ya construido, **When** se revisa su configuración,
   **Then** no contiene ninguna dirección sin cifrar.
3. **Given** un binario de depuración apuntando al backend local, **When** se ejecuta,
   **Then** sí se permite la conexión sin cifrar, porque el destino es la propia máquina.

---

### User Story 3 - La app no ofrece caminos que no existen (Priority: P2)

Quien usa la app no encuentra opciones para registrarse, verificar su correo o recuperar su
contraseña, porque en Granatum las cuentas las crea la empresa y esos caminos no llevan a
ninguna parte.

**Why this priority**: Una opción visible que falla al pulsarla genera incidencias de
soporte y desconfianza. Además, mantener código de flujos que nadie usa obliga a razonar
sobre ellos en cada cambio de autenticación.

**Independent Test**: Recorrer la app desde la pantalla inicial buscando cualquier acceso a
registro, verificación de correo o recuperación de contraseña, y no encontrar ninguno.

**Acceptance Scenarios**:

1. **Given** la pantalla de acceso, **When** la persona la examina, **Then** no ve ninguna
   opción de crear cuenta ni de recuperar contraseña.
2. **Given** el código de la aplicación, **When** se busca el soporte de esos flujos,
   **Then** no queda ni la lógica, ni las rutas, ni las pantallas, ni los tests asociados.
3. **Given** la app tras la limpieza, **When** una persona accede con sus credenciales,
   cambia su contraseña y cierra sesión, **Then** las tres operaciones siguen funcionando.

---

### User Story 4 - Un desarrollador nuevo arranca la app sin ayuda (Priority: P3)

Alguien que se incorpora al equipo clona el repositorio, sigue el documento de inicio y
consigue la app funcionando contra el backend local sin preguntar a nadie.

**Why this priority**: Importa, pero el equipo puede avanzar con transmisión oral mientras
tanto. Es lo primero que se degrada y lo último que bloquea.

**Independent Test**: Pedir a alguien ajeno al proyecto que siga únicamente el documento de
inicio, cronometrando y anotando en qué punto se atasca.

**Acceptance Scenarios**:

1. **Given** un repositorio recién clonado, **When** se sigue el documento de inicio paso a
   paso, **Then** la app arranca contra el backend local sin consultar a nadie.
2. **Given** ese mismo documento, **When** se busca qué valores hay que configurar en local,
   **Then** están todos enumerados, con su significado y sin ningún valor real.

---

### Edge Cases

- **Falta un valor de configuración.** Si quien compila no ha definido la dirección del
  entorno elegido, la construcción debe detenerse con un mensaje claro, nunca recurrir
  silenciosamente a un valor por defecto que apunte a un servidor equivocado.
- **La dirección del anfitrión difiere por plataforma.** El emulador de Android y el
  simulador de iOS no se refieren igual a la máquina del desarrollador. El entorno local
  debe resolver correctamente en ambos sin que haya que tocar nada entre una ejecución y
  otra.
- **El backend local no responde.** La app debe mostrar un mensaje comprensible en lugar de
  quedarse en blanco o cerrarse.
- **Sesión previa tras la limpieza.** Una instalación anterior que tuviera sesión abierta no
  debe quedar en un estado inconsistente al desaparecer los flujos retirados.
- **Configuración de producción apuntando a pruebas.** Un error humano al rellenar los
  valores no debe pasar inadvertido: conviene que cada entorno sea identificable en el
  binario resultante.
- **Nombre truncado en el lanzador.** «Granatum Suite» son 14 caracteres y iOS recorta
  alrededor de los 12 en algunas rejillas. Si se comprueba que se ve cortado, se fija un
  nombre corto solo para iOS sin cambiar el de Android.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir elegir entre tres entornos —local, pruebas y
  producción— en el momento de construir la aplicación.
- **FR-002**: La dirección del backend MUST inyectarse en el binario durante la
  construcción. NO puede figurar como valor fijo en el código fuente.
- **FR-003**: Los entornos de pruebas y producción MUST resolver exclusivamente conexiones
  cifradas.
- **FR-004**: Las conexiones sin cifrar MUST admitirse únicamente en construcciones de
  depuración y contra la máquina del propio desarrollador.
- **FR-005**: Los valores concretos de cada entorno MUST tomarse de la configuración local
  de cada máquina y NO MUST incorporarse al control de versiones.
- **FR-006**: El repositorio MUST incluir un fichero de ejemplo que enumere todas las claves
  de configuración necesarias, con su significado y sin valores reales.
- **FR-007**: La construcción MUST fallar de forma explícita si falta una clave obligatoria
  del entorno seleccionado.
- **FR-008**: El entorno local MUST funcionar tanto en el emulador de Android como en el
  simulador de iOS, resolviendo en cada caso la dirección que corresponde a la máquina
  anfitriona.
- **FR-009**: La aplicación NO MUST ofrecer registro público, verificación de correo ni
  recuperación de contraseña, ni en la interfaz ni en el código.
- **FR-010**: La aplicación MUST conservar el acceso con credenciales, el cambio de
  contraseña y el cierre de sesión.
- **FR-011**: La retirada de los flujos no usados MUST dejar el código sin restos huérfanos:
  ni rutas, ni estructuras de datos, ni pantallas, ni tests sin uso.
- **FR-012**: El documento de inicio del repositorio MUST describir Granatum Suite —qué es,
  qué necesita, cómo configurar los entornos y cómo arrancar en ambas plataformas— y MUST
  dejar de describir la plantilla de la que partió el proyecto.
- **FR-013**: La aplicación MUST mostrarse en el lanzador del dispositivo como
  «Granatum Suite».
- **FR-014**: La aplicación MUST ofrecer su interfaz únicamente en español en el primer
  lanzamiento. Los textos MUST residir en recursos, de forma que incorporar un segundo
  idioma más adelante no obligue a tocar las pantallas.
- **FR-015**: La aplicación MUST presentarse con un icono provisional —la inicial de la
  marca sobre color plano— sustituible por el definitivo sin cambios estructurales.

### Key Entities

- **Entorno**: el destino con el que habla la aplicación. Tres valores posibles —local,
  pruebas y producción—, cada uno con su dirección de backend y su exigencia de cifrado.
  Se fija al construir y no cambia en ejecución.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Cambiar el entorno al que apunta la app no requiere editar ningún fichero de
  código fuente.
- **SC-002**: La app arranca en emulador de Android y en simulador de iOS y completa con
  éxito la comprobación de salud contra el backend local.
- **SC-003**: Ninguna construcción destinada a personas usuarias contiene direcciones sin
  cifrar.
- **SC-004**: Una construcción a la que le falte un valor de configuración obligatorio falla
  el 100 % de las veces, en lugar de producir un binario mal apuntado.
- **SC-005**: No existe ningún recorrido dentro de la app que lleve a registro, verificación
  de correo o recuperación de contraseña.
- **SC-006**: Alguien ajeno al proyecto consigue la app funcionando contra el backend local
  en menos de 30 minutos siguiendo únicamente el documento de inicio.

## Assumptions

- El backend local escucha en el puerto 8080 y expone un extremo de comprobación de salud.
  Es lo que refleja la configuración actual del repositorio.
- Las direcciones de pruebas y producción las facilita el equipo de backend. La aplicación
  no las inventa, en coherencia con el principio VI de la constitución.
- Las cuentas de persona usuaria las crea la empresa. Por eso se retiran los flujos de
  autoservicio en lugar de dejarlos ocultos.
- El guardado seguro de credenciales en el almacén del sistema pertenece a la spec 2 y
  queda fuera de aquí, aunque sea una brecha abierta de la constitución.
- El proyecto solo tiene como objetivo Android e iOS. No hay escritorio ni web.
- Existe ya un mecanismo de inyección de configuración en la construcción, de modo que esta
  feature lo aprovecha en lugar de introducir uno nuevo.

## Dependencies

- **Spec 2 (acceso y navegación por roles)** consume la configuración de entorno definida
  aquí, y hereda `login`, `changePassword` y `logout` tal y como quedan tras la limpieza.
- Esta feature NO depende del contrato de API publicado, a diferencia de las specs 3 y 5,
  porque solo usa la comprobación de salud. Puede avanzar mientras ese contrato no exista.

## Out of Scope

- El acceso con credenciales y la navegación condicionada por rol (spec 2).
- El guardado de credenciales en el almacén seguro del sistema (spec 2).
- Inventario (spec 3), fichaje (spec 4) y panel de encargado (spec 5).
- Cualquier renombrado de identidad: ya está hecho y verificado.
