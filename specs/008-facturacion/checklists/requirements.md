# Specification Quality Checklist: Facturación contra el backend

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-09
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- Decisiones por defecto: solo con conexión (datos fiscales, sin idempotencia), el servidor es la
  fuente de los avisos, la cuenta pasa a la barra superior para no superar cinco destinos.
- Reglas del backend que conviene conocer: los NIF extranjeros dejan un aviso bloqueante y el
  borrador puede moverse a un trimestre cerrado (el fallo llega al confirmar).
