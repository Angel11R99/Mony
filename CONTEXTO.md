# Mony — Contexto de la App

> **App:** Mony · **Paquete:** `com.angel.mony` · **Stack:** Kotlin · Jetpack Compose (Material 3) · Room · Hilt · Glance · WorkManager · Navigation Compose · Coroutines/Flow
> **Idioma UI:** Español · **Moneda:** DOP / RD$ (`es-DO`) · **Offline-first** · `minSdk 24` · `targetSdk 36` · `FinanceDatabase v15` (`exportSchema = true`)

---

## 1. Propósito

Mony es un tracker personal **rápido, simple y offline** para registrar ingresos/gastos sin fricción, con presupuesto por ciclo, entradas fijas/pendientes, listas de compra con escaneo, metas de ahorro, estadísticas comparativas, 11 widgets y cierre de ciclo automático.

Principios: **Simple, Rápido, Offline, Predecible, Consistente, Seguro con los datos** — ver `AGENTS.md`.

---

## 2. Arquitectura (Clean + MVVM)

```
app/src/main/java/com/angel/mony/
├── domain/          # Kotlin puro. Reglas de negocio testeables
│   ├── model/       # FinanceTransaction, BudgetConfig, BudgetCycle*, Category, FixedEntry, PendingEntry, SavingsGoal, ShoppingList*, etc.
│   ├── repository/  # Interfaces: TransactionRepository, BudgetRepository, CategoryRepository, FixedEntryRepository, PendingEntryRepository, SavingsRepository, ShoppingListRepository, ProductCatalogRepository
│   └── usecase/     # SaveTransaction
├── data/
│   ├── local/       # 12 entities Room, 8 DAOs, FinanceDatabase (v15)
│   ├── mapper/      # FinanceMappers.kt — Entity ↔ Domain
│   └── repository/  # RoomRepositories.kt + OpenFoodFactsProductCatalogRepository
├── presentation/    # Un subpaquete por pantalla (Screen + ViewModel)
├── navigation/      # FinanceApp.kt (único NavHost)
├── ui/theme/        # Theme, Color, Type, AppAppearance
├── widget/          # 11 widgets Glance
├── di/              # DatabaseModule + RepositoryModule
└── core/            # MoneyFormatter, CyclePreferences, CsvExporter, HistoryPdfWriter, etc.
```

**Reglas clave:**
- ViewModel → Repository interface → Room. Nunca DAO directo en UI.
- `MoneyFormatter` (`es-DO`, DOP) es el único lugar de formateo. Todo dinero en `Long amountInCents`.
- Fechas con `java.time` (`LocalDate`/`Instant`), persistidas como `*EpochDay` / `*EpochMillis`.
- Tras cualquier cambio que afecte widgets/apariencia → `updateAllFinanceWidgets(context)`.
- Widgets/workers usan Hilt `@EntryPoint` + `EntryPointAccessors`.

---

## 3. Base de datos Room

**DB:** `personal_finance.db` — `FinanceDatabase` (`data/local/database/FinanceDatabase.kt`)

**12 Entities:** `CategoryEntity`, `TransactionEntity`, `BudgetConfigEntity`, `BudgetCycleEntity`, `FixedEntryEntity`, `PendingEntryEntity`, `SavingsGoalEntity`, `ShoppingListEntity`, `ShoppingListItemEntity`, `ShoppingAdjustmentEntity`, `KnownProductEntity`, `ProductRecognitionAliasEntity`.

**Migraciones** en `di/DatabaseModule.kt` — `1→2 … 14→15`. Schema JSON en `app/schemas/`. **Prohibido** `fallbackToDestructiveMigration()`.

| Migración | Cambio principal |
|---|---|
| 1→2 | Crea `budget_config` |
| 2→3 | `cycleStart/cycleStartedAt` + `budget_cycle_history` |
| 3→5 | `incomeTransactionId` + crea `fixed_entries` |
| 5→7 | `scheduleMode/nextRunAt/lastAdded` en fixed + `fixedEntryId` en transactions |
| 7→9 | `closingDays` (String) + crea `pending_entries` |
| 9→11 | `reminderMinutesOfDay` + `budgetLimitInCents` en categories |
| 11→13 | Crea `savings_goals` + `savingsGoalId` + `completedAtEpochMillis` |
| 13→15 | Crea `shopping_lists/items/adjustments/known_products` + `product_recognition_aliases` |

**Seed `onCreate`:** 21 categorías por defecto en `DatabaseModule.database()`.

---

## 4. Modelo de dominio (`domain/model/`)

- **`FinanceTransaction`**: `id, amountInCents, type (INCOME/EXPENSE), categoryId, description, date, createdAt/updatedAt, fixedEntryId?, savingsGoalId?`
- **`BudgetConfig`**: `amountInCents, period (MONTHLY/FORTNIGHTLY), cycleStart, cycleSchedules: List<BudgetCycleSchedule>`. `BudgetCycleSchedule(openingDay, closingDay)` serializado como `"15:31,1:14"`.
- **`BudgetCycleCalculator.kt`** — núcleo presupuestario: `activeBudgetPeriod`, `belongsToActiveBudgetCycle`, `canManuallyCloseBudgetCycle`, `shouldAutomaticallyCloseBudgetCycle`.
- **`FixedEntry`**: `type, description, amountInCents, categoryId, isActive, manualDateMode, scheduleMode, scheduleHour, nextRunAt, lastAddedAt/Date`.
- **`PendingEntry`**: `type (PAYMENT/COLLECTION)`, `reminderMinutesOfDay?, isDone, transactionId?, sourceShoppingListId?`.
- **`SavingsGoal`**: `targetAmountInCents`, progreso %, `isCompleted`, `canComplete`.
- **`ShoppingList/Item/Adjustment/KnownProduct`** — totales con `MoneySum` overflow-safe.

---

## 5. Repositorios (`data/repository/RoomRepositories.kt`)

Efectos cross-table preservados (no simplificar sin entender):

- **`RoomTransactionRepository.delete`**: borra transacción, re-vincula `fixedEntry.lastAdded`, bloquea borrado si pertenece a lista completada o crédito.
- **`RoomFixedEntryRepository.post`**: transaccional (entry + transaction).
- **`RoomPendingEntryRepository`**: `complete` crea transacción + marca `isDone`; `reopen` borra transacción vinculada.
- **`RoomShoppingListRepository`** (≈450 líneas): `finalizePurchase` valida categoría, calcula totales overflow-aware, crea transacción (DEBIT/CASH) o PendingEntry (CREDIT), aprende `KnownProduct`. `applyTicketReview` crea items + ajustes + alias.
- **`RoomBudgetRepository.closeCycle`**: transaccional (cycle + nextConfig).
- **`RoomSavingsRepository.delete`**: `unlinkTransactions` transaccional.

---

## 6. Navegación (`navigation/FinanceApp.kt`)

Único `NavHost`, `popUpTo("home")` para navegación modular.

| Ruta | Pantalla |
|---|---|
| `home` | `HomeScreen` |
| `add/{type}` | `AddTransactionScreen` (INCOME/EXPENSE) |
| `edit/{type}/{transactionId}` | `AddTransactionScreen` modo edición |
| `history` | `HistoryScreen` (filtros, búsqueda, exportar CSV/PDF) |
| `statistics` | `StatisticsScreen` |
| `fixed` | `FixedEntriesScreen` |
| `pending` | `PendingEntriesScreen` |
| `list?query={query}` | `ShoppingListsScreen` |
| `list/{listId}` | `ShoppingListScreen` (escaneo, ticket OCR) |
| `savings` | `SavingsScreen` |
| `settings` | `SettingsScreen` |

`FloatingModuleBar` (barra flotante bottom) visible en rutas de nivel superior. Deep-links desde widgets/notificaciones vía `MainActivity`.

---

## 7. Pantallas principales

- **Home**: chips registro rápido, card presupuesto con selector de período, métricas INGRESOS/GASTOS/RESTANTE, gastos por categoría, últimos movimientos. Auto-cierre con `LaunchedEffect`.
- **Add/Edit Transacción**: validación, guardado transaccional, feedback Snackbar, `isSaving` anti-duplicado, refresh widgets.
- **History**: filtros por fecha/tipo/categoría/búsqueda, acciones editar/eliminar/duplicar, exportación CSV/PDF.
- **Statistics**: reportes por rango (presupuesto actual, semanal, mensual, anual, personalizado). Filtros por ciclo y categoría. Donut Ingresos vs Gastos, barras por categoría con límite.
- **Fixed**: CRUD de `FixedEntry`, toggle activo, ejecución manual. `FixedEntryWorker` (WorkManager) cada 15 min.
- **Pending**: obligaciones por pagar/cobrar, fecha y recordatorio. `complete` genera transacción, `reopen` la revierte.
- **Shopping Lists**: crear/editar/duplicar/eliminar listas (PENDING → SHOPPING → COMPLETED). Items con escaneo barcode + ticket OCR. Finalizar: categoría + fecha + método pago → transacción o PendingEntry.
- **Savings**: metas con `targetAmountInCents`, vinculación de transacciones, progreso %.
- **Settings**: apariencia (SYSTEM/LIGHT/DARK + seeds), cierre automático, closing days, FloatingModuleBar, gestión categorías, backup restore.

---

## 8. Workers & scheduling

Registrados en `Application.onCreate` (no desde UI):

| Worker | Qué hace | Cadencia |
|---|---|---|
| `FixedEntryWorker` | Publica entradas fijas vencidas | 15 min |
| `BudgetAlertWorker` | Notifica si uso ≥ umbral | Periódico |
| `PendingReminderWorker` | Notifica pendientes del día | Diario |
| `WidgetRefreshWorker` | Refresca widgets | Periódico |

---

## 9. Widgets (11 Glance)

`FinanceWidget`, `IncomeExpenseWidget`, `BudgetProgressWidget`, `StatisticsWidget`, `RecentMovementsWidget`, `PendingRemindersWidget`, `SavingsGoalsWidget`, `FixedCommitmentsWidget`, `QuickAccessWidget`, `DailySpendingWidget`, `CategoryLimitsWidget`.

**Datos** en `WidgetData.kt`, **tema** en `WidgetTheme.kt`, **formato** en `WidgetFormatting.kt`. **Refresh central:** `updateAllFinanceWidgets(context)`.

---

## 10. Core / utilidades

- **`MoneyFormatter`** — `format(cents)` / `parseToCents(String)` con `NumberFormat` DOP.
- **`CyclePreferences`**: `pinnedBudgetView`, `automaticClose`, `automaticCloseTime`.
- **`AppearancePreferences`**: `AppAppearance(primaryArgb, accentArgb, themeMode)`.
- **`CsvExporter / HistoryPdfWriter`** — exportación historial.

---

## 11. Tema y apariencia

Material 3 `PersonalFinanceTrackerTheme(darkTheme, primarySeed, accentSeed)` genera `ColorScheme` dinámico. Nunca hardcodear colores; usar `MaterialTheme.colorScheme/typography/shapes`.

---

## 12. Lógica de ciclo presupuestario

1. Usuario configura `amount + period + closingDays`.
2. `activeBudgetPeriod` calcula periodo actual a partir de `cycleSchedules`.
3. Transacciones se filtran con `belongsToActiveBudgetCycle`.
4. Ingreso sintético se crea/actualiza para balance `income - expense`.
5. Cierre manual solo si `today == period.endInclusive`; cierre automático a las 21:00.
6. Widgets/estadísticas consumen el mismo cálculo.

---

## 13. Seguridad & offline

- **100% offline** — Room es fuente de verdad. `OpenFoodFactsProductCatalogRepository` es la única feature online y es opcional.
- Sin auth, sin sync, sin backend. Backup vía CSV/PDF local + restore con deduplicación.
- Permiso único: `POST_NOTIFICATIONS`.

---

## 14. Para implementar algo nuevo

- [ ] Inspeccionar implementación existente antes de asumir estructura genérica.
- [ ] Respetar `updateAllFinanceWidgets()` tras cambios visibles en widgets.
- [ ] Dinero siempre en `amountInCents` + `MoneyFormatter`.
- [ ] Validar antes de persistir; mensajes en español; feedback tras éxito/fracaso.
- [ ] Si tocas Entity → incrementar versión + migración + schema JSON.
- [ ] No DAO en ViewModel/UI; no crear componente duplicado.
- [ ] Verificar flujo completo (validación → guardado → feedback → refresh → estado final).

---

*Generado inspeccionando el repo real — refleja la implementación actual de Mony.*
