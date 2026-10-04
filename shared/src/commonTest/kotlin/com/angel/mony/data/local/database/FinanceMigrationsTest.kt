package com.angel.mony.data.local.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class FinanceMigrationsTest {
    @Test
    fun migrationPathIsContiguousFromVersionOneToSeventeen() {
        val migrations = FinanceMigrationSteps.all

        assertEquals((1..16).toList(), migrations.map(FinanceMigrationStep::startVersion))
        assertEquals((2..17).toList(), migrations.map(FinanceMigrationStep::endVersion))
    }

    @Test
    fun completeMigrationPathExecutesEveryStatementInOrder() {
        val connection = RecordingConnection()
        val expectedStatements = FinanceMigrationSteps.all.flatMap(FinanceMigrationStep::statements)

        FinanceMigrationSteps.roomMigrations.forEach { it.migrate(connection) }

        assertEquals(70, expectedStatements.size)
        assertEquals(expectedStatements, connection.executedStatements)
        assertFalse(expectedStatements.any { it.contains("DROP TABLE", ignoreCase = true) })
        assertFalse(expectedStatements.any { it.contains("DELETE FROM", ignoreCase = true) })
    }

    @Test
    fun initialCategorySeedPreservesAllDefaultValues() {
        val connection = RecordingConnection()

        seedInitialCategories(connection, createdAtEpochMillis = 1234L)

        assertEquals(21, connection.boundRows.size)
        assertEquals(listOf("Salario", "INCOME", "payments", 1234L), connection.boundRows.first())
        assertEquals(listOf("Otros", "EXPENSE", "more_horiz", 1234L), connection.boundRows.last())
    }
}

private class RecordingConnection : SQLiteConnection {
    val executedStatements = mutableListOf<String>()
    val boundRows = mutableListOf<List<Any>>()

    override fun prepare(sql: String): SQLiteStatement {
        executedStatements += sql
        return RecordingStatement(boundRows)
    }

    override fun close() = Unit
}

private class RecordingStatement(
    private val boundRows: MutableList<List<Any>>,
) : SQLiteStatement {
    private val bindings = mutableMapOf<Int, Any>()

    override fun bindBlob(index: Int, value: ByteArray) = unsupported()
    override fun bindDouble(index: Int, value: Double) = unsupported()
    override fun bindLong(index: Int, value: Long) {
        bindings[index] = value
    }
    override fun bindText(index: Int, value: String) {
        bindings[index] = value
    }
    override fun bindNull(index: Int) = unsupported()
    override fun getBlob(index: Int): ByteArray = unsupported()
    override fun getDouble(index: Int): Double = unsupported()
    override fun getLong(index: Int): Long = unsupported()
    override fun getText(index: Int): String = unsupported()
    override fun isNull(index: Int): Boolean = unsupported()
    override fun getColumnCount(): Int = 0
    override fun getColumnName(index: Int): String = unsupported()
    override fun getColumnType(index: Int): Int = unsupported()
    override fun step(): Boolean {
        if (bindings.isNotEmpty()) {
            boundRows += bindings.toSortedMap().values.toList()
        }
        return false
    }
    override fun reset() = Unit
    override fun clearBindings() = bindings.clear()
    override fun close() = Unit

    private fun unsupported(): Nothing = error("La migración solo debe ejecutar SQL sin parámetros")
}
