# Data Model: facturación

Todo en `feature/invoicing/domain`. Sin persistencia local (research D1).

## Money

`value class Money(val cents: Long)`. `parse("1.234,56" | "1234.56")` → céntimos (máx. 10 enteros,
2 decimales, signo opcional); `toApi()` → `"1234.56"`; `format()` → `"1.234,56 €"`.

## Invoice

| Campo | Tipo | Regla |
|---|---|---|
| id | String | |
| state | `InvoiceState` | PENDING_RECOGNITION, DRAFT, CONFIRMED, DISCARDED |
| type | `InvoiceType?` | ISSUED, RECEIVED (null: sin clasificar) |
| version | Int | se envía en todo cambio |
| issuer / recipient | `Party?` | nombre ≤ 200, NIF ≤ 20 |
| number | String? | ≤ 60 |
| issueDate | LocalDate? | |
| concept | String? | ≤ 500 |
| currency | String | `^[A-Z]{3}$`, EUR |
| corrective | Boolean | rectificativa |
| lines | List<`VatLine`> | |
| withholding | Money | ≥ 0 salvo rectificativa |
| total | Money? | |
| quarterClosed | Boolean | |
| recognition | `RecognitionSummary?` | result, doubtfulFields |
| warnings | List<`InvoiceWarning`> | field, code, blocking, message |

Derivados: `hasBlockingWarnings`, `canEdit` (no descartada y, si confirmada, trimestre abierto),
`canConfirm` (borrador), `canDiscard` (no descartada y, si confirmada, trimestre abierto),
`canRecognize` (pendiente o borrador), `balanceGap()` = total − (Σ(base+cuota+recargo) − retenciones).

## VatLine

`rate: Money` (porcentaje, 0–100), `base`, `quota`, `surcharge: Money`, `noQuotaCause: NoQuotaCause?`
(EXEMPT, REVERSE_CHARGE, INTRA_EU), obligatoria si `quota == 0`.

## InvoiceDraft

Lo que se envía en el `PUT`: los campos editables + `version`.

## InvoiceSummary (lista)

id, state, type, issuer/recipient (nombre y NIF), number, issueDate, total, warningCount.
`Page<T>(items, page, size, total)`.

## InvoiceFilter

state?, type?, from?, to?, text.

## UploadDocument / UploadResult

`UploadDocument(bytes, fileName, mimeType)`. `UploadResult(position, outcome, invoiceId?)` con
`UploadOutcome`: ACCEPTED, DUPLICATE, UNSUPPORTED_FORMAT, TOO_LARGE, EMPTY, UNREADABLE_PDF.

## InvoiceHistory

`recognitions: List<(result, model, createdAt, error?)>`, `changes: List<(action, authorId,
at, previousValues: Map<String, String>)>`.

## Quarter

year, quarter (1–4), closed, events: (action CLOSE/REOPEN, authorId, at, reason?).

## Report

period (type, year, month?, quarter?, from, to), issued y received `ReportGroup(count, base,
vatByRate: Map<String, Money>, surcharge, withholding, total, noQuota: Map<NoQuotaCause, Money>)`,
pending, closedQuarters, computedAt. `ReportPeriod`: MONTHLY(year, month), QUARTERLY(year,
quarter), YEARLY(year). `ReportFormat`: CSV, PDF.

## Company

legalName ≤ 200, taxId ≤ 20, recognitionEnabled (solo lectura).

## InvoicingError

NoInternet, NotFound, CompanyNotConfigured, InvalidTaxId, StaleVersion, StateNotAllowed,
QuarterClosed, QuarterOpen, QuarterHasPending(message), Duplicate, Incoherent(message),
RecognitionUnavailable, InvalidPeriod, Invalid, TooLarge, Forbidden, RateLimited, Unknown.
