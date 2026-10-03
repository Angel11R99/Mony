# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, diacríticos, aritmética exacta, calendario, transacciones, entradas fijas y pendientes comunes, 20 archivos de modelo y 8 contratos de repositorio movidos a `commonMain`. Verificación: **Android 31 clases / 244 tests** + **shared 6 clases / 27 tests**, 0 fallos (271 tests totales).

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

**Comunes (20 archivos):** BackupMovement, BudgetAlertEvaluator, BudgetConfig, BudgetCycle, BudgetCycleSchedule/defaultCycleSchedules, BudgetPeriod, Category, CategoryValidator, DateRange, BudgetCycleCalculator, EntryCardSize, ExpenseFunding, FinanceTransaction, FixedEntry, FixedEntrySchedule, ListProductMatcher, ListReceiptParser, PendingEntry, SavingsGoal y TransactionType — ✅

**Pendientes con `java.time` (3):** FortnightPeriod ⬜, Fortnight ⬜, ShoppingList ⬜

**Pendientes en `:app`: 6 archivos.** `BackupMovement`, `BudgetConfig`, `BudgetCycle`, `SavingsGoal`, `DateRange`, `BudgetCycleCalculator`, `FinanceTransaction` y `ExpenseFunding` ya validan la transición con conversiones explícitas en los límites CSV, Room y UI. El archivo Android `BudgetCycleCalculator.kt` conserva únicamente overloads de interoperabilidad con `java.time`.

## 4. domain/repository (11)

✅: BudgetRepository, CategoryRepository, ExpenseFundingRepository, ProductCatalogRepository, SavingsRepository, TransactionRepository (6)
⬜: BackupRepository, FortnightRepository, ShoppingListRepository (3)

Progreso: **6/11 (55%)**. Cada contrato pasa a `shared` solo cuando todos sus tipos de firma existan en `commonMain`; `shared` nunca depende de modelos de `:app`.

## 5. domain/usecase (4)

⬜: EvaluateExpenseFunding, SaveBudget, SaveExpenseWithFunding, SaveTransaction — **0/4 (0%)**

## 6. Data/Room KMP

⬜ FinanceDatabase KMP 2.8.3 (Alto) • ⬜ Migraciones v1–v17 (Alto, sin destructive) • ⬜ Drivers Android+Native (Alto) • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio) • ⬜ DAOs (Medio-Alto)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros restantes → commonTest • ✅ shared: 4 clases / 22 tests • ✅ Android: 32 clases / 249 tests / 0 fallos • ⬜ Room KMP • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ testDebugUnitTest (249/249) • ✅ testAndroidHostTest (22/22) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

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

**#4 — Migrar `FortnightPeriod` y `Fortnight` con su contrato.** Después: `ShoppingList` → resto.
