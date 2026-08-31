package com.angel.mony.domain.model

import java.time.Instant
import java.time.LocalDate

data class FixedEntry(
    val id: Long = 0,
    val type: TransactionType,
    val description: String,
    val amountInCents: Long,
    val categoryId: Long,
    val comment: String?,
    val isActive: Boolean = true,
    val manualDateMode: FixedDateMode = FixedDateMode.TODAY,
    val manualSpecificDate: LocalDate? = null,
    val scheduleMode: FixedScheduleMode = FixedScheduleMode.MANUAL,
    val scheduleHour: Int = 9,
    val scheduleSpecificDate: LocalDate? = null,
    val nextRunAt: Instant? = null,
    val lastAddedAt: Instant? = null,
    val lastAddedDate: LocalDate? = null,
)

enum class FixedDateMode {
    TODAY,
    PREVIOUS_FORTNIGHT,
    PREVIOUS_MONTH,
    SPECIFIC_DATE,
}

enum class FixedScheduleMode {
    MANUAL,
    AFTER_FORTNIGHT,
    AFTER_MONTH,
    SPECIFIC_DATE_TIME,
}
