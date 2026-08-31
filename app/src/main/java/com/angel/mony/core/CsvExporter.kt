package com.angel.mony.core

import com.angel.mony.domain.model.BackupMovement
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.FinanceTransaction
import com.angel.mony.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object CsvExporter {
    const val UTF8_BOM = "\uFEFF"
    val header = listOf("Fecha", "Tipo", "Categoría", "Monto (RD$)", "Descripción")
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val fallbackDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun buildCsv(
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
    ): String {
        val rows = transactions.map { transaction ->
            listOf(
                dateFormatter.format(transaction.date),
                transaction.type.csvLabel,
                categories[transaction.categoryId]?.name ?: "Sin categoría",
                BigDecimal.valueOf(transaction.amountInCents, 2).toPlainString(),
                transaction.description.orEmpty(),
            )
        }
        val body = (listOf(header) + rows).joinToString(separator = "\r\n") { row ->
            row.joinToString(separator = ",") { escape(it) }
        }
        return UTF8_BOM + body
    }

    /**
     * Interpreta el contenido de un archivo exportado por la aplicación.
     * Debe recibirse el texto ya decodificado en UTF-8 y sin BOM.
     */
    fun parseBackup(content: String): List<BackupMovement> {
        val records = parseRecords(content)
        if (records.isEmpty()) error("El archivo está vacío")
        val fileHeader = records.first()
        val expectedHeader = header.map { it.normalizeForCompare() }
        if (fileHeader.map { it.normalizeForCompare() } != expectedHeader) {
            error("El archivo no corresponde a un respaldo exportado por la aplicación")
        }
        return records.drop(1).mapIndexedNotNull { index, record -> parseRow(index + 2, record) }
    }

    private fun parseRow(lineNumber: Int, record: List<String>): BackupMovement? {
        if (record.all { it.isBlank() }) return null
        if (record.size < header.size) error("La línea $lineNumber está incompleta")
        val date = parseDate(record[0].trim())
            ?: error("La línea $lineNumber tiene una fecha inválida: \"${record[0].trim()}\"")
        val type = TransactionType.entries.firstOrNull { it.csvLabel.equals(record[1].trim(), ignoreCase = true) }
            ?: error("La línea $lineNumber tiene un tipo inválido: \"${record[1].trim()}\". Usa ${TransactionType.EXPENSE.csvLabel} o ${TransactionType.INCOME.csvLabel}")
        val amountInCents = runCatching {
            BigDecimal(record[3].trim())
                .movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP)
        }.getOrNull()?.longValueExactOrNull()
            ?: error("La línea $lineNumber tiene un monto inválido: \"${record[3].trim()}\"")
        if (amountInCents <= 0) error("La línea $lineNumber tiene un monto menor o igual a cero")
        val categoryName = record[2].trim().ifEmpty { "Sin categoría" }
        val description = record[4].trim().ifEmpty { null }
        return BackupMovement(date, type, categoryName, amountInCents, description)
    }

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDate.parse(raw, dateFormatter)
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(raw, fallbackDateFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** Parser CSV con soporte de campos entrecomillados según RFC 4180. */
    private fun parseRecords(content: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        var field = StringBuilder()
        var record = mutableListOf<String>()
        var inQuotes = false
        var index = 0
        fun endField() {
            record.add(field.toString())
            field = StringBuilder()
        }
        fun endRecord() {
            endField()
            records.add(record)
            record = mutableListOf()
        }
        while (index < content.length) {
            val char = content[index]
            when {
                inQuotes -> when {
                    char == '"' -> {
                        if (index + 1 < content.length && content[index + 1] == '"') {
                            field.append('"')
                            index++
                        } else {
                            inQuotes = false
                        }
                    }
                    else -> field.append(char)
                }
                char == '"' -> inQuotes = true
                char == ',' -> endField()
                char == '\r' -> {
                    if (index + 1 < content.length && content[index + 1] == '\n') index++
                    endRecord()
                }
                char == '\n' -> endRecord()
                else -> field.append(char)
            }
            index++
        }
        if (field.isNotEmpty() || record.isNotEmpty()) endRecord()
        return records
    }

    private fun String.normalizeForCompare() = trim().removePrefix(UTF8_BOM).lowercase()

    private fun BigDecimal.longValueExactOrNull(): Long? = try {
        longValueExact()
    } catch (_: ArithmeticException) {
        null
    }

    private val TransactionType.csvLabel: String
        get() = if (this == TransactionType.EXPENSE) "Gasto" else "Ingreso"

    private fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
