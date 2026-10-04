# Contrato — Configuración de construcción

**Feature**: `001-backend-env-config` · **Fecha**: 2026-10-05

Esta feature no expone ninguna API de red nueva. Su contrato es la frontera entre **quien
construye la app** y **el código que consume la configuración**: qué claves hay que
rellenar, qué se genera a partir de ellas y cómo falla cuando algo no cuadra.

## 1. Entrada — claves de `local.properties`

Fichero por máquina, nunca versionado. `local.properties.example` debe documentar todas
estas claves sin valores reales.

Nombres de clave alineados con Squadfy_KMM (`BASE_URL_HTTP`), con el sufijo de target solo
donde hace falta distinguir plataforma.

| Clave | Obligatoria | Ejemplo | Notas |
|---|---|---|---|
| `API_KEY` | sí, en todo flavor | `your-dev-api-key` | Ya existe. Cabecera `x-api-key` |
| `BASE_URL_HTTP` | no | `http://10.0.2.2:8080/api` | Override global. Gana sobre los valores por defecto de cualquier target |
| `BASE_URL_HTTP_IOS` | no | `http://localhost:8080/api` | Override solo del target iOS en el flavor `local` |
| `BASE_URL_HTTP_STAGING` | sí, con flavor `staging` | `https://staging.ejemplo/api` | **Debe** ser `https://` |
| `BASE_URL_HTTP_PROD` | sí, con flavor `prod` | `https://api.ejemplo/api` | **Debe** ser `https://` |
| `sdk.dir` | no | `/Users/tu/Library/Android/sdk` | Solo si AGP no detecta el SDK |

El flavor `local` **no exige ninguna clave de URL**: trae valores por defecto correctos para
cada plataforma, que es lo que permite clonar y arrancar sin configurar nada. Staging y
producción sí las exigen, porque no hay valor por defecto razonable ni seguro.

### Cadena de resolución

Tomada de Squadfy. Para cada clave, el primer valor que exista:

1. Propiedad de Gradle — `-PBASE_URL_HTTP=...`, útil para inyectar desde CI sin tocar ficheros.
2. Entrada en `local.properties`.
3. Valor por defecto del target y flavor.

### Valores por defecto del flavor `local`

| Target | Valor |
|---|---|
| Android | `http://10.0.2.2:8080/api` |
| iOS | `http://localhost:8080/api` |

Esta tabla es la corrección concreta al problema de iOS. Squadfy usa `10.0.2.2` también en
iOS y obliga a un override manual; aquí el target lo resuelve solo.

## 2. Selección de entorno

```bash
./gradlew :composeApp:assembleDebug -Pbuildkonfig.flavor=local
./gradlew :composeApp:assembleRelease -Pbuildkonfig.flavor=prod
```

Si se omite la propiedad, el flavor por defecto es `local`: es el caso del día a día y el
único que no puede causar daño si se equivoca.

## 3. Salida — campos generados

Generados por BuildKonfig en el paquete de `core/data`, disponibles en `commonMain`:

| Campo | Tipo | Contenido |
|---|---|---|
| `BuildKonfig.API_KEY` | `String` | La clave del flavor activo |
| `BuildKonfig.BASE_URL_HTTP` | `String` | La raíz de API ya resuelta para target y flavor |

`BASE_URL_HTTP` llega **ya resuelta**. El código que la consume no sabe en qué entorno está
ni en qué plataforma corre: no debe haber ninguna rama condicional sobre el entorno en
`commonMain`.

### Envoltorio y consumidores

`UrlConstants` se conserva como envoltorio fino, igual que en Squadfy (D10):

```kotlin
object UrlConstants {
    val BASE_URL_HTTP = BuildKonfig.BASE_URL_HTTP
}
```

Consecuencia: **`HttpClientExt.kt` no se modifica**. Sigue importando
`UrlConstants.BASE_URL_HTTP` y su lógica de construcción de rutas (líneas 134-136) queda
intacta. El único cambio en el fichero del envoltorio es que `const val` pasa a `val`,
porque un valor generado no es constante de compilación.

`BASE_URL_WS` desaparece del envoltorio por falta de consumidores (D8).

## 4. Contrato de fallo

Todos los fallos ocurren **en tiempo de configuración de Gradle**, nunca en ejecución. Una
construcción o produce un binario correctamente apuntado, o no produce binario.

| Situación | Comportamiento |
|---|---|
| Falta una clave obligatoria del flavor | Falla, nombrando la clave y el flavor |
| `staging` o `prod` con URL no `https://` | Falla, explicando que esos entornos exigen cifrado |
| Flavor desconocido | Falla, enumerando los tres válidos |
| Falta `API_KEY` | Falla. Comportamiento ya vigente, se conserva |

Ningún caso debe recurrir a un valor por defecto silencioso. Un binario apuntando al
servidor equivocado es peor que una construcción fallida.

## 5. Dependencia externa: comprobación de salud

La única llamada de red que esta feature ejercita.

| | |
|---|---|
| Método y ruta | `GET {API_BASE_URL}` + extremo de salud del backend |
| Éxito | Respuesta correcta → criterio SC-002 cumplido |
| Fallo | Mensaje comprensible en pantalla, nunca pantalla en blanco ni cierre |

Esta feature **no** depende de `docs/openapi.json`, al contrario que las specs 3 y 5. Usa
únicamente la comprobación de salud, así que puede completarse mientras el contrato de API
siga sin publicarse.
