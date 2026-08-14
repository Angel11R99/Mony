# AGENTS.md

Single-module Android app (`:app`) in Kotlin + Jetpack Compose (Material3), Room, Hilt, WorkManager, and Glance app widgets. No README and no CI. All UI text and domain labels are **Spanish**; currency is Dominican Peso.

## Commands

- Build: `.\gradlew.bat assembleDebug` (Windows) or `./gradlew assembleDebug`
- Unit tests (JUnit4, in `app/src/test/`): `.\gradlew.bat testDebugUnitTest`
- One test class: `.\gradlew.bat testDebugUnitTest --tests "com.example.personalfinancetracker.presentation.home.BudgetCycleTest"`
- Hilt, KSP, and the Kotlin Compose plugin require a full Gradle build before code referencing generated symbols compiles. There is no separate lint/typecheck step; the build itself is the check.

## Architecture

- `domain/model/` — pure Kotlin, no Android imports. Contains the core business logic (budget cycle math in `BudgetCycleCalculator.kt`, `DateRange`, `MoneyFormatter`-adjacent helpers) and is the primary target for unit tests.
- `domain/repository/` — interfaces; `data/repository/RoomRepositories.kt` holds the Room implementations (one class per repo, `@Inject`).
- `data/local/` — Room DAOs/entities; `data/mapper/FinanceMappers.kt` maps entity ↔ domain via `toEntity()`/`toDomain()`.
- `presentation/` — one subpackage per screen (`home`, `statistics`, `fixed`, `pending`, `transactions`, `settings`) with a ViewModel + Screen. Shared Compose components live in `presentation/components/`.
- `navigation/FinanceApp.kt` — single `NavHost` with string routes: `home`, `add/{type}`, `edit/{type}/{transactionId}`, `history`, `statistics`, `fixed`, `pending`, `settings`. New screens must be registered here.
- `ui/theme/` — theming + `AppearancePreferences`/`AppAppearance` (persisted SharedPreferences; seed colors chosen by user).
- `widget/` — 4 Glance app widgets. ViewModels and widgets access repos via **Hilt `@EntryPoint` + `EntryPointAccessors.fromApplication`** (see `WidgetData.kt`, `FixedEntryWorker.kt`), not constructor injection.

## Gotchas

- **Room migrations are hand-written, no destructive fallback.** `FinanceDatabase` is at version 9 (`exportSchema = true`, schemas committed to `app/schemas/.../FinanceDatabase/N.json`). Any entity change requires bumping the version, writing a new `Migration` in `di/DatabaseModule.kt` (migrations are 1→2 … 8→9), and committing the freshly generated schema JSON. Missing a migration crashes existing installs; there is no `.fallbackToDestructiveMigration()`.
- **Money is stored in integer cents** (`amountInCents`). Format/parse through `core/MoneyFormatter.kt` (es-DO locale, `DOP`). Never format raw cents inline.
- Default categories are seeded in `DatabaseModule.database()`'s `onCreate` callback — new defaults go there.
- Widgets do not auto-refresh on data changes: call `updateAllFinanceWidgets(context)` after anything that changes transactions/budget/appearance (theme change, transaction save, fixed/pending post, budget close).
- Periodic work: `FixedEntryWorker` posts due fixed entries; it is scheduled once in `FinanceApplication.onCreate` via `FixedEntryScheduler.ensureScheduled` (unique periodic 15-min work, `KEEP` policy). New recurring jobs must be registered there.
- `minSdk = 24` with core library desugaring enabled, so `java.time` is available. Dates are stored as epoch days/millis (`*EpochDay` / `*EpochMillis` columns) and `LocalDate`/`Instant` in domain models.
- Deleting a transaction re-links a fixed entry's `lastAdded` state (see `RoomTransactionRepository.delete` in `RoomRepositories.kt`); editing/deleting fixed or pending entries has cross-table logic (delete linked transaction on pending reopen). Preserve the transaction-grouping logic when touching these repos.