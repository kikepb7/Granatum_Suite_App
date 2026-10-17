# Implementation Plan: Eventos (app)

**Branch**: `events-feature` · **Spec**: [spec.md](spec.md) · **Research**: [research.md](research.md)

## Summary

Nueva feature `events` con lista, formulario y detalle. El detalle gestiona los materiales del
evento contra el inventario del servidor (backend 011), con selector de materiales, contador y aviso
de stock insuficiente. Pestaña nueva para los cuatro roles; solo lectura para `REPRESENTANTE`.

## Technical Context

Kotlin 2.2.20, Compose Multiplatform 1.9, Ktor 3.2.3, Room KMP 2.7.2, Koin 4.1, kotlinx-datetime.
Nueva dependencia de test: Turbine 1.2.1 (multiplataforma).

## Structure

```
feature/events/domain        model, EventsError, EventsRepository, use cases, validation
feature/events/database      Room: EventEntity (materiales en JSON), dao
feature/events/data          DTOs del contrato, remote data source, repositorio offline-first, mappers
feature/events/presentation  list / form / detail (ViewModel + Screen + Content + previews), mappers
core/domain                  UserRole.canSeeEvents / canManageEvents
core/demo                    rutas de eventos sobre el stock de su inventario
composeApp                   pestaña y grafo de navegación
```
