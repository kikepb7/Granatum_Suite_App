# Phase 0 — Investigación: facturación contra el backend

**Feature**: `008-facturacion` · **Fecha**: 2026-10-09

Verificado contra `docs/openapi.json` (backend `d1857ad`), el código del módulo del backend
(`features/invoices`, idéntico en `main` y `develop`) y su `specs/004-invoices`.

## D1 — Solo con conexión, sin caché local

**Decisión**: facturación no guarda nada en el móvil. Lee y escribe directamente contra el
servidor; sin conexión muestra un estado de error con «Reintentar».

**Justificación**: son datos fiscales de terceros (NIF, importes) y documentos de hasta 10 MB; el
servidor es el único que valida (avisos, duplicados, trimestres) y no ofrece idempotencia. Se usa
en la oficina, con cobertura. Principio III: offline-first donde aporta; aquí una caché solo
añadiría datos sensibles en el dispositivo y lecturas desactualizadas de algo que cambia en
segundo plano (el reconocimiento).

**Alternativa descartada**: caché Room como en inventario; el reconocimiento asíncrono la
invalidaría constantemente.

## D2 — Importes en céntimos

**Decisión**: `Money` es un `Long` de céntimos. Se parsea de texto con punto (servidor) o coma
(teclado), máximo 10 enteros y 2 decimales, y se serializa como `"1234.56"`. El tipo de IVA usa el
mismo tipo (porcentaje con dos decimales).

**Por qué**: KMP no tiene `BigDecimal` en `commonMain` y la coma flotante no vale para dinero.
10¹² céntimos cabe de sobra en un `Long`. `ResumenFactura.total` llega como número JSON: se lee
como texto del `JsonPrimitive` para no pasar por `Double`.

## D3 — Avisos: el servidor manda

**Decisión**: la app valida en local solo el formato (longitudes, decimales, tipo 0–100). Los
avisos de negocio (obligatorio, NIF, cuadre, duplicada, trimestre…) los calcula el servidor en cada
guardado y la app los muestra por campo. Como ayuda, la app enseña el cuadre en vivo: total menos
(líneas − retenciones).

**Por qué**: replicar el validador (NIF por dígito de control, duplicados) duplicaría reglas que el
backend puede cambiar; principio VI.

## D4 — Concurrencia: versión en el cuerpo

Todo `PUT`, `confirmar` y `descartar` lleva `version`. Ante `409 VERSION_DESACTUALIZADA` la app
recarga la factura y avisa; los cambios locales no guardados se descartan tras avisar (el
reconocimiento en segundo plano también sube la versión).

## D5 — Confirmar = guardar y confirmar

Si hay cambios sin guardar, «Confirmar» primero guarda (`PUT`) y, con la versión nueva y sin
avisos bloqueantes, confirma. Así nunca se confirma algo distinto de lo que se ve.

## D6 — Sondeo del reconocimiento

**Decisión**: mientras la lista o el detalle muestren facturas `PENDIENTE_RECONOCER`, la app
consulta cada 5 s durante como mucho 2 minutos desde la última subida o petición de reconocer, y
después solo al refrescar.

**Por qué**: el backend no tiene push (lo dice su spec); el reconocimiento tarda segundos y se
reintenta cada 5 min si falla.

## D7 — Ficheros: selector propio por plataforma, sin dependencias

**Decisión**: `core/presentation` gana `documentpicker` (`expect`/`actual`):
- **Android**: `TakePicture` con `FileProvider` para la cámara, `PickMultipleVisualMedia` para la
  galería y `OpenMultipleDocuments` (`application/pdf`, `image/*`) para archivos.
- **iOS**: `UIImagePickerController` para la cámara (`NSCameraUsageDescription`), `PHPicker` para
  la galería y `UIDocumentPickerViewController` para archivos.

La conversión se hace en la plataforma: HEIC/HEIF y cualquier imagen que no sea JPEG/PNG/WebP pasan
a JPEG; una imagen de más de 10 MB se reduce (lado mayor 3000 px, calidad 85). PDF y WebP se envían
tal cual, comprobando el tamaño.

**Alternativa descartada**: librerías como FileKit o Peekaboo: añaden dependencias por algo que son
unas 300 líneas; principio XI.

## D8 — Abrir y compartir ficheros

`core/presentation` gana `fileopener` (`expect`/`actual`) que recibe bytes, nombre y tipo:
- **Android**: escribe en `cacheDir/shared`, lo expone con `FileProvider` y lanza `ACTION_VIEW`
  (o `ACTION_SEND` para compartir).
- **iOS**: escribe en el directorio temporal y presenta `UIActivityViewController` (que también
  previsualiza PDF).

Las imágenes del original se ven dentro de la app con Coil a partir de los bytes.

## D9 — Subida multipart

`submitFormWithBinaryData` de Ktor con una parte `ficheros` por documento, cada una con su
`Content-Type` y nombre. Respuesta `202` con un resultado por posición (1-based).

## D10 — Errores del módulo

No están en el contrato. Del código del backend: `EMPRESA_SIN_CONFIGURAR` (404/409),
`NIF_INVALIDO` (422), `FACTURA_NOT_FOUND` (404), `VERSION_DESACTUALIZADA`, `ESTADO_NO_PERMITIDO`,
`TRIMESTRE_CERRADO`, `TRIMESTRE_ABIERTO`, `TRIMESTRE_CON_PENDIENTES`, `FACTURA_DUPLICADA`,
`RECONOCIMIENTO_NO_DISPONIBLE` (409), `FACTURA_INCOHERENTE`, `PERIODO_INVALIDO` (422), además de
los comunes `VALIDACION` (400), `PETICION_DEMASIADO_GRANDE` (413), `FORBIDDEN` (403) y
`DEMASIADAS_PETICIONES` (429).

## D11 — Navegación: la cuenta sale de la barra inferior

Con facturación, la administración tendría seis destinos (Fichar, Historial, Inventario, Equipo,
Facturación, Cuenta). **Decisión**: la cuenta pasa a un botón con la inicial en la barra superior
de las pantallas principales, mediante un `CompositionLocal` que `AppTopBar` lee cuando no hay
botón de volver; así ningún módulo de feature cambia de firma. La barra inferior se oculta si solo
queda un destino (representante).

## D12 — Desajustes contrato / código (a favor del código)

1. `ResumenFactura` es plano (`emisorNombre`, `emisorNif`…), como en `openapi.json`.
2. `ResumenFactura.total` es número JSON; el resto de importes son texto.
3. `valoresAnteriores` del historial es un mapa libre: se muestra como pares campo–valor.
4. El `PUT` de un borrador no comprueba el trimestre cerrado; el fallo llega al confirmar
   (`TRIMESTRE_CERRADO`) y la app lo explica.

## D13 — Tests

Dominio (`Money`, cuadre, reglas de acciones por estado), datos (`MockEngine`: nombres de campo
del `PUT`, versión, multipart, mapeo de errores, total numérico) y ViewModels (lista con filtros y
sondeo, detalle con guardar/confirmar/conflicto, trimestres, reporte, empresa). El trinquete sube.
