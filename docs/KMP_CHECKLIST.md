# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, dominio funcional común, diacríticos, aritmética exacta y fechas con `kotlinx.datetime`; 23 archivos de modelo y 10 contratos de repositorio movidos a `commonMain`. Verificación: **Android 28 clases / 208 tests** + **shared 9 clases / 61 tests**, 0 fallos (269 tests totales).

## 1. Infraestructura KMP

| Tarea | Estado | Notas |
|---|---|---|
| Módulo `:shared` + configuración | ✅ | AGP 9, `androidLibrary`, JvmTarget 11, iosArm64/iosSimulatorArm64 |
| `include(":shared")` | ✅ | settings.gradle.kts |
| `kotlinx.serialization` commonMain | ⬜ Pendiente | La versión 1.8.1 ya existe en el catálogo; se añadirá cuando migre el modelo de backup |
| `kotlinx.coroutines` commonMain | ✅ | 1.9.0 |
| `kotlinx-datetime` catálogo + commonMain | ✅ | Versión 0.6.2, expuesta como API de `shared` |
| Interoperabilidad Android `java.time` ↔ `kotlinx-datetime` | ✅ | `JavaTimeInterop.kt`, preserva fechas y nanosegundos de `Instant` |
| `expect/actual` base | ⬜ Pendiente | Platform services |

## 2. Core común

| Componente | Estado | Notas |
|---|---|---|
| `core/text/Diacritics` + tabla (Normalizer) | ✅ | Equivalencia exacta BMP verificada |
| `core/math/ExactArithmetic` | ✅ | `multiply/add/subtractExactOrNull` (coincide runCatching) |
| `ExactArithmeticTest` | ✅ | 7 tests (JUnit4) verdes |
| `MoneyFormatter` | ⬜ Pendiente | JVM-only hoy |
| Otros `java.*` | ⬜ Revisar | Fuera de modelos |

## 3. domain/model

**Comunes (23 archivos):** BackupMovement, BudgetAlertEvaluator, BudgetConfig, BudgetCycle, BudgetCycleSchedule/defaultCycleSchedules, BudgetPeriod, Category, CategoryValidator, DateRange, BudgetCycleCalculator, EntryCardSize, ExpenseFunding, FinanceTransaction, FixedEntry, FixedEntrySchedule, Fortnight, FortnightPeriod, ListProductMatcher, ListReceiptParser, PendingEntry, SavingsGoal, ShoppingList y TransactionType — ✅

**Modelos de dominio pendientes:** ninguno — ✅

**Archivos en `:app`: 2.** `BudgetCycleCalculator.kt` y `EntryScheduleJavaTimeInterop.kt` conservan únicamente overloads de interoperabilidad con `java.time`. Los límites Room y UI convierten explícitamente sin cambiar la persistencia en `epochDay`/`epochMillis`.

## 4. domain/repository (11)

✅: BudgetRepository, CategoryRepository, ExpenseFundingRepository, FixedEntryRepository, FortnightRepository, PendingEntryRepository, ProductCatalogRepository, SavingsRepository, ShoppingListRepository, TransactionRepository (10)
⬜: BackupRepository (1)

Progreso: **10/11 (91%)**. Cada contrato pasa a `shared` solo cuando todos sus tipos de firma existan en `commonMain`; `shared` nunca depende de modelos de `:app`.

## 5. domain/usecase (4)

⬜: EvaluateExpenseFunding, SaveBudget, SaveExpenseWithFunding, SaveTransaction — **0/4 (0%)**

## 6. Data/Room KMP

⬜ FinanceDatabase KMP 2.8.3 (Alto) • ⬜ Migraciones v1–v17 (Alto, sin destructive) • ⬜ Drivers Android+Native (Alto) • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio) • ⬜ DAOs (Medio-Alto)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros restantes → commonTest • ✅ shared: 9 clases / 61 tests • ✅ Android: 28 clases / 208 tests / 0 fallos • ⬜ Room KMP • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ testDebugUnitTest (208/208) • ✅ testAndroidHostTest (61/61) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

## Siguiente paso

**#1 — Bloque base de fechas (`DateRange` + `BudgetCycleCalculator`) — ✅ completado**
1. `DateRange` y el calendario de `BudgetCycleCalculator` usan `kotlinx.datetime` en `commonMain`.
2. Room conserva exactamente `epochDay`; UI y modelos Android convierten explícitamente en sus límites.
3. Los cálculos que todavía reciben `FinanceTransaction` permanecen en Android hasta mover ese modelo.
4. Las pruebas puras de fechas/ciclo se ejecutan con `testAndroidHostTest` en `shared`.
5. `assembleDebug`, `testDebugUnitTest` y `testAndroidHostTest` están verdes.

**#2 — `FinanceTransaction` + contratos de transacciones — ✅ completado**
1. `FinanceTransaction`, `ExpenseFunding` y sus resultados usan `kotlinx.datetime` en `commonMain`.
2. `TransactionRepository` y `ExpenseFundingRepository` son contratos comunes.
3. Los mappers Room conservan exactamente `epochDay` y `epochMillis`; no hubo cambio de esquema.
4. Los cálculos financieros restantes de `BudgetCycleCalculator` ya son comunes.
5. Las pruebas de financiación y alertas presupuestarias se ejecutan en `shared`.

**#4 — `FortnightPeriod` + `Fortnight` + contrato de Plan de ciclo — ✅ completado**
1. El calendario, modelos, cálculos y resultados de Plan de ciclo usan `kotlinx.datetime` en `commonMain`.
2. `FortnightRepository` es un contrato común y Android convierte fechas únicamente en sus límites de UI y Room.
3. Room conserva exactamente `epochDay` y `epochMillis`; no hubo cambio de esquema ni migración de datos.
4. Las 30 pruebas de calendario y matemática de Plan de ciclo se ejecutan en `shared`.

**#5 — `ShoppingList` + `ShoppingListRepository` — ✅ completado**
1. Listas, productos, ajustes, alias, cálculos de totales y resultados usan `kotlinx.datetime` en `commonMain`.
2. La aritmética conserva la detección de desbordamiento sin depender de `java.lang.Math`.
3. Room conserva exactamente `epochDay` y `epochMillis`; no hubo cambio de esquema ni migración de datos.
4. Las 4 pruebas de totales y desbordamiento se ejecutan en `shared`.

**#6 — Migrar los 4 casos de uso a `commonMain`.** Después: `BackupRepository` → persistencia Room KMP.
