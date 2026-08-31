# Mony — Contexto Completo de la App

> **App:** Mony · **Paquete:** `com.angel.mony` (`applicationId` + `namespace`)
> **Stack:** Kotlin · Jetpack Compose (Material 3) · Room 2 · Hilt · Glance (app widgets) · WorkManager · Navigation Compose · Coroutines/Flow
> **Idioma UI:** 100% Español · **Moneda:** DOP / RD$ (Dominican Peso, `es-DO`) · **Offline-first** · `minSdk 24` · `targetSdk 36` · `FinanceDatabase v15` (`exportSchema = true`)

---

## 1. Propósito y filosofía

**Mony** es un tracker personal **rápido, simple y offline** para registrar ingresos/gastos sin fricción, con:

- Presupuesto por ciclo (mensual/quincenal configurable).
- Entradas fijas (recurrentes) y pendientes (por cobrar/pagar).
- Listas de compra con escaneo + finalización financiera.
- Metas de ahorro.
- Estadísticas comparativas.
- 11 widgets de pantalla de inicio.
- Cierre de ciclo presupuestario manual / automático.

Principios: **Simple, Rápido, Offline, Predecible, Consistente, Seguro con los datos** — ver `AGENTS.md`.

---

## 2. Arquitectura (Clean + MVVM)

```
app/src/main/java/com/angel/mony/
├── domain/          # Kotlin puro, sin imports Android. Reglas de negocio testeables
│   ├── model/       # FinanceTransaction, BudgetConfig, BudgetCycle*, Category, FixedEntry, PendingEntry, SavingsGoal, ShoppingList*, etc.
│   ├── repository/  # Interfaces: BudgetRepository, CategoryRepository, TransactionRepository, FixedEntryRepository, PendingEntryRepository, SavingsRepository, ShoppingListRepository, ProductCatalogRepository
│   └── usecase/     # SaveTransaction
├── data/
│   ├── local/
│   │   ├── entity/   # 12 entities Room
│   │   ├── dao/      # 8 DAOs
│   │   └── database/ # FinanceDatabase (v15)
│   ├── mapper/       # FinanceMappers.kt — Entity ↔ Domain (toEntity/toDomain)
│   └── repository/   # RoomRepositories.kt (1 clase por repo con @Inject) + OpenFoodFactsProductCatalogRepository
├── presentation/    # Un subpaquete por pantalla (Screen + ViewModel)
│   ├── home/
│   ├── transactions/ # Add + History
│   ├── statistics/
│   ├── fixed/
│   ├── pending/
│   ├── list/        # ShoppingLists + ShoppingList
│   ├── savings/
│   ├── categories/
│   ├── settings/
│   └── components/  # FinanceComponents, TransactionRow, Skeleton, AmountVisualTransformation, etc.
├── navigation/      # FinanceApp.kt (único NavHost) + FloatingModuleBar
├── ui/theme/        # Theme, Color, Type, AppAppearance (SharedPreferences)
├── widget/          # 11 widgets Glance + WidgetData/WidgetFormatting/WidgetTheme/WidgetComponents
├── di/              # DatabaseModule (migrations + @Provides) + RepositoryModule (@Binds)
├── core/            # MoneyFormatter, CyclePreferences, EntryDisplayPreferences, BudgetAlertPreferences, CsvExporter, HistoryPdfWriter, ToastNotifier
└── FinanceApplication.kt / MainActivity.kt
```

**Configuración de paquete:**
- `app/build.gradle.kts:9` → `namespace = "com.angel.mony"` · `applicationId = "com.angel.mony"`
- `settings.gradle.kts` → `rootProject.name = "Mony"`
- `app/src/main/res/values/strings.xml` → `<string name="app_name">Mony</string>`
- `FileProvider` authority migra automáticamente a `com.angel.mony.fileprovider` vía `${applicationId}`.

**Reglas clave:**
- ViewModel → Repository interface → Room. Nunca DAO directo en UI.
- `MoneyFormatter` (`es-DO`, DOP) es el único lugar de formateo. Todo dinero en `Long amountInCents` (nunca Float/Double).
- Fechas con `java.time` (`LocalDate`/`Instant`), persistidas como `*EpochDay` / `*EpochMillis`.
- Tras cualquier cambio que afecte widgets/apariencia → `updateAllFinanceWidgets(context)`.
- Widgets/workers usan Hilt `@EntryPoint` + `EntryPointAccessors`.

---

## 3. Base de datos Room

**DB:** `personal_finance.db` — `FinanceDatabase:53` (`app/src/main/java/com/angel/mony/data/local/database/FinanceDatabase.kt:41`)

**Entities (12):** `CategoryEntity`, `TransactionEntity`, `BudgetConfigEntity`, `BudgetCycleEntity`, `FixedEntryEntity`, `PendingEntryEntity`, `SavingsGoalEntity`, `ShoppingListEntity`, `ShoppingListItemEntity`, `ShoppingAdjustmentEntity`, `KnownProductEntity`, `ProductRecognitionAliasEntity`.

**Migraciones registradas en** `di/DatabaseModule.kt:277` — `1→2 … 14→15` (15 versiones). Schema JSON en `app/schemas/com.angel.mony.data.local.database.FinanceDatabase/` (migrado desde `com.example.personalfinancetracker...`). **Prohibido** `fallbackToDestructiveMigration()`.

| Migración | Qué hace |
|---|---|
| 1→2 | Crea `budget_config` |
| 2→3 | Añade `cycleStart/cycleStartedAt` + crea `budget_cycle_history` |
| 3→4 | `incomeTransactionId` en `budget_config` |
| 4→5 | Crea `fixed_entries` |
| 5→6 | `manualDateMode/scheduleMode/nextRunAt/lastAdded*` en fixed |
| 6→7 | `fixedEntryId` en transactions |
| 7→8 | `closingDays` (String `"opening:closing,…"`) |
| 8→9 | Crea `pending_entries` |
| 9→10 | `reminderMinutesOfDay` |
| 10→11 | `budgetLimitInCents` en categories |
| 11→12 | Crea `savings_goals` + `savingsGoalId` en transactions |
| 12→13 | `completedAtEpochMillis` en savings |
| 13→14 | Crea `shopping_lists`, `shopping_list_items`, `shopping_adjustments`, `known_products` |
| 14→15 | `payableId/purchaseDate/paymentMethod/expenseCategoryId` en shopping_lists + `sourceShoppingListId` en pending + tabla `product_recognition_aliases` |

**Seed `onCreate`:** 21 categorías por defecto (`Salario`, `Freelance`, `Alimentación`, `Transporte`, `Vivienda`, … `Otros`) en `DatabaseModule.database()`.

---

## 4. Modelo de dominio (`domain/model/`)

- **`FinanceTransaction`** (`FinanceTransaction.kt`): `id, amountInCents, type (INCOME/EXPENSE), categoryId, description, date: LocalDate, createdAt/updatedAt: Instant, fixedEntryId?, savingsGoalId?`
- **`Category`** (+ `CategoryValidator`, `budgetLimitInCents?: Long` — límite por categoría).
- **`BudgetConfig`** (`BudgetConfig.kt`): `amountInCents, period (MONTHLY/FORTNIGHTLY), cycleStart?: LocalDate, cycleStartedAt?: Instant, incomeTransactionId?, cycleSchedules: List<BudgetCycleSchedule>`. `BudgetCycleSchedule(openingDay:1..31, closingDay:1..31)` serializado como `"15:31,1:14"`.
- **`BudgetCycle`** (histórico): `period, budgetAmountInCents, incomeInCents, expenseInCents, startDate, endDate, closedAt`.
- **`DateRange(start, endInclusive)`** con helpers `current()/currentFortnight()`.
- **`BudgetCycleCalculator.kt`** — **núcleo presupuestario**:
  - `activeBudgetPeriod(budget, today)` / `nextBudgetPeriod` / `previousBudgetPeriod` / `budgetPeriodForSchedule`
  - `belongsToActiveBudgetCycle()` — fecha es fuente de verdad; `cycleStartedAt` solo filtra si `date == period.start` y `createdAt < boundary`.
  - `availableForBudget`, `budgetCycleExpenses`, `budgetUsagePercent`, `canManuallyCloseBudgetCycle` (solo si `today == period.endInclusive`), `shouldAutomaticallyCloseBudgetCycle(now, closeTime=21:00)`.
- **`FixedEntry`** (`FixedEntry.kt`): `type, description, amountInCents, categoryId, isActive, manualDateMode (TODAY/PREVIOUS_FORTNIGHT/PREVIOUS_MONTH/SPECIFIC_DATE), scheduleMode (MANUAL/AFTER_FORTNIGHT/AFTER_MONTH/SPECIFIC_DATE_TIME), scheduleHour, nextRunAt, lastAddedAt/Date`.
- **`PendingEntry`**: `type (PAYMENT/COLLECTION), description, amountInCents, categoryId, date, reminderMinutesOfDay?, isDone, doneAt, transactionId?, sourceShoppingListId?` (si tiene `sourceShoppingListId`, solo editable vía su lista madre).
- **`SavingsGoal` / `SavingsGoalProgress`**: progreso `%`, `isCompleted`, `canComplete`, `excessInCents`.
- **`ShoppingList` / `ShoppingListItem` / `ShoppingAdjustment` / `KnownProduct` / `ProductRecognitionAlias`** — ver `ShoppingList.kt:102` totales con `MoneySum` usando `Math.addExact/multiplyExact` (overflow-safe).
- **`BudgetAlertEvaluator`, `EntryCardSize`, `BackupMovement`, `ListReceiptParser`, `ListProductMatcher`**.

---

## 5. Repositorios (`data/repository/RoomRepositories.kt`)

Efectos cross-table preservados (no simplificar sin entender):

- **`RoomTransactionRepository.delete(id)`**: borra transacción, re-vincula `fixedEntry.lastAdded` al último `latestForFixedEntry`, bloquea borrado si la transacción pertenece a una lista completada (`findListIdByExpenseTransaction`) o a un crédito (`sourceShoppingListId != null`). `update()` con los mismos guards. `duplicate()` clona a hoy. `restoreBackup()` con deduplicación `date|amount|type|categoryId|description` y auto-creación de categorías.
- **`RoomFixedEntryRepository`**: `post(entry, transaction)` transaccional.
- **`RoomPendingEntryRepository`**: `complete` crea transacción + marca `isDone`; `reopen` borra transacción vinculada; guards `sourceShoppingListId`.
- **`RoomShoppingListRepository`**: módulo grande (≈450 líneas):
  - `finalizePurchase(listId, categoryId, date, paymentMethod, allowMissingPrices)` → valida categoría, verifica `actualUnitPriceInCents`, calcula totales (`finalizableTotalInCents` overflow-aware), marca `COMPLETED`, `markAllItemsPurchased`, luego `syncCompletedPurchase` que crea **transacción directa** (si DEBIT/CASH) o **PendingEntry PAYMENT** (si CREDIT); aprende `KnownProduct` por barcode.
  - `updatePurchaseSettings`, `reopen` (borra transacción/payable), `duplicate`, `applyTicketReview` (crea items + ajustes + `learnAlias` con `normalizeProductName`), `saveItem/Adjustment` con `touchList` y re-sync si completada.
- **`RoomBudgetRepository`**: `closeCycle(cycle, nextConfig)` transaccional.
- **`RoomCategoryRepository`**: `deleteIfUnused` verifica `observeUsedCategoryIds()`.
- **`RoomSavingsRepository`**: `delete` hace `unlinkTransactions` transaccional.

---

## 6. Navegación (`navigation/FinanceApp.kt`)

Único `NavHost` (`FinanceApp:88`), sin transiciones animadas, `popUpTo("home")` para navegación modular.

**Rutas string:**

| Ruta | Pantalla |
|---|---|
| `home` | `HomeScreen` |
| `add/{type}` | `AddTransactionScreen` (type=INCOME/EXPENSE) |
| `edit/{type}/{transactionId}` | `AddTransactionScreen` en modo edición |
| `history` | `HistoryScreen` (filtros, búsqueda, edición/eliminado/duplicado, exportar CSV/PDF) |
| `statistics` | `StatisticsScreen` |
| `fixed` | `FixedEntriesScreen` |
| `pending` | `PendingEntriesScreen` |
| `list?query={query}` | `ShoppingListsScreen` |
| `list/{listId}` | `ShoppingListScreen` (detalle, escaneo barcode, ticket OCR, ajustes) |
| `savings` | `SavingsScreen` |
| `settings` | `SettingsScreen` |

`FloatingModuleBar` (barra flotante bottom) visible solo en `topLevelRoutes = {home, history, statistics, fixed, pending, savings, list}` (`FinanceApp:214`). Configurable (rutas visibles, labels, tamaño) vía `FloatingModuleBarPreferences` (SharedPreferences).

Deep-links desde widgets/notificaciones vía `MainActivity.EXTRA_TRANSACTION_TYPE / EXTRA_DESTINATION / EXTRA_EDIT_TRANSACTION_ID`.

---

## 7. Pantallas (presentación)

### Home (`presentation/home/`)
`HomeViewModel:70` — combina `observeAll transactions + categories + budget + history + periodViewState (pinned/selected + currentDate ticker)`. Calcula `periodIncome/Expense`, `recent (5)`, `spending by category`, `current/next Period`. `pinPeriodView` persiste en `CyclePreferences` y refresca widgets. `saveBudget` valida `>0` y hace `upsertBudgetIncome` (transacción INCOME "Ingreso mensual/quincenal" con categoría Salario). `closeCurrentCycle` solo si `today == period.endInclusive`, crea `BudgetCycle`, genera nuevo ingreso futuro, guarda `closedCycle + nextConfig` transaccional. `budgetIncomeMutex` evita carreras.
`HomeScreen:92` — chips registro rápido, card presupuesto con `PeriodViewSelector` (Actual/Próxima + pin), métricas INGRESOS/GASTOS/RESTANTE, botones Historial/Cerrar ciclo, lista gastos por categoría (Top 5), últimos movimientos con `TransactionRow` + diálogo detalle. Auto-cierre con `LaunchedEffect(automaticCycleClose)`.

### Transacciones (`presentation/transactions/`)
- **Add** (`AddTransactionViewModel/Screen`): crear/editar, validación, guardado transaccional, feedback Snackbar, `isSaving` anti-duplicado, refresh widgets.
- **History** (`HistoryViewModel/Screen`): lista con filtros por fecha/tipo/categoría/búsqueda, acciones editar/eliminar/duplicar, exportación CSV (`core/CsvExporter`) y PDF (`core/HistoryPdfWriter`) con `FileProvider`.

### Estadísticas (`presentation/statistics/`)
`StatisticsScreen:99` + `StatisticsViewModel` — reportes por rango (`CURRENT_BUDGET`, semanal, mensual, anual, `CUSTOM` con DatePicker). Filtros por ciclo (`BudgetCycleSchedule`) y categoría. `calculateStatistics` produce `StatisticsReport(balance, income, expense, count, avgExpense, expenseByCategory)`. Gráfico donut Ingresos vs Gastos, `BalanceCard`, `ActivityCard`, `TrendComparisonCard` (delta % vs periodo anterior con flechas), barras por categoría con límite de presupuesto.

### Fijas (`presentation/fixed/`)
`FixedEntriesScreen/ViewModel` — CRUD de `FixedEntry`, toggle activo, ejecución manual ("post"), programación (`scheduleHour/specificDate`). `FixedEntryWorker` (WorkManager) se dispara cada 15 min.

### Pendientes (`presentation/pending/`)
`PendingEntriesScreen/ViewModel` — obligaciones por pagar/cobrar (`PAYMENT/COLLECTION`), con fecha y `reminderMinutesOfDay`. `complete` genera transacción, `reopen` la revierte. `PendingReminderWorker/Scheduler` notifica diarios. Guard: entradas con `sourceShoppingListId` no editables directamente.

### Listas de compra (`presentation/list/`)
`ShoppingListsScreen + ShoppingListScreen`:
- Crear/editar/duplicar/eliminar listas (status `PENDING → SHOPPING → COMPLETED`).
- Items con `estimated/actualUnitPrice, quantity, barcode, isPurchased, isIdentified`.
- Escaneo barcode (ML Kit + `libs.google.code.scanner`) + lookup `KnownProduct` / `OpenFoodFactsProductCatalogRepository` (online opcional) + alias aprendidos (`ProductRecognitionAlias`).
- Ticket OCR (`mlkit.text.recognition` + `ListReceiptParser`) → `applyTicketReview`.
- Ajustes (descuentos/recargos) `ShoppingAdjustment`.
- Finalizar compra: elige categoría gasto + fecha + método pago (`CASH/DEBIT` → transacción EGRESO; `CREDIT` → PendingEntry PAYMENT vinculada). Si la lista ya completada, cualquier edición de items/ajustes re-sincroniza `syncCompletedPurchase`.

### Ahorros (`presentation/savings/`)
`SavingsScreen/ViewModel` — metas `SavingsGoal` con `targetAmountInCents`, vinculación de transacciones via `savingsGoalId`, progreso %, `complete/reopen/delete` (unlink transaccional).

### Ajustes (`presentation/settings/`)
`SettingsScreen/ViewModel` — `AppAppearance` (modo SYSTEM/LIGHT/DARK + seeds `primary/accent` con auto-corrección contraste), cierre automático (`CyclePreferences.automaticClose + automaticCloseTime` default `21:00`), `closingDays` (días de apertura/cierre), `FloatingModuleBar` config, gestión categorías (crear/renombrar/límite `budgetLimitInCents`/desactivar/borrar si no usada), backup restore.

---

## 8. Workers & scheduling (`FinanceApplication:13`)

Registrados una vez en `Application.onCreate` (no desde UI):

| Worker/Scheduler | Qué hace | Cadencia |
|---|---|---|
| `FixedEntryScheduler.ensureScheduled` | `FixedEntryWorker` publica entradas fijas vencidas (`nextRunAt <= now`) | `PeriodicWork 15 min`, `KEEP` |
| `BudgetAlertScheduler` | `BudgetAlertWorker` notifica si `budgetUsagePercent >= umbral` (evalúa `BudgetAlertEvaluator`) | Periódico |
| `PendingReminderScheduler.createNotificationChannel` | `PendingReminderWorker` notifica pendientes del día (`reminderMinutesOfDay`) | Diario |
| `WidgetRefreshScheduler.ensureScheduled` | `WidgetRefreshWorker` refresca widgets en background | Periódico |

---

## 9. Widgets (`widget/` + `AndroidManifest.xml`)

**11 Glance widgets**, cada uno con `*Widget.kt` + `*WidgetReceiver` + `*_widget_info.xml`:

1. `FinanceWidget` — balance general
2. `IncomeExpenseWidget` — ingresos vs gastos del periodo
3. `BudgetProgressWidget` — % uso presupuesto
4. `StatisticsWidget` — resumen estadístico
5. `RecentMovementsWidget` — últimos movimientos
6. `PendingRemindersWidget` — pendientes por pagar/cobrar
7. `SavingsGoalsWidget` — progreso metas
8. `FixedCommitmentsWidget` — fijas activas
9. `QuickAccessWidget` — atajos Registrar ingreso/gasto
10. `DailySpendingWidget` — gasto de hoy + daily allowance
11. `CategoryLimitsWidget` — límites por categoría

**Datos** en `WidgetData.kt`: `loadCoreSnapshot` (filtra `belongsToActiveBudgetCycle`, calcula `topExpenseCategories 4`, `recent 6`, `todayExpense`, `previousCycleExpense`, `dailyAllowance`), `loadPendingSnapshot (5)`, `loadSavingsSnapshot`, `loadFixedSnapshot (3)`. **Tema** en `WidgetTheme.kt`, **formato** en `WidgetFormatting.kt` (usa `NumberFormat` con `DOP` pero simplificado para RemoteViews). **Refresh central:** `updateAllFinanceWidgets(context)` llama `updateAll()` en los 11 (`WidgetData:327`).

---

## 10. Core / utilidades (`core/`)

- **`MoneyFormatter.kt:7`** — `format(cents: Long): String` con `NumberFormat.getCurrencyInstance(es-DO, DOP)` + `parseToCents(String): Long?` (soporta `,`/` .`).
- **`CyclePreferences`** (SharedPreferences): `pinnedBudgetView (CURRENT/NEXT)`, `automaticClose: Boolean`, `automaticCloseTime: LocalTime`.
- **`EntryDisplayPreferences`**: tamaño tarjeta entrada, etc.
- **`BudgetAlertPreferences`**: umbrales alerta.
- **`AppearancePreferences`** (`ui/theme/AppAppearance.kt`): `AppAppearance(primaryArgb, accentArgb, themeMode)`, `ensureColorsCompatible(dark)`, `setThemeModeWithAutoCorrection`.
- **`ToastNotifier / showToast`** — feedback obligatorio tras cada escritura.
- **`CsvExporter / HistoryPdfWriter`** — exportación historial.

---

## 11. Tema y apariencia

`ui/theme/{Theme,Color,Type,AppAppearance}.kt` — Material 3 `PersonalFinanceTrackerTheme(darkTheme, primarySeed, accentSeed)` genera `ColorScheme` dinámico. Nunca hardcodear colores; usar `MaterialTheme.colorScheme/typography/shapes`. `MainActivity:65` maneja `AppThemeMode.SYSTEM/LIGHT/DARK`, corrige colores por contraste y aplica `isAppearanceLightStatusBars`.

---

## 12. Lógica de ciclo presupuestario (resumen operativo)

1. Usuario configura `amount + period + closingDays` (ej. quincenal `1:14,15:31`).
2. `activeBudgetPeriod` calcula periodo actual a partir de `cycleSchedules` ±2 meses.
3. Transacciones se filtran con `belongsToActiveBudgetCycle` (fecha + `cycleStartedAt` guard).
4. Ingreso sintético "Ingreso quincenal/mensual" se crea/actualiza para representar el presupuesto como transacción INCOME (permite balance `income - expense`).
5. Cierre manual solo si `today == period.endInclusive`; crea `BudgetCycle` histórico, avanza `cycleStart` a `nextPeriod.start`, crea nuevo ingreso futuro. Cierre automático idem si `now >= closeTime (21:00)`.
6. Widgets/estadísticas consumen el mismo cálculo, respetando `pinnedBudgetView`.

---

## 13. Navegación inferior / UX global

- `FloatingModuleBar` flotante (pill) con iconos + labels opcionales, tamaño configurable, rutas visibles configurables. Se oculta en `add/edit/settings`.
- Feedback obligatorio: todo `Guardar/Eliminar/Cerrar` muestra `Toast/Snackbar` en español (`"Presupuesto guardado correctamente."`, `"Ciclo cerrado correctamente"`…).
- Validaciones visibles: `"El monto debe ser mayor que cero."` / `"Introduce un monto válido"` antes de persistir.
- Anti-duplicado: `isSaving/closingCycle` booleans deshabilitan botón durante la operación.
- Empty/loading: `SkeletonHost + SkeletonCard/Line/Circle` para cada pantalla; empty states en español (`"Todavía no tienes movimientos."`).

---

## 14. Seguridad & offline

- **100% offline** — Room es fuente de verdad. `OpenFoodFactsProductCatalogRepository` es la única feature online y es opcional (lookup de barcode).
- Sin auth, sin sync, sin backend. Backup vía CSV/PDF local + restore con deduplicación.
- Permiso único: `POST_NOTIFICATIONS` (alertas presupuesto / recordatorios pendientes).

---

## 15. Estructura de archivos clave (rutas absolutas)

```
app/src/main/java/com/angel/mony/
  FinanceApplication.kt
  MainActivity.kt
  navigation/FinanceApp.kt:40
  data/local/database/FinanceDatabase.kt:41
  di/DatabaseModule.kt:277
  core/MoneyFormatter.kt:7
  domain/model/BudgetCycleCalculator.kt
  domain/model/ShoppingList.kt:102
  presentation/home/HomeViewModel.kt:64
  presentation/home/HomeScreen.kt:93
  widget/WidgetData.kt:327
  widget/FinanceWidget.kt (+ 10 más)
app/src/main/AndroidManifest.xml
app/schemas/com.angel.mony.data.local.database.FinanceDatabase/*.json
settings.gradle.kts (rootProject.name = "Mony")
app/build.gradle.kts (namespace/applicationId = "com.angel.mony")
app/src/main/res/values/strings.xml (app_name = "Mony")
```

---

## 16. Para implementar algo nuevo — checklist (de AGENTS.md)

- [ ] Inspeccionar implementación existente antes de asumir estructura genérica.
- [ ] Respetar `updateAllFinanceWidgets()` tras cambios visibles en widgets.
- [ ] Dinero siempre en `amountInCents` + `MoneyFormatter`.
- [ ] Validar antes de persistir; mensajes en español; feedback tras éxito/fracaso.
- [ ] Si tocas Entity → incrementar `FinanceDatabase.version` + añadir `Migration` + registrar + generar schema + commit JSON.
- [ ] No DAO en ViewModel/UI; no crear `TransactionCard2` si ya existe componente.
- [ ] Verificar flujo completo (validación → guardado → feedback → refresh → estado final).

---

*Generado el 2026-08-31 inspeccionando el repo real — refleja la implementación actual de **Mony** (`com.angel.mony`, DB v15, 11 widgets, lista→transacción/pending, etc.). Build verificado: `assembleDebug` + `testDebugUnitTest` OK tras el renombrado.*
