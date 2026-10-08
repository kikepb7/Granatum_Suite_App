# Quickstart: verificar facturación

**Prerrequisitos**: backend Granatum local (sin `ANTHROPIC_API_KEY` = modo manual; con ella,
reconocimiento real), una cuenta ADMIN, la app compilada contra el entorno local.

| # | Escenario | Esperado |
|---|---|---|
| 1 | Entrar como EMPLEADO y como ENCARGADO | No aparece Facturación |
| 2 | Entrar como ADMIN sin empresa configurada | Aviso y acceso a configurarla |
| 3 | Guardar empresa con NIF inválido / válido | Error en el campo / guardada |
| 4 | Hacer una foto de una factura y subirla | Aparece pendiente (manual) o borrador (reconocimiento) y la lista se actualiza sola |
| 5 | Subir el mismo fichero otra vez | «Duplicado», con acceso a la existente |
| 6 | Subir una foto HEIC desde la galería del iPhone | Se sube como JPEG |
| 7 | Subir un .txt renombrado a .pdf | «Formato no admitido» solo para ese fichero |
| 8 | Rellenar un borrador con importes que no cuadran | Aviso bloqueante «no cuadra»; la ayuda de cuadre muestra la diferencia |
| 9 | Corregir y confirmar | Confirmada |
| 10 | Editar la misma factura desde dos móviles | El segundo recibe «ha cambiado» y recarga |
| 11 | Ver el original de una imagen y de un PDF | Imagen en la app; PDF en el visor del sistema |
| 12 | Descartar un borrador | Descartada, sin edición |
| 13 | Cerrar un trimestre con borradores | «Quedan N pendientes» |
| 14 | Cerrar con todo confirmado; reabrir con motivo corto / válido | Cerrado; error de motivo / reabierto |
| 15 | Corregir una confirmada de un trimestre cerrado | No se ofrece |
| 16 | Reporte trimestral; descargar PDF y CSV | Resumen correcto; ficheros abiertos o compartidos |
| 17 | Modo avión en facturación | Estado sin conexión con «Reintentar» |
| 18 | Representante | Sin barra inferior (un único destino), cuenta en la barra superior |
