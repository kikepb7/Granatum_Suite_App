# Contrato de API

La app habla con el backend de Granatum ([kikepb7/Granatum_Suite_Backend](https://github.com/kikepb7/Granatum_Suite_Backend))
y **no inventa endpoints, campos ni códigos de estado** (principio VI de la constitución). Este
documento dice dónde está ese contrato y cómo se actualiza.

## Qué es el contrato

| Pieza | Dónde | Qué cubre |
|---|---|---|
| `docs/openapi.json` | este repo, copia fijada | Rutas, cuerpos de petición y respuesta, roles por ruta |
| `docs/openapi.pin` | este repo | El commit del backend del que sale la copia |
| `specs/<feature>/contracts/README.md` | backend | **Los códigos de error de cada ruta**. El `openapi.json` no los lista todos: por ejemplo, el `401 CREDENCIALES_INVALIDAS` del login solo está aquí |
| `docs/ARCHITECTURE.md`, sección «Seguridad y roles» | backend | El contenido del token, que OpenAPI no describe |

Las tres fuentes del backend se leen **en el mismo commit** que marca `docs/openapi.pin`.

### El token de acceso

Es un JWT. La app lee sus claims sin verificar la firma: verificarla es trabajo del servidor, y
la app no tiene la clave.

| Claim | Contenido |
|---|---|
| `sub` | Id de la **persona empleada** (no de la cuenta) |
| `role` | `ADMIN`, `ENCARGADO`, `EMPLEADO` o `REPRESENTANTE` |
| `type` | `access` o `refresh` |
| `pwd_change` | `true` si la sesión solo puede cambiar la contraseña; ausente en otro caso |
| `exp` | Caducidad. Para programar la renovación se usa `expiresIn` de la respuesta |

No existe un endpoint de «quién soy»: el id y el rol salen del token, y `GET /api/empleados/{id}`
solo está abierto a `ADMIN`.

### Errores

Siempre `{ "code": "...", "message": "..." }`. La app decide por `code`, que es estable;
`message` es texto para personas y puede cambiar. La única excepción declarada es
`422 PASSWORD_DEBIL`, que añade `requisitos: [...]`.

## Cómo se actualiza

```bash
scripts/sync-openapi.sh <commit-del-backend>
```

El script exige el sha completo, no una rama: el pin tiene que decir exactamente contra qué
contrato se construyó la app. Después, el diff de `docs/openapi.json` se revisa como cualquier
otro cambio de código. Un cambio de contrato que rompa algo se arregla en la misma PR que
actualiza el pin.

No se edita `docs/openapi.json` a mano.
