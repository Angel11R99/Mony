# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, dominio funcional común, formato de respaldo v3 portable y las 17 entidades Room en `commonMain`; Room 2.8.3 y SQLite Bundled 2.6.1 disponibles para los targets KMP. Verificación: **Android 29 clases / 209 tests** + **shared 11 clases / 69 tests**, 0 fallos (278 tests totales).

## 1. Infraestructura KMP

| Tarea | Estado | Notas |
|---|---|---|
| Módulo `:shared` + configuración | ✅ | AGP 9, `androidLibrary`, JvmTarget 11, iosArm64/iosSimulatorArm64 |
| `include(":shared")` | ✅ | settings.gradle.kts |
| `kotlinx.serialization` commonMain | ✅ | 1.8.1; formato de respaldo v3 y sus 17 DTOs comunes |
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

**Comunes (24 archivos):** BackupMovement, FullBackup, BudgetAlertEvaluator, BudgetConfig, BudgetCycle, BudgetCycleSchedule/defaultCycleSchedules, BudgetPeriod, Category, CategoryValidator, DateRange, BudgetCycleCalculator, EntryCardSize, ExpenseFunding, FinanceTransaction, FixedEntry, FixedEntrySchedule, Fortnight, FortnightPeriod, ListProductMatcher, ListReceiptParser, PendingEntry, SavingsGoal, ShoppingList y TransactionType — ✅

**Modelos de dominio pendientes:** ninguno — ✅

**Archivos en `:app`: 2.** `BudgetCycleCalculator.kt` y `EntryScheduleJavaTimeInterop.kt` conservan únicamente overloads de interoperabilidad con `java.time`. Los límites Room y UI convierten explícitamente sin cambiar la persistencia en `epochDay`/`epochMillis`.

## 4. domain/repository (11)

✅: BackupRepository, BudgetRepository, CategoryRepository, ExpenseFundingRepository, FixedEntryRepository, FortnightRepository, PendingEntryRepository, ProductCatalogRepository, SavingsRepository, ShoppingListRepository, TransactionRepository (11)

Progreso: **11/11 (100%)**. `shared` no depende de modelos de `:app`.

## 5. domain/usecase (4)

✅: EvaluateExpenseFunding, SaveBudget, SaveExpenseWithFunding, SaveTransaction — **4/4 (100%)**

## 6. Data/Room KMP

✅ 17 entidades en `commonMain` • ✅ Room runtime 2.8.3 + SQLite Bundled 2.6.1 disponibles en `shared` • ⬜ FinanceDatabase KMP (Alto) • ⬜ Migraciones v1–v17 (Alto, sin destructive) • ⬜ Activar driver en Android+Native (Alto) • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio) • ⬜ DAOs (Medio-Alto)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros restantes → commonTest • ✅ shared: 11 clases / 69 tests • ✅ Android: 29 clases / 209 tests / 0 fallos • ⬜ Room KMP • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ testDebugUnitTest (209/209) • ✅ testAndroidHostTest (69/69) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

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

**#6 — Migrar los 4 casos de uso a `commonMain` — ✅ completado**
1. `EvaluateExpenseFunding`, `SaveBudget`, `SaveExpenseWithFunding` y `SaveTransaction` ya no dependen de `javax.inject` ni de APIs JVM.
2. `SaveBudget` usa `Clock` y `TimeZone` de `kotlinx.datetime`; el reloj y la zona son inyectables para pruebas deterministas.
3. Android conserva Hilt mediante proveedores explícitos en `DatabaseModule`; no cambió el flujo de las pantallas.
4. Cinco pruebas comunes cubren validación, normalización, financiación y creación inicial del presupuesto.
5. `assembleDebug`, `testDebugUnitTest` y `testAndroidHostTest` están verdes.

**#7 — Migrar `BackupRepository` y el modelo de backup a `commonMain` — ✅ completado**
1. `BackupRepository`, `BackupPreview` y `BackupRestoreResult` son comunes y usan `kotlinx.datetime`.
2. Los 17 DTOs del respaldo v3 y su codec JSON usan `kotlinx.serialization` en `commonMain`.
3. Android conserva un adaptador explícito Entity ↔ DTO y el parser CSV legado; Room todavía no se movió.
4. Se corrigió la pérdida de `expense_funding`: ahora se exporta, parsea y restaura con su `transactionId` remapeable.
5. Los respaldos previos que omiten colecciones nuevas y los campos futuros desconocidos siguen siendo legibles.
6. Tres pruebas comunes de formato y una prueba Android del adaptador están verdes.

**#8 — Iniciar persistencia Room KMP: runtime y entidades — ✅ completado**
1. Las 17 entidades Room se movieron sin cambios a `commonMain`, conservando paquetes, tablas, columnas, índices y claves foráneas.
2. `shared` incorpora Room runtime 2.8.3 y SQLite Bundled 2.6.1; el driver aún no se activa mientras `FinanceDatabase` permanezca en Android.
3. `FinanceDatabase`, DAOs, migraciones y repositorios siguen en `:app`; el procesador Room Android continúa generándolos desde las entidades comunes.
4. El schema exportado v17 conserva el mismo `identityHash` y no se generó una versión nueva.
5. `assembleDebug`, `testDebugUnitTest` y `testAndroidHostTest` están verdes.

**#9 — Mover DAOs y `FinanceDatabase` a `commonMain`.** Configurar KSP por target y validar que el schema v17 generado sea idéntico antes de tocar migraciones o el builder Android.
