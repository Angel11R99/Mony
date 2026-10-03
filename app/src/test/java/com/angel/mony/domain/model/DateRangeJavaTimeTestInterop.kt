package com.angel.mony.domain.model

import com.angel.mony.core.time.toKotlinLocalDate
import java.time.LocalDate

@Suppress("FunctionName")
fun DateRange(start: LocalDate, endInclusive: LocalDate): DateRange =
    DateRange(start.toKotlinLocalDate(), endInclusive.toKotlinLocalDate())
