package com.angel.mony.presentation.transactions

import com.angel.mony.domain.model.Category
import com.angel.mony.domain.model.TransactionType

enum class VoiceTypeBadgeState {
    UNIDENTIFIED,
    CONFIRMATION_REQUIRED,
    EXPENSE,
    INCOME,
}

fun voiceTypeBadgeState(type: TransactionType?, confirmationRequired: Boolean): VoiceTypeBadgeState = when {
    confirmationRequired -> VoiceTypeBadgeState.CONFIRMATION_REQUIRED
    type == TransactionType.EXPENSE -> VoiceTypeBadgeState.EXPENSE
    type == TransactionType.INCOME -> VoiceTypeBadgeState.INCOME
    else -> VoiceTypeBadgeState.UNIDENTIFIED
}

fun validCategoryIdForType(
    categoryId: Long?,
    type: TransactionType?,
    categories: List<Category>,
): Long? = categoryId?.takeIf { id ->
    type != null && categories.any { it.id == id && it.type == type && it.isActive }
}
