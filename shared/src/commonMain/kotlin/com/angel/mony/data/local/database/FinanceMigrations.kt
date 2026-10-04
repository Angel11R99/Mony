package com.angel.mony.data.local.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

data class FinanceMigrationStep(
    val startVersion: Int,
    val endVersion: Int,
    val statements: List<String>,
) {
    fun migrate(connection: SQLiteConnection) {
        statements.forEach(connection::execSQL)
    }
}

object FinanceMigrationSteps {
    val migration1To2 = step(
        1,
        "CREATE TABLE IF NOT EXISTS budget_config (id INTEGER NOT NULL, amountInCents INTEGER NOT NULL, period TEXT NOT NULL, PRIMARY KEY(id))",
    )

    val migration2To3 = step(
        2,
        "ALTER TABLE budget_config ADD COLUMN cycleStartEpochDay INTEGER",
        "ALTER TABLE budget_config ADD COLUMN cycleStartedAtEpochMillis INTEGER",
        """CREATE TABLE IF NOT EXISTS budget_cycle_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            period TEXT NOT NULL,
            budgetAmountInCents INTEGER NOT NULL,
            incomeInCents INTEGER NOT NULL,
            expenseInCents INTEGER NOT NULL,
            startDateEpochDay INTEGER NOT NULL,
            endDateEpochDay INTEGER NOT NULL,
            closedAtEpochMillis INTEGER NOT NULL
        )""".trimIndent(),
    )

    val migration3To4 = step(3, "ALTER TABLE budget_config ADD COLUMN incomeTransactionId INTEGER")

    val migration4To5 = step(
        4,
        """CREATE TABLE IF NOT EXISTS fixed_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            type TEXT NOT NULL,
            description TEXT NOT NULL,
            amountInCents INTEGER NOT NULL,
            categoryId INTEGER NOT NULL,
            comment TEXT,
            isActive INTEGER NOT NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_fixed_entries_categoryId ON fixed_entries(categoryId)",
        "CREATE INDEX IF NOT EXISTS index_fixed_entries_type ON fixed_entries(type)",
    )

    val migration5To6 = step(
        5,
        "ALTER TABLE fixed_entries ADD COLUMN manualDateMode TEXT NOT NULL DEFAULT 'TODAY'",
        "ALTER TABLE fixed_entries ADD COLUMN manualSpecificDateEpochDay INTEGER",
        "ALTER TABLE fixed_entries ADD COLUMN scheduleMode TEXT NOT NULL DEFAULT 'MANUAL'",
        "ALTER TABLE fixed_entries ADD COLUMN scheduleHour INTEGER NOT NULL DEFAULT 9",
        "ALTER TABLE fixed_entries ADD COLUMN scheduleSpecificDateEpochDay INTEGER",
        "ALTER TABLE fixed_entries ADD COLUMN nextRunAtEpochMillis INTEGER",
        "ALTER TABLE fixed_entries ADD COLUMN lastAddedAtEpochMillis INTEGER",
        "ALTER TABLE fixed_entries ADD COLUMN lastAddedDateEpochDay INTEGER",
    )

    val migration6To7 = step(
        6,
        "ALTER TABLE transactions ADD COLUMN fixedEntryId INTEGER",
        "CREATE INDEX IF NOT EXISTS index_transactions_fixedEntryId ON transactions(fixedEntryId)",
    )

    val migration7To8 = step(
        7,
        "ALTER TABLE budget_config ADD COLUMN closingDays TEXT NOT NULL DEFAULT '15'",
    )

    val migration8To9 = step(
        8,
        """CREATE TABLE IF NOT EXISTS pending_entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            type TEXT NOT NULL,
            description TEXT NOT NULL,
            amountInCents INTEGER NOT NULL,
            categoryId INTEGER NOT NULL,
            dateEpochDay INTEGER NOT NULL,
            comment TEXT,
            isDone INTEGER NOT NULL,
            doneAtEpochMillis INTEGER,
            transactionId INTEGER,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_pending_entries_categoryId ON pending_entries(categoryId)",
        "CREATE INDEX IF NOT EXISTS index_pending_entries_type ON pending_entries(type)",
        "CREATE INDEX IF NOT EXISTS index_pending_entries_dateEpochDay ON pending_entries(dateEpochDay)",
        "CREATE INDEX IF NOT EXISTS index_pending_entries_isDone ON pending_entries(isDone)",
    )

    val migration9To10 = step(
        9,
        "ALTER TABLE pending_entries ADD COLUMN reminderMinutesOfDay INTEGER",
    )

    val migration10To11 = step(
        10,
        "ALTER TABLE categories ADD COLUMN budgetLimitInCents INTEGER DEFAULT NULL",
    )

    val migration11To12 = step(
        11,
        """CREATE TABLE IF NOT EXISTS savings_goals (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            targetAmountInCents INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL
        )""".trimIndent(),
        "ALTER TABLE transactions ADD COLUMN savingsGoalId INTEGER",
        "CREATE INDEX IF NOT EXISTS index_transactions_savingsGoalId ON transactions(savingsGoalId)",
    )

    val migration12To13 = step(
        12,
        "ALTER TABLE savings_goals ADD COLUMN completedAtEpochMillis INTEGER DEFAULT NULL",
    )

    val migration13To14 = step(
        13,
        """CREATE TABLE IF NOT EXISTS shopping_lists (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            status TEXT NOT NULL,
            budgetInCents INTEGER,
            expenseTransactionId INTEGER,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            completedAtEpochMillis INTEGER,
            FOREIGN KEY(expenseTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )""".trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_lists_expenseTransactionId ON shopping_lists(expenseTransactionId)",
        """CREATE TABLE IF NOT EXISTS shopping_list_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            shoppingListId INTEGER NOT NULL,
            name TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            estimatedUnitPriceInCents INTEGER,
            actualUnitPriceInCents INTEGER,
            barcode TEXT,
            isPurchased INTEGER NOT NULL,
            isIdentified INTEGER NOT NULL,
            notes TEXT,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_shopping_list_items_shoppingListId ON shopping_list_items(shoppingListId)",
        "CREATE INDEX IF NOT EXISTS index_shopping_list_items_barcode ON shopping_list_items(barcode)",
        """CREATE TABLE IF NOT EXISTS shopping_adjustments (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            shoppingListId INTEGER NOT NULL,
            name TEXT NOT NULL,
            isPositive INTEGER NOT NULL,
            amountInCents INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(shoppingListId) REFERENCES shopping_lists(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_shopping_adjustments_shoppingListId ON shopping_adjustments(shoppingListId)",
        """CREATE TABLE IF NOT EXISTS known_products (
            barcode TEXT NOT NULL,
            name TEXT NOT NULL,
            lastPriceInCents INTEGER,
            lastUsedAtEpochMillis INTEGER NOT NULL,
            PRIMARY KEY(barcode)
        )""".trimIndent(),
    )

    val migration14To15 = step(
        14,
        "ALTER TABLE shopping_lists ADD COLUMN payableId INTEGER",
        "ALTER TABLE shopping_lists ADD COLUMN purchaseDateEpochDay INTEGER",
        "ALTER TABLE shopping_lists ADD COLUMN paymentMethod TEXT",
        "ALTER TABLE shopping_lists ADD COLUMN expenseCategoryId INTEGER",
        "ALTER TABLE pending_entries ADD COLUMN sourceShoppingListId INTEGER",
        "UPDATE shopping_lists SET paymentMethod = 'DEBIT' WHERE expenseTransactionId IS NOT NULL",
        "UPDATE shopping_lists SET purchaseDateEpochDay = (SELECT dateEpochDay FROM transactions WHERE transactions.id = shopping_lists.expenseTransactionId) WHERE expenseTransactionId IS NOT NULL",
        "UPDATE shopping_lists SET expenseCategoryId = (SELECT categoryId FROM transactions WHERE transactions.id = shopping_lists.expenseTransactionId) WHERE expenseTransactionId IS NOT NULL",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_shopping_lists_payableId ON shopping_lists(payableId)",
        "CREATE INDEX IF NOT EXISTS index_shopping_lists_expenseCategoryId ON shopping_lists(expenseCategoryId)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_pending_entries_sourceShoppingListId ON pending_entries(sourceShoppingListId)",
        """CREATE TABLE IF NOT EXISTS product_recognition_aliases (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            detectedText TEXT NOT NULL,
            normalizedAlias TEXT NOT NULL,
            displayName TEXT NOT NULL,
            barcode TEXT,
            confirmationCount INTEGER NOT NULL,
            lastUsedAtEpochMillis INTEGER NOT NULL
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_product_recognition_aliases_normalizedAlias ON product_recognition_aliases(normalizedAlias)",
        "CREATE INDEX IF NOT EXISTS index_product_recognition_aliases_barcode ON product_recognition_aliases(barcode)",
    )

    val migration15To16 = step(
        15,
        """CREATE TABLE IF NOT EXISTS fortnight_templates (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            description TEXT NOT NULL,
            firstFortnightAmountInCents INTEGER,
            secondFortnightAmountInCents INTEGER,
            categoryId INTEGER NOT NULL,
            type TEXT NOT NULL,
            note TEXT,
            isActive INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_fortnight_templates_categoryId ON fortnight_templates(categoryId)",
        "CREATE INDEX IF NOT EXISTS index_fortnight_templates_isActive ON fortnight_templates(isActive)",
        """CREATE TABLE IF NOT EXISTS fortnight_plans (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            startDateEpochDay INTEGER NOT NULL,
            endDateEpochDay INTEGER NOT NULL,
            slot TEXT NOT NULL,
            budgetInCents INTEGER NOT NULL,
            status TEXT NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            closedAtEpochMillis INTEGER
        )""".trimIndent(),
        "CREATE UNIQUE INDEX IF NOT EXISTS index_fortnight_plans_startDateEpochDay_endDateEpochDay ON fortnight_plans(startDateEpochDay, endDateEpochDay)",
        "CREATE INDEX IF NOT EXISTS index_fortnight_plans_status ON fortnight_plans(status)",
        """CREATE TABLE IF NOT EXISTS fortnight_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            planId INTEGER NOT NULL,
            templateId INTEGER,
            description TEXT NOT NULL,
            plannedAmountInCents INTEGER NOT NULL,
            categoryId INTEGER NOT NULL,
            type TEXT NOT NULL,
            savingsGoalId INTEGER,
            note TEXT,
            position INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(planId) REFERENCES fortnight_plans(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(templateId) REFERENCES fortnight_templates(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(savingsGoalId) REFERENCES savings_goals(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_fortnight_items_planId ON fortnight_items(planId)",
        "CREATE INDEX IF NOT EXISTS index_fortnight_items_templateId ON fortnight_items(templateId)",
        "CREATE INDEX IF NOT EXISTS index_fortnight_items_categoryId ON fortnight_items(categoryId)",
        "CREATE INDEX IF NOT EXISTS index_fortnight_items_savingsGoalId ON fortnight_items(savingsGoalId)",
        """CREATE TABLE IF NOT EXISTS fortnight_payments (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            itemId INTEGER NOT NULL,
            amountInCents INTEGER NOT NULL,
            dateEpochDay INTEGER NOT NULL,
            transactionId INTEGER,
            createdAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(itemId) REFERENCES fortnight_items(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_fortnight_payments_itemId ON fortnight_payments(itemId)",
        "CREATE UNIQUE INDEX IF NOT EXISTS index_fortnight_payments_transactionId ON fortnight_payments(transactionId)",
    )

    val migration16To17 = step(
        16,
        """CREATE TABLE IF NOT EXISTS expense_funding (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            transactionId INTEGER NOT NULL,
            amountInCents INTEGER NOT NULL,
            sourceDescription TEXT NOT NULL,
            dateEpochDay INTEGER NOT NULL,
            createdAtEpochMillis INTEGER NOT NULL,
            updatedAtEpochMillis INTEGER NOT NULL,
            FOREIGN KEY(transactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )""".trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_expense_funding_transactionId ON expense_funding(transactionId)",
        "CREATE INDEX IF NOT EXISTS index_expense_funding_dateEpochDay ON expense_funding(dateEpochDay)",
        "CREATE INDEX IF NOT EXISTS index_expense_funding_createdAtEpochMillis ON expense_funding(createdAtEpochMillis)",
    )

    val all = listOf(
        migration1To2,
        migration2To3,
        migration3To4,
        migration4To5,
        migration5To6,
        migration6To7,
        migration7To8,
        migration8To9,
        migration9To10,
        migration10To11,
        migration11To12,
        migration12To13,
        migration13To14,
        migration14To15,
        migration15To16,
        migration16To17,
    )

    private fun step(startVersion: Int, vararg statements: String) = FinanceMigrationStep(
        startVersion = startVersion,
        endVersion = startVersion + 1,
        statements = statements.toList(),
    )
}
