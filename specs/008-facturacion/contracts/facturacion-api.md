# Contrato usado: `/api/facturacion` (backend `d1857ad`)

Solo ADMIN (`SecurityConfig`: `/api/facturacion/**` → `hasRole("ADMIN")`). Importes como texto
`^-?\d{1,10}(\.\d{1,2})?$`; fechas `yyyy-MM-dd`; instantes ISO UTC.

| Ruta | Uso en la app | Errores del módulo |
|---|---|---|
| `GET /empresa` | Empresa | 404 `EMPRESA_SIN_CONFIGURAR` |
| `PUT /empresa` `{razonSocial, nif}` | Empresa | 422 `NIF_INVALIDO` |
| `GET /facturas?desde&hasta&parte&tipo&estado&pagina&tamano` | Lista (tamaño 50) | — |
| `POST /facturas` multipart `ficheros[]` → 202 `[{fichero, resultado, facturaId?}]` | Subida | 413 |
| `GET /facturas/{id}` | Detalle | 404 `FACTURA_NOT_FOUND` |
| `PUT /facturas/{id}` `FacturaRequest` (+`version`) | Guardar | 409 `VERSION_DESACTUALIZADA`, `ESTADO_NO_PERMITIDO`, `TRIMESTRE_CERRADO`; 422 `FACTURA_INCOHERENTE` |
| `POST /facturas/{id}/confirmar` `{version}` | Confirmar | 409 versión, estado, `EMPRESA_SIN_CONFIGURAR`, `TRIMESTRE_CERRADO`, `FACTURA_DUPLICADA`; 422 `FACTURA_INCOHERENTE` |
| `POST /facturas/{id}/descartar` `{version}` | Descartar | 409 versión, estado, `TRIMESTRE_CERRADO` |
| `POST /facturas/{id}/reconocer` → 202 | Reconocer de nuevo | 409 `RECONOCIMIENTO_NO_DISPONIBLE`, `ESTADO_NO_PERMITIDO` |
| `GET /facturas/{id}/original` → bytes + `Content-Type` | Original | 404 |
| `GET /facturas/{id}/historial` | Historial | 404 |
| `GET /trimestres?anio` | Trimestres | — |
| `POST /trimestres/{a}/{t}/cerrar` | Cerrar | 409 `TRIMESTRE_CERRADO`, `TRIMESTRE_CON_PENDIENTES`; 422 `PERIODO_INVALIDO` |
| `POST /trimestres/{a}/{t}/reabrir` `{motivo 10–500}` | Reabrir | 409 `TRIMESTRE_ABIERTO`; 422 |
| `GET /reportes?periodo&anio&mes&trimestre&formato=json|csv|pdf` | Reporte y descarga | 422 `PERIODO_INVALIDO` |

`resultado` de subida: `ACEPTADA`, `DUPLICADA`, `FORMATO_NO_ADMITIDO`, `DEMASIADO_GRANDE`, `VACIO`,
`PDF_NO_LEGIBLE`. Límites: 10 MB por fichero, 50 MB por envío, JPEG/PNG/WebP/PDF por bytes
mágicos, PDF ≤ 20 páginas.
