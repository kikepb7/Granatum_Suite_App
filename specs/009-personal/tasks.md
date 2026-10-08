# Tasks: Alta del propietario y gestión del personal

**Input**: Design documents from `/specs/009-personal/`
**Tests**: SÍ (research D10).

## Phase 1: Setup

- [ ] T001 Fijar el contrato a `2ec33d0` con `scripts/sync-openapi.sh` (hecho en su propio commit)
- [ ] T002 Crear `feature/staff/{domain,data,presentation}` e incluirlos en `settings.gradle.kts`, `composeApp` y Koin; `strings.xml` del módulo

## Phase 2: US1 — Registro del propietario (P1)

- [ ] T003 `AuthError`: `InvalidBootstrapCode`, `EmailTaken`, `AccountExists`, `InvalidDocument`; `AuthErrorMapper` con sus códigos
- [ ] T004 `AuthRepository.registerOwner` en `KtorAuthRepositoryImpl` (registro y login encadenado) + `RegistroRequestDto`; test con `MockEngine`
- [ ] T005 `feature/auth/presentation/signup/OwnerSignUpViewModel.kt` + `OwnerSignUpScreen.kt` (validación local, política de contraseña); enlace desde el login; la puerta alterna login y registro; test del ViewModel

## Phase 3: Foundational personal

- [ ] T006 [P] Dominio: `StaffMember`, `ContractType`, `StaffDraft`, `Onboarding`, `TemporaryCredentials`, `StaffError`, `StaffRepository`, `StaffUseCases`
- [ ] T007 [P] Datos: DTOs, `StaffRemoteDataSource`, `KtorStaffRepository`, DI; test con `MockEngine` (campos, `PUT` sin documento, errores)

## Phase 4: US2 — Alta (P1)

- [ ] T008 [US2] `ui/onboarding/OnboardingViewModel.kt` + `OnboardingScreen.kt` y hoja de credenciales (copiar, aviso de una sola vez); test

## Phase 5: US3 + US4 — Personal (P2)

- [ ] T009 [US3] `ui/list/StaffListViewModel.kt` + `StaffListScreen.kt` (búsqueda, activo/inactivo, alta, acceso a la jornada)
- [ ] T010 [US3] [US4] `ui/detail/StaffDetailViewModel.kt` + `StaffDetailScreen.kt` (editar, baja/reactivar, restablecer o dar acceso)
- [ ] T011 [P] Tests de los ViewModels de lista y ficha

## Phase 6: Polish

- [ ] T012 `UserRole.canManageStaff`; la pestaña Equipo de ADMIN abre el personal
- [ ] T013 Filtro del log de red con `/empleados`
- [ ] T014 [P] Sin cadenas visibles en el código (principio IX)
- [ ] T015 Trinquete, README y `docs/api-contract.md`
- [ ] T016 CI en local completa
- [ ] T017 Quickstart en dispositivo (al final)
