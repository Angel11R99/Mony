# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, diacríticos + aritmética exacta comunes, 6 modelos + 8 repositorios + `ListProductMatcher` movidos a `commonMain`. Suite Android: **34 clases / 261 tests, 0 fallos**.

## 1. Infraestructura KMP

| Tarea | Estado | Notas |
|---|---|---|
| Módulo `:shared` + configuración | ✅ | AGP 9, `androidLibrary`, JvmTarget 11, iosArm64/iosSimulatorArm64 |
| `include(":shared")` | ✅ | settings.gradle.kts |
| `kotlinx.serialization` commonMain | ✅ | 1.8.1 |
| `kotlinx.coroutines` commonMain | ✅ | 1.9.0 |
| `kotlinx-datetime` catálogo + commonMain | ⬜ Pendiente | Necesario para `java.time` |
| `expect/actual` base | ⬜ Pendiente | Platform services |

## 2. Core común

| Componente | Estado | Notas |
|---|---|---|
| `core/text/Diacritics` + tabla (Normalizer) | ✅ | Equivalencia exacta BMP verificada |
| `core/math/ExactArithmetic` | ✅ | `multiply/add/subtractExactOrNull` (coincide runCatching) |
| `ExactArithmeticTest` | ✅ | 7 tests (JUnit4) verdes |
| `MoneyFormatter` | ⬜ Pendiente | JVM-only hoy |
| Otros `java.*` | ⬜ Revisar | Fuera de modelos |

## 3. domain/model (21 total)

**Comunes (7):** BudgetAlertEvaluator, Category, CategoryValidator, EntryCardSize, ListProductMatcher, ListReceiptParser, TransactionType — ✅

**Pendientes con `java.time` (14):** DateRange ⬜, BudgetConfig ⬜, BudgetCycle ⬜, **BudgetCycleCalculator** ⬜ (crítico), FinanceTransaction ⬜, FixedEntry ⬜, PendingEntry ⬜, ExpenseFunding ⬜, SavingsGoal ⬜, FixedEntrySchedule ⬜, FortnightPeriod ⬜, Fortnight ⬜, ShoppingList ⬜, BackupMovement ⬜

Progreso: **7/21 (33%)**

## 4. domain/repository (11)

✅: BudgetRepository, CategoryRepository, ExpenseFundingRepository, FixedEntryRepository, PendingEntryRepository, ProductCatalogRepository, SavingsRepository, TransactionRepository (8)
⬜: BackupRepository, FortnightRepository, ShoppingListRepository (3)

Progreso: **8/11 (73%)**

## 5. domain/usecase (4)

⬜: EvaluateExpenseFunding, SaveBudget, SaveExpenseWithFunding, SaveTransaction — **0/4 (0%)**

## 6. Data/Room KMP

⬜ FinanceDatabase KMP 2.8.3 (Alto) • ⬜ Migraciones v1–v17 (Alto, sin destructive) • ⬜ Drivers Android+Native (Alto) • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio) • ⬜ DAOs (Medio-Alto)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros → commonTest • ✅ Android: 34 clases / 261 tests / 0 fallos • ⬜ Room KMP • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ testDebugUnitTest (261/261) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

## Siguiente paso

**#1 — `DateRange.kt`** (menor riesgo)
1. Añadir `kotlinx-datetime` (versión + dependencia `commonMain`)
2. Migrar `java.time` → `kotlinx.datetime.*` (sin cambiar lógica)
3. Mover a `shared/commonMain/kotlin/com/angel/mony/domain/model/DateRange.kt`
4. Compilar + tests (`DateRangeTest`)
5. `assembleDebug` + `testDebugUnitTest` verdes

Orden tras #1: `BudgetConfig` → `BudgetCycle` → **`BudgetCycleCalculator`** (máxima precaución) → `FinanceTransaction/FixedEntry/PendingEntry` → `FortnightPeriod/Fortnight` → resto.