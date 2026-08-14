# AGENTS.md

Single-module Android app (`:app`) in Kotlin + Jetpack Compose (Material3), Room, Hilt, WorkManager, and Glance app widgets.

The application is a personal finance tracker focused on fast local/offline registration of incomes, expenses, budget-cycle information, fixed entries, pending entries, statistics, and quick actions through Android widgets.

There is currently no README and no CI.

All UI text and domain labels are **Spanish**.

Currency is **Dominican Peso (DOP / RD$)**.

The application is fundamentally **offline-first**. Core functionality must continue working without Internet access.

---

# Core Development Rule

Before making any change:

> **Inspect the existing implementation first. Do not assume the project follows a generic Android structure just because this document describes one.**

The repository itself is the primary implementation reference.

This document defines constraints and conventions, but the agent must also learn from existing code.

When a new stable convention is discovered in the project, follow it consistently in future changes.

Do not introduce a competing pattern unless there is a strong technical reason.

---

# Definition of Done

A feature is NOT complete merely because:

* it compiles;
* data reaches Room;
* a button executes;
* a ViewModel method runs;
* no exception appears.

A feature is complete only when the full user flow is correctly handled.

For every user action, evaluate:

```text
Initial state
    ↓
User action
    ↓
Validation
    ↓
Processing
    ↓
Success
    ↓
User feedback
    ↓
Dependent data refresh
    ↓
Final visible state
```

and also:

```text
Failure
    ↓
Useful error feedback
    ↓
Recoverable state
```

Before finishing a feature always ask:

> What happens before, during, after, if it succeeds, and if it fails?

If any of those states is undefined, the feature is not complete.

---

# User Feedback Is Mandatory

Any important action that changes persistent data or settings must give the user understandable feedback unless the result is immediately obvious from the UI.

Examples:

* save settings;
* register income;
* register expense;
* edit transaction;
* delete transaction;
* close budget cycle;
* add fixed entry;
* post pending entry;
* update appearance;
* change closing day.

Never implement:

```text
Tap Save
    ↓
Database updated
    ↓
Nothing visible happens
```

The user must not wonder whether the action worked.

Appropriate feedback may include:

* Snackbar;
* inline success state;
* navigation;
* visible state refresh;
* dialog dismissal;
* button state change.

Prefer Snackbar for short non-critical confirmations.

Examples:

```text
Configuración guardada correctamente.
Gasto registrado correctamente.
Ingreso guardado.
Movimiento actualizado.
Movimiento eliminado.
Cierre realizado correctamente.
```

Do not show technical implementation details to the user.

---

# Commands

## Build

Windows:

```bash
.\gradlew.bat assembleDebug
```

Unix-like systems:

```bash
./gradlew assembleDebug
```

## Unit tests

```bash
.\gradlew.bat testDebugUnitTest
```

## Single test class

```bash
.\gradlew.bat testDebugUnitTest --tests "com.example.personalfinancetracker.presentation.home.BudgetCycleTest"
```

Hilt, KSP, and the Kotlin Compose plugin require a full Gradle build before code referencing generated symbols compiles.

There is no separate lint/typecheck step.

The Gradle build itself is the primary compile-time verification.

A task should not be considered complete until the relevant build succeeds when practical.

---

# Architecture

The project uses a layered architecture similar to Clean Architecture + MVVM.

Do not convert it to another architecture without explicit need.

The current structure is:

```text
domain/
data/
presentation/
navigation/
ui/
widget/
di/
core/
```

The important rule is responsibility separation, not folder naming alone.

---

# Domain Layer

`domain/model/`

Pure Kotlin.

No Android imports.

Contains core business rules and models.

Examples include:

* budget-cycle math in `BudgetCycleCalculator.kt`;
* `DateRange`;
* date-related domain calculations;
* financial-domain models.

This layer is the primary target for unit tests.

Do not introduce:

```text
Context
Activity
Fragment
Compose
Room
SharedPreferences
Glance
```

into domain code.

Business rules should remain independently testable.

---

# Domain Repositories

`domain/repository/`

Contains repository interfaces.

Presentation and domain logic should depend on these abstractions, not Room implementations.

Example dependency direction:

```text
ViewModel
    ↓
Repository interface
    ↓
Room implementation
```

Do not inject Room DAO directly into ViewModels.

---

# Data Layer

`data/repository/RoomRepositories.kt`

Contains Room repository implementations.

Currently one class per repository using `@Inject`.

Do not split or reorganize this file purely for stylistic preference unless the task justifies it.

Preserve cross-table behaviors implemented here.

---

# Room Mappers

`data/mapper/FinanceMappers.kt`

Maps:

```text
Entity ↔ Domain
```

using conventions such as:

```kotlin
toEntity()
toDomain()
```

Do not create another independent mapping convention without need.

Before adding new manual mapping logic, check this file.

---

# Presentation

`presentation/`

One subpackage per screen:

```text
home
statistics
fixed
pending
transactions
settings
```

Typical structure:

```text
Screen
ViewModel
UI state/events when required
```

Shared Compose components belong in:

```text
presentation/components/
```

Before creating a new component, search for an equivalent existing component.

Do not create:

```text
TransactionCard2
CustomTransactionCard
NewTransactionItem
```

if an existing component can reasonably support the new use case.

---

# Navigation

`navigation/FinanceApp.kt`

Contains the single `NavHost`.

Current string routes include:

```text
home
add/{type}
edit/{type}/{transactionId}
history
statistics
fixed
pending
settings
```

All new screens must be integrated here unless navigation architecture changes explicitly.

Do not create a second NavHost or parallel navigation system.

Do not migrate to typed navigation purely because it is newer.

The existing navigation strategy has priority unless the task requires change.

---

# Theme and Appearance

`ui/theme/`

Contains:

* application theme;
* appearance configuration;
* `AppearancePreferences`;
* `AppAppearance`;
* seed colors selected by the user.

Appearance preferences currently use SharedPreferences.

Do not migrate them to DataStore merely because DataStore is generally recommended unless there is an explicit migration task.

New UI must respect:

* current color scheme;
* typography;
* shapes;
* dark mode;
* user-selected appearance;
* Material3 conventions.

Do not hardcode random colors inside Composables.

Prefer:

```kotlin
MaterialTheme.colorScheme
MaterialTheme.typography
MaterialTheme.shapes
```

and existing theme abstractions.

---

# Visual Consistency

Before creating or redesigning UI, inspect existing screens and shared components.

Identify:

* padding patterns;
* card shapes;
* button styles;
* spacing;
* icon usage;
* typography hierarchy;
* color semantics;
* section headers;
* dialogs;
* bottom sheets;
* Snackbar behavior.

New UI should look like it belongs to the same app.

Do not redesign unrelated screens while implementing a functional task.

---

# Material Icons

Use Material Icons Extended for application iconography where the project already follows that convention.

Do not use emojis as functional interface icons.

Use icons semantically.

Examples:

```text
Restaurant → alimentación
DirectionsCar → transporte
Home → vivienda
MedicalServices → salud
Savings → ahorro
Payments → ingresos
ReceiptLong → gastos
```

---

# UX Completeness

Every interactive flow must consider:

```text
Idle
Input
Validation
Processing
Success
Error
Final state
```

Not every screen needs visible representations of all six states, but the agent must consciously determine which ones apply.

---

# Save Actions

Any Save action must define:

1. what makes the data valid;
2. whether Save is enabled;
3. whether repeated taps are possible;
4. what occurs during persistence;
5. what confirms success;
6. what occurs on failure;
7. whether navigation changes;
8. what UI data must refresh.

Example:

```text
User changes closing day
    ↓
Save enabled
    ↓
User taps Save
    ↓
Value validated
    ↓
Persisted
    ↓
Snackbar:
"Configuración guardada correctamente."
    ↓
Save disabled until another change
```

Do not leave successful saves silent.

---

# Prevent Duplicate Actions

Any write operation that could create duplicate financial records should guard against rapid repeated execution.

Examples:

```text
Add expense
Add income
Post fixed entry
Post pending entry
Close budget
```

Use the existing ViewModel/UI state pattern.

Possible approach:

```text
isSaving = true
button disabled
perform operation
isSaving = false
```

Do not automatically use this pattern for operations that are truly immediate if it adds unnecessary complexity.

---

# Validation

Validation errors must be visible and understandable.

Examples:

```text
El monto debe ser mayor que cero.
Selecciona una categoría.
El día seleccionado no es válido.
```

Do not rely on Room exceptions as user validation.

Validation should normally occur before persistence.

Shared business validation should live outside individual Composables.

---

# Errors

Never silently swallow errors.

Avoid patterns like:

```kotlin
try {
    save()
} catch (_: Exception) {
}
```

or:

```kotlin
runCatching {
    save()
}
```

without handling failure.

User-facing error messages must be understandable Spanish.

Do not display:

```text
SQLiteConstraintException
IllegalStateException
NullPointerException
```

to users.

---

# One-Time UI Events

Actions such as:

```text
Show Snackbar
Navigate back
Close dialog
Display error message
```

should use the project's existing one-time event/effect strategy if one exists.

Do not represent transient events as persistent state if that causes duplicate execution after recomposition.

Before introducing:

```text
Channel
SharedFlow
UiEffect
UiEvent
```

check how similar screens already handle the problem.

Use the dominant project convention.

---

# Empty States

A screen with no data should not look broken.

Examples:

```text
Todavía no tienes movimientos.
Registra tu primer gasto o ingreso para comenzar.
```

Use an action when helpful.

Do not add elaborate empty-state illustrations unless the existing visual system uses them.

---

# Loading States

Only show loading UI when the operation can take perceptible time or preventing multiple interactions is useful.

Do not add unnecessary spinners to fast local Room writes merely for decoration.

A disabled Save button may be sufficient.

---

# Money

Money is stored in integer cents:

```text
amountInCents
```

Never replace this with Float or Double.

Formatting/parsing must go through:

```text
core/MoneyFormatter.kt
```

Locale:

```text
es-DO
```

Currency:

```text
DOP
```

Never format raw cents inline.

Do not duplicate currency formatting logic inside:

* Composables;
* widgets;
* ViewModels;
* repositories.

If financial formatting behavior changes, update the shared formatter.

---

# Dates

`minSdk = 24`.

Core library desugaring is enabled.

`java.time` is available.

Dates are persisted using:

```text
*EpochDay
*EpochMillis
```

and represented in domain code through:

```text
LocalDate
Instant
```

Do not introduce another date representation without need.

Date and budget-period calculations should remain centralized.

---

# Budget Cycle

Budget-cycle logic is domain logic.

Review:

```text
BudgetCycleCalculator.kt
BudgetCycleTest
```

before modifying related behavior.

Do not reproduce budget-cycle calculations inside Screens or ViewModels.

Any budget-cycle change should receive unit tests.

---

# Room

Current `FinanceDatabase` version:

```text
9
```

Configuration:

```text
exportSchema = true
```

Schemas are committed to:

```text
app/schemas/.../FinanceDatabase/N.json
```

Never use:

```kotlin
fallbackToDestructiveMigration()
```

Existing installations contain financial data and must survive upgrades.

---

# Room Migration Procedure

Any Entity/schema change requires ALL of the following:

```text
1. Modify entity/schema.
2. Increment FinanceDatabase version.
3. Add Migration in di/DatabaseModule.kt.
4. Register migration with database builder.
5. Build project.
6. Generate updated schema JSON.
7. Commit new schema JSON.
8. Verify old data survives.
```

Migrations currently exist:

```text
1 → 2
2 → 3
...
8 → 9
```

Continue this pattern.

Never change an Entity and forget the migration.

Missing migration = crash for existing installs.

---

# Migration Safety

When writing migrations:

* preserve existing rows;
* preserve transaction IDs;
* preserve relationships;
* use safe defaults for new non-null columns;
* verify indexes and foreign keys when relevant.

Migration code is data-preservation code, not just schema code.

---

# Default Categories

Default categories are seeded inside:

```text
DatabaseModule.database()
```

using the database `onCreate` callback.

Any new default categories belong there unless the project architecture changes explicitly.

Do not hardcode default categories separately inside UI.

---

# Repository Side Effects

Repository methods contain important cross-table behavior.

Before changing them, inspect related operations completely.

For example:

`RoomTransactionRepository.delete`

currently re-links a fixed entry's `lastAdded` state.

Editing/deleting fixed or pending entries also contains cross-table logic.

Example:

```text
Pending entry reopened
    ↓
Linked transaction deleted
```

Do not simplify these methods without understanding their side effects.

A repository operation may affect more than one table.

---

# Transaction Grouping

Preserve existing transaction-grouping behavior.

Before modifying:

* fixed entries;
* pending entries;
* transaction deletion;
* transaction editing;
* budget close;

inspect how grouping identifiers and linked records are currently used.

Do not change grouping semantics incidentally.

---

# Widgets

The app currently has 4 Glance widgets.

Widget code lives in:

```text
widget/
```

Widgets are a first-class part of the application.

They must not become stale after relevant data changes.

---

# Widget Dependency Injection

Widgets and workers access repositories using:

```text
Hilt @EntryPoint
EntryPointAccessors.fromApplication
```

Examples:

```text
WidgetData.kt
FixedEntryWorker.kt
```

Do not replace this with constructor injection in Glance components unless Android/Hilt support and project architecture are intentionally changed.

Follow the existing pattern.

---

# Widget Refresh

Widgets do NOT automatically refresh when finance data changes.

After anything that changes:

```text
transactions
budget
appearance
fixed/pending posting
budget closing
```

call:

```kotlin
updateAllFinanceWidgets(context)
```

Relevant examples:

```text
Transaction saved
    ↓
Room updated
    ↓
updateAllFinanceWidgets(context)
```

If a feature changes data visible in widgets and does not call this function, the feature is incomplete.

---

# Appearance and Widgets

Theme/appearance changes also require widget refresh.

After updating appearance:

```kotlin
updateAllFinanceWidgets(context)
```

Do not assume Glance automatically follows Compose theme state.

---

# WorkManager

Periodic work currently includes:

```text
FixedEntryWorker
```

It posts due fixed entries.

Scheduling occurs once in:

```text
FinanceApplication.onCreate
```

through:

```text
FixedEntryScheduler.ensureScheduled
```

Current schedule:

```text
unique periodic work
15-minute interval
ExistingPeriodicWorkPolicy.KEEP
```

New recurring jobs must be intentionally registered there.

Do not schedule recurring WorkManager jobs from Screens or ViewModels.

---

# WorkManager Use

Do not add WorkManager for tasks that can be handled immediately.

Use it for:

* periodic jobs;
* deferred reliable background work.

Do not use it as a replacement for regular repository calls.

---

# Hilt

Respect existing Hilt modules and bindings.

Do not manually instantiate repositories that are already provided by Hilt.

Do not add service locator patterns parallel to Hilt.

For Android classes where Hilt constructor injection is unavailable, follow the project's current `@EntryPoint` approach.

---

# Gradle Dependencies

Before adding a dependency:

1. inspect existing Gradle files;
2. inspect version catalog if present;
3. check whether Jetpack already solves the problem;
4. check whether an existing dependency already solves it.

Prefer:

```text
Kotlin standard library
Android SDK
Jetpack
Google official libraries
existing dependencies
```

before external packages.

Do not add dependencies for trivial helpers.

---

# SOLID

Apply SOLID where it improves actual maintainability.

Especially:

```text
Single Responsibility
Dependency Inversion
Interface Segregation
```

Do not mechanically add interfaces/classes solely to claim SOLID compliance.

---

# KISS

Prefer simple implementations.

But:

> **KISS does not justify incomplete UX.**

Not showing feedback after Save is not simplicity.

It is an incomplete interaction.

---

# DRY

Avoid meaningful duplication.

Especially:

* financial calculations;
* money formatting;
* date logic;
* validation;
* mapping;
* widget refresh behavior;
* common Compose components.

Do not create premature abstractions for two coincidentally similar lines.

---

# YAGNI

Do not implement speculative features.

Do not add:

```text
login
cloud sync
Firebase
backend
multi-user
AI features
multi-currency
remote API
```

unless explicitly requested.

Preparing clean boundaries is enough.

Do not implement the future preemptively.

---

# Offline First

Core functionality must remain fully operational without Internet.

Do not introduce Internet as a requirement for:

* transactions;
* budget calculations;
* statistics based on local data;
* settings;
* fixed entries;
* pending entries;
* widgets.

Network features may only be additive if explicitly requested.

---

# No Authentication

The app currently has no login requirement.

Do not add:

```text
LoginScreen
UserEntity
SessionManager
TokenManager
OAuth
Firebase Authentication
```

unless explicitly requested.

---

# User Language

All visible UI text must be Spanish.

Avoid accidentally adding English UI strings such as:

```text
Save
Delete
Error
Done
Loading
```

Prefer:

```text
Guardar
Eliminar
Error
Listo
Cargando
```

Code identifiers may remain in English.

---

# Currency Semantics

Use Dominican Peso.

Prefer consistent display:

```text
RD$ 1,250.00
```

according to the project's formatter.

Do not introduce `$` alone where it could be ambiguous.

---

# Compose State

Follow existing state-management patterns.

Use lifecycle-aware collection when already established.

Prefer:

```kotlin
collectAsStateWithLifecycle()
```

where appropriate.

Do not directly perform persistence operations inside Composables.

---

# Composable Responsibilities

Composable functions should primarily:

* render state;
* emit user actions;
* handle local visual state where appropriate.

Do not place:

* Room queries;
* repository calls;
* budget calculations;
* financial persistence logic;

directly inside UI code.

---

# ViewModel Responsibilities

ViewModels should:

* expose UI state;
* coordinate domain/repository actions;
* process screen events;
* trigger one-time UI effects.

ViewModels should NOT:

* perform raw SQL;
* directly construct RoomDatabase;
* format money independently;
* duplicate domain calculations.

---

# Accessibility

New UI should consider:

* touch target size;
* contrast;
* readable text;
* content descriptions where required;
* state indication beyond color.

Do not make success/error depend solely on green/red color.

---

# Dark Mode

Any new UI must work with the current theme system.

Do not hardcode:

```text
white background
black text
```

unless the design system explicitly requires it.

---

# Settings Screens

Settings changes must define:

```text
Current value
    ↓
Edit
    ↓
Validate
    ↓
Save
    ↓
Persist
    ↓
Success feedback
    ↓
Updated state
```

A Save button that persists silently is considered incomplete.

If values have not changed, consider disabling Save if that matches the existing UX.

---

# Financial Actions

Financial write operations should be treated as high-value interactions.

For:

```text
add transaction
edit transaction
delete transaction
post fixed entry
post pending entry
close budget
```

verify:

* persistence;
* related repository side effects;
* balance refresh;
* list refresh;
* widget refresh;
* success feedback;
* error feedback.

---

# Delete Actions

Deleting financial information must be intentional.

Follow existing confirmation/undo patterns if present.

Do not introduce inconsistent confirmation behavior between similar screens.

After deletion verify all linked state.

---

# Cross-Screen Effects

When changing data, inspect all screens that depend on it.

Example:

```text
Transaction saved
```

may affect:

```text
Home
History
Statistics
Fixed
Pending
Widget
Budget totals
```

Reactive Room/Flow data should update naturally where possible.

Do not patch every screen manually when the existing observable data pipeline already handles it.

---

# Single Source of Truth

Room should remain the primary persistent source of truth for financial data.

Do not maintain independent duplicate balances or transaction lists in SharedPreferences or widget storage unless there is a documented reason.

Widgets may cache presentation data if required by Glance, but the authoritative data remains the repository/database.

---

# Existing Code Over Generic Advice

When generic Android guidance conflicts with a stable project convention, prefer the project's convention unless:

* it causes data loss;
* it is clearly broken;
* it violates an explicit task requirement;
* it creates a serious architectural problem.

Example:

The project currently uses SharedPreferences for appearance.

Do not migrate to DataStore just because DataStore is newer.

---

# Discover New Project Conventions

The agent must actively detect patterns not listed in this file.

Examples:

```text
custom UiEffect pattern
shared Snackbar host
specific spacing constants
specific result wrapper
custom form field
repository helper
widget refresh abstraction
testing utility
date utility
```

If such patterns are consistently used and correct:

> reuse them.

Do not create competing abstractions.

---

# Pattern vs Accident

Do not assume every existing line is a convention.

If five screens follow one pattern and one screen differs, the dominant pattern is probably the intended one.

Example:

```text
5 screens → collectAsStateWithLifecycle()
1 screen → collectAsState()
```

Prefer the lifecycle-aware pattern for new work unless the exception has a reason.

---

# Do Not Propagate Bugs

Respecting existing code does NOT mean copying obvious defects.

Examples:

```text
silent save
DAO used directly from UI
money formatted manually
missing widget refresh
swallowed exception
```

If a nearby implementation is clearly wrong, do not reproduce it.

Use the dominant correct architecture and fix the directly related issue when safe.

---

# Scope Control

Do not turn small tasks into project-wide refactors.

Example:

User asks:

```text
Show confirmation after saving closing day
```

Do NOT:

```text
replace navigation
migrate SharedPreferences
split all repositories
rewrite theme
convert routes to typed navigation
```

Make the smallest complete correct change.

---

# Boy Scout Rule

Small cleanup directly related to the modified code is acceptable when it:

* removes obvious duplication;
* fixes a small nearby bug;
* improves clarity;
* reduces risk.

Do not use this rule to justify unrelated refactoring.

---

# Testing Expectations

Business logic changes should receive unit tests where practical.

Priority areas:

```text
budget-cycle math
date calculations
financial calculations
validation
repository behavior where testable
migration behavior
```

Do not add meaningless tests purely for coverage.

---

# UX Verification

Before completing any feature, mentally run the user flow.

Example:

```text
Open Settings
    ↓
Change closing day from 25 to 28
    ↓
Tap Guardar
```

Now verify:

```text
Did Save respond?
Was the value validated?
Could it save twice?
Was it persisted?
Did the user see confirmation?
If I leave and return, is 28 shown?
What happens if persistence fails?
```

If any answer is unclear, the feature requires more work.

---

# Data Verification

When a setting or transaction is saved, verify persistence rather than only UI state.

For example:

```text
Change closing day
Save
Navigate away
Return
```

The persisted value must still be correct.

---

# Feature Checklist

Before considering a feature finished:

## Functional

* [ ] Main action works.
* [ ] Correct data is persisted.
* [ ] Existing related behavior remains intact.
* [ ] Dependent data updates correctly.

## UX

* [ ] User receives feedback after important actions.
* [ ] Success state is understandable.
* [ ] Failure state is understandable.
* [ ] No important action ends silently.
* [ ] Repeated taps cannot accidentally duplicate data.

## Validation

* [ ] Invalid inputs are rejected.
* [ ] Errors are visible in Spanish.
* [ ] Domain constraints are respected.

## Architecture

* [ ] No DAO access from UI.
* [ ] Repository boundaries remain intact.
* [ ] Domain remains Android-free.
* [ ] Existing mapper conventions are followed.
* [ ] No unnecessary abstraction was added.

## Room

* [ ] Entity changes include migration.
* [ ] Database version incremented if required.
* [ ] New migration registered.
* [ ] New schema JSON generated.
* [ ] Existing data preserved.

## Widgets

* [ ] Widget-visible data changes call `updateAllFinanceWidgets(context)`.
* [ ] Appearance changes refresh widgets.
* [ ] Widget repository access follows Hilt EntryPoint pattern.

## Visual

* [ ] Material3/theme conventions preserved.
* [ ] UI text is Spanish.
* [ ] DOP formatting uses MoneyFormatter.
* [ ] Dark mode remains correct.
* [ ] Existing components reused where appropriate.

## Verification

* [ ] Relevant unit tests pass.
* [ ] `assembleDebug` succeeds.
* [ ] No dead imports or temporary debug code remain.
* [ ] Diff is focused on the requested task.

---

# Final Review Rule

Before finishing any task, ask:

> **Would a normal user consider this feature finished?**

Not:

> Does the code compile?

Not:

> Did Room save the value?

But:

> **Can the user complete the task, understand what happened, recover from errors, and see the correct resulting state?**

---

# Final Principle

The application must remain:

```text
Simple
Fast
Offline
Predictable
Consistent
Safe with user data
Maintainable
```

The agent must preserve the existing architecture while continually learning from the real repository.

The governing rule is:

> **Inspect first, implement the smallest complete solution, preserve data and architecture, communicate every important result to the user, refresh every dependent part of the app, and verify the entire flow—not just the line of code that performs the action.**