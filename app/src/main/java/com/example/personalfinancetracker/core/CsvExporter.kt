package com.example.personalfinancetracker.core

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.FinanceTransaction
import com.example.personalfinancetracker.domain.model.TransactionType
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

object CsvExporter {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun buildCsv(
        transactions: List<FinanceTransaction>,
        categories: Map<Long, Category>,
    ): String {
        val header = listOf("Fecha", "Tipo", "Categoría", "Monto (RD$)", "Descripción")
        val rows = transactions.map { transaction ->
            listOf(
                dateFormatter.format(transaction.date),
                if (transaction.type == TransactionType.EXPENSE) "Gasto" else "Ingreso",
                categories[transaction.categoryId]?.name ?: "Sin categoría",
                BigDecimal.valueOf(transaction.amountInCents, 2).toPlainString(),
                transaction.description.orEmpty(),
            )
        }
        val body = (listOf(header) + rows).joinToString(separator = "\r\n") { row ->
            row.joinToString(separator = ",") { escape(it) }
        }
        return "\uFEFF$body"
    }

    private fun escape(value: String) = "\"${value.replace("\"", "\"\"")}\""
}
