# Implementation Plan: Facturación contra el backend

**Branch**: `facturacion-feature` | **Date**: 2026-10-09 | **Spec**: [spec.md](./spec.md)

## Summary

Módulo nuevo `feature/invoicing/{domain,data,presentation}` contra `/api/facturacion`, solo para
ADMIN y solo con conexión. Subida de fotos y PDF con selectores propios de cada plataforma,
revisión con los avisos del servidor, confirmación con control de versión, trimestres, reportes y
datos de empresa. La cuenta sale de la barra inferior para dejar sitio. Ver
[research.md](./research.md).

## Technical Context

**Language/Version**: Kotlin 2.2.20 · **Dependencies**: Ktor 3.2.3 (multipart), Koin, Compose
Multiplatform, Coil 3.3.0; sin dependencias nuevas · **Storage**: ninguno · **Testing**:
`commonTest` con `MockEngine` · **Platform**: Android e iOS · **Constraints**: contrato en
`d1857ad`; solo con conexión.

## Constitution Check

Evaluado contra la constitución **v1.1.2**.

| Principio | Estado |
|---|---|
| I · II | ✅ `feature/invoicing/{domain,data,presentation}` en `commonMain`; selectores en `core/presentation` |
| III Offline-first | ✅ con matiz justificado (D1): datos fiscales sin caché local |
| IV Errores | ✅ `InvoicingError` en el dominio, texto en presentation |
| VI Contrato | ✅ [contracts/facturacion-api.md](./contracts/facturacion-api.md); códigos del módulo del código del backend |
| VIII Tests | ✅ D13; el trinquete sube |
| IX UI | ✅ textos en `composeResources`; componentes de `core/designsystem` |
| XI Build | ✅ sin dependencias nuevas (D7) |
| XII Paridad | ✅ cámara, galería, archivos y apertura en Android e iOS |

**Resultado: PASA.**

## Project Structure

```text
core/presentation/   documentpicker/ (cámara, galería, archivos) y fileopener/ (ver y compartir)
core/designsystem/   AppTopBar con botón de cuenta (LocalAccountAction)
feature/invoicing/
├── domain/          Money, Invoice, VatLine, Quarter, Report, Company, InvoicingError,
│                    InvoicingRepository
├── data/            DTOs, InvoicingRemoteDataSource, KtorInvoicingRepository
└── presentation/    list, upload, detail (+original, historial), quarters, report, company
composeApp/          pestaña Facturación (ADMIN), cuenta en la barra superior
```

## Fases

1. `core/presentation`: selector de documentos y apertura de ficheros (Android e iOS).
2. Dominio y datos con tests.
3. Lista y subida (US1, US4).
4. Detalle, edición, confirmación, descarte, original e historial (US2, US3).
5. Trimestres, reportes y empresa (US5, US6).
6. Navegación, trinquete, README y CI en local. Verificación en dispositivo al final.

## Complexity Tracking

Sin desviaciones.
