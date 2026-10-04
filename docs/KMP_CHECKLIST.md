# Checklist de Migración KMP (Mony)

> **Estado actual:** `:shared` creado, dominio funcional común y persistencia Room en `commonMain`: 17 entidades, 12 DAOs, `FinanceDatabase` v17, 16 migraciones y configuración compartida. Android activa SQLite Bundled 2.6.1 sobre `personal_finance.db`; iOS dispone de factory por ruta, pendiente de compilar en macOS. Verificación: **Android 29 clases / 209 tests** + **shared 12 clases / 72 tests**, 0 fallos (281 tests totales). Falta abrir una copia real v17 en dispositivo.

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

✅ 17 entidades • ✅ 12 DAOs • ✅ `FinanceDatabase` v17 + constructor KMP • ✅ generación Room/KSP Android • ✅ Room runtime 2.8.3 + SQLite Bundled 2.6.1 • ✅ Migraciones v1–v17 comunes (sin destructive) • ✅ Builders Android+iOS y driver común • ⬜ validar generación KSP/iOS en macOS • ⬜ validar base v17 real en dispositivo • ⬜ RoomRepositories transacciones (Alto) • ⬜ Mappers (Medio)

## 7. Platform Services (expect/actual)

⬜ Preferencias • ⬜ Notificaciones • ⬜ WorkManager↔BGTaskScheduler (4, EntryPoints intactos) • ⬜ Archivos/Export/PDF • ⬜ Deep links (Uri) • ⬜ OCR/Captura

## 8. UI/ViewModels

⬜ ViewModels 14 (Fase 1B) • ⬜ Navegación (Uri) • ⬜ Iconografía/tema (parcial) • ✅ Glance Widgets (no mover) • ⬜ CMP pantallas (Fase 2)

## 9. Tests

⬜ Tests puros restantes → commonTest • ✅ shared: 12 clases / 72 tests • ✅ Android: 29 clases / 209 tests / 0 fallos • ⬜ Room KMP en dispositivo • ✅ NormalizationEquivalenceTest

## 10. Verificación

✅ assembleDebug • ✅ assembleDebugAndroidTest • ✅ testDebugUnitTest (209/209) • ✅ testAndroidHostTest (72/72) • ✅ SQLite Bundled empaquetado en 4 ABI • ⬜ tests instrumentados/base real (sin `adb`) • ⬜ ios* compile (macOS) • ✅ APK/versionado intactos

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

**#9 — Mover DAOs y `FinanceDatabase` a `commonMain` — ✅ completado**
1. Los 12 DAOs y sus filas de proyección se movieron sin cambios de consultas a `commonMain`.
2. `FinanceDatabase` v17 es común y usa `@ConstructedBy` con `FinanceDatabaseConstructor` generado por Room.
3. El procesador Room se trasladó de `:app` a KSP por target en `shared`; Android genera `FinanceDatabase_Impl` y los 12 `Dao_Impl`.
4. Room 2.8.3 no enlaza su plugin de schemas con `kspAndroidMain` del target `androidLibrary` de AGP 9; `shared` conserva `room.schemaLocation` explícito hasta que esa integración sea compatible.
5. El builder Android, el nombre `personal_finance.db` y las migraciones 1→17 siguen intactos en `DatabaseModule`.
6. El schema v17 generado coincide byte por byte con el anterior (`ea77e26a1bc143f153e97eeeb971d95a6b73c11c`).
7. `assembleDebugAndroidTest` vuelve a compilar tras actualizar una prueba de integración rezagada a `kotlinx.datetime`.
8. `assembleDebug`, `testDebugUnitTest` y `testAndroidHostTest` están verdes.

**#10 — Extraer las migraciones Room 1→17 a `commonMain` — ✅ completado**
1. Las 16 migraciones y sus 70 sentencias SQL viven en `FinanceMigrationSteps` y se ejecutan mediante `SQLiteConnection`.
2. La ruta es continua de v1 a v17, no contiene operaciones destructivas y conserva los mismos tokens y orden de SQL que el código Android anterior.
3. Android adapta cada paso común a `Migration(SupportSQLiteDatabase)`; mantiene el builder, `personal_finance.db`, el callback inicial y las migraciones expuestas a pruebas.
4. El schema v17 permanece idéntico al commit anterior y conserva su `identityHash` (`ea77e26a1bc143f153e97eeeb971d95a6b73c11c`).
5. `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `testAndroidHostTest` están verdes; los tests instrumentados compilan, pero no se ejecutaron porque este host no dispone de `adb`.

**#11 — Portar builders y drivers de plataforma — ⚠️ implementación completada; validación en dispositivo pendiente**
1. La configuración común aplica `BundledSQLiteDriver`, las 16 migraciones y el callback inicial mediante `SQLiteConnection`.
2. Android conserva `personal_finance.db`; Room sigue resolviéndolo con `Context.getDatabasePath`, por lo que no cambia la ubicación del archivo existente.
3. iOS dispone de una factory que recibe la ruta absoluta elegida por la aplicación y usa el mismo driver y configuración.
4. Las 21 categorías iniciales se conservan y su inserción preparada está cubierta por una prueba común.
5. El APK contiene `libsqliteJni.so` para arm64-v8a, armeabi-v7a, x86 y x86_64.
6. `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` y `testAndroidHostTest` están verdes. iOS requiere macOS y la apertura de una copia v17 real requiere un dispositivo con `adb`.

**#12 — Validar persistencia real y migrar repositorios.** Abrir una copia de `personal_finance.db` v17 en dispositivo, comparar `user_version` y filas de las 17 tablas, y solo entonces mover mappers y `RoomRepositories` a `commonMain`.
