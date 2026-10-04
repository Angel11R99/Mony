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

        FinanceMigrationSteps.all.forEach { it.migrate(connection) }

        assertEquals(70, expectedStatements.size)
        assertEquals(expectedStatements, connection.executedStatements)
        assertFalse(expectedStatements.any { it.contains("DROP TABLE", ignoreCase = true) })
        assertFalse(expectedStatements.any { it.contains("DELETE FROM", ignoreCase = true) })
    }
}

private class RecordingConnection : SQLiteConnection {
    val executedStatements = mutableListOf<String>()

    override fun prepare(sql: String): SQLiteStatement {
        executedStatements += sql
        return RecordingStatement
    }

    override fun close() = Unit
}

private object RecordingStatement : SQLiteStatement {
    override fun bindBlob(index: Int, value: ByteArray) = unsupported()
    override fun bindDouble(index: Int, value: Double) = unsupported()
    override fun bindLong(index: Int, value: Long) = unsupported()
    override fun bindText(index: Int, value: String) = unsupported()
    override fun bindNull(index: Int) = unsupported()
    override fun getBlob(index: Int): ByteArray = unsupported()
    override fun getDouble(index: Int): Double = unsupported()
    override fun getLong(index: Int): Long = unsupported()
    override fun getText(index: Int): String = unsupported()
    override fun isNull(index: Int): Boolean = unsupported()
    override fun getColumnCount(): Int = 0
    override fun getColumnName(index: Int): String = unsupported()
    override fun getColumnType(index: Int): Int = unsupported()
    override fun step(): Boolean = false
    override fun reset() = Unit
    override fun clearBindings() = Unit
    override fun close() = Unit

    private fun unsupported(): Nothing = error("La migración solo debe ejecutar SQL sin parámetros")
}
