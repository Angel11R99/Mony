# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, diacríticos y aritmética exacta comunes, 12 archivos de modelo y 3 contratos de repositorio movidos a `commonMain`. Suite Android: **35 clases / 263 tests, 0 fallos**.

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

**Comunes (12 archivos):** BackupMovement, BudgetAlertEvaluator, BudgetConfig, BudgetCycle, BudgetCycleSchedule/defaultCycleSchedules, BudgetPeriod, Category, CategoryValidator, EntryCardSize, ListProductMatcher, ListReceiptParser y TransactionType — ✅

**Pendientes con `java.time` (11):** DateRange ⬜, **BudgetCycleCalculator** ⬜ (crítico), FinanceTransaction ⬜, FixedEntry ⬜, PendingEntry ⬜, ExpenseFunding ⬜, SavingsGoal ⬜, FixedEntrySchedule ⬜, FortnightPeriod ⬜, Fortnight ⬜, ShoppingList ⬜

**Pendientes en `:app`: 11 archivos.** `BackupMovement`, `BudgetConfig` y `BudgetCycle` ya validan la transición con conversiones explícitas en los límites CSV, Room y UI. `DateRange` tiene más de 100 referencias y no debe migrarse de forma aislada.

## 4. domain/repository (11)

✅: BudgetRepository, CategoryRepository, ProductCatalogRepository (3)
⬜: BackupRepository, ExpenseFundingRepository, FixedEntryRepository, FortnightRepository, PendingEntryRepository, SavingsRepository, ShoppingListRepository, TransactionRepository (8)

Progreso: **3/11 (27%)**. Cada contrato pasa a `shared` solo cuando todos sus tipos de firma existan en `commonMain`; `shared` nunca depende de modelos de `:app`.

## 5. domain/usecase (4)

⬜: EvaluateExpenseFunding, SaveBudget, SaveExpenseWithFunding, SaveTransaction — **0/4 (0%)**

## 6. Data/Room KMP

⬜ FinanceDatabase KMP 2.8.3 (Alto) • ⬜ Migraciones v1–v17 (Alto, sin destructive) • ⬜ Drivers Android+Native (Alto) • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio) • ⬜ DAOs (Medio-Alto)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros → commonTest • ✅ Android: 35 clases / 263 tests / 0 fallos • ⬜ Room KMP • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ testDebugUnitTest (263/263) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

## Siguiente paso

**#1 — Completar el bloque base de fechas (`DateRange` + `BudgetCycleCalculator`)**
1. ~~Añadir `kotlinx-datetime` (versión + dependencia `commonMain`).~~ ✅
2. ~~Definir conversiones Android entre `java.time` y `kotlinx.datetime` en los límites de Room/UI.~~ ✅
3. ~~Migrar `BudgetConfig` y `BudgetCycle`.~~ ✅
4. Migrar `DateRange` y sus consumidores directos para evitar firmas incompatibles.
5. Mantener `BudgetCycleCalculator` sin cambios de comportamiento y moverlo cuando `DateRange` sea común.
6. Ejecutar tests de fechas/ciclo y después `assembleDebug` + `testDebugUnitTest`.

Orden tras #1: **`BudgetCycleCalculator`** (máxima precaución) → `FinanceTransaction/FixedEntry/PendingEntry` → `FortnightPeriod/Fortnight` → resto.
