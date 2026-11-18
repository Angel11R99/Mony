package com.angel.mony.presentation.transactions

import com.angel.mony.core.MoneyFormatter
import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType
import com.angel.mony.domain.model.VoiceDraftField
import com.angel.mony.domain.model.VoiceTransactionCommand

/** Pure form-field mapping. No Room writes and no synthetic fields. */
internal data class VoiceFormFields(
    val type: TransactionType?,
    val amount: String,
    val categoryId: Long?,
    val date: String,
    val note: String,
    val fundingSource: String,
)

internal fun applyVoiceFields(
    old: VoiceFormFields,
    command: VoiceTransactionCommand.Apply,
    categories: List<Category>,
): VoiceFormFields {
    val mentioned = command.mentioned
    val inferred = command.draft
    val newType = if (VoiceDraftField.TYPE in mentioned) inferred.type else old.type
    val newCategory = when {
        VoiceDraftField.CATEGORY in mentioned ->
            validCategoryIdForType(inferred.categoryId, newType, categories)
        VoiceDraftField.TYPE in mentioned ->
            validCategoryIdForType(old.categoryId, newType, categories)
        else -> old.categoryId
    }
    return old.copy(
        type = newType,
        amount = if (VoiceDraftField.AMOUNT in mentioned)
            inferred.amountInCents?.let(MoneyFormatter::formatToInput).orEmpty()
            else old.amount,
        categoryId = newCategory,
        date = if (VoiceDraftField.DATE in mentioned) inferred.date?.toString().orEmpty() else old.date,
        note = if (VoiceDraftField.NOTE in mentioned) inferred.note.orEmpty() else old.note,
        fundingSource = if (VoiceDraftField.FUNDING_SOURCE in mentioned)
            inferred.fundingSource.orEmpty() else old.fundingSource,
    )
}
