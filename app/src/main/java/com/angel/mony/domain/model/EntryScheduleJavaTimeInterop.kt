@file:JvmName("EntryScheduleJavaTimeInterop")

package com.angel.mony.domain.model

import com.angel.mony.core.time.toJavaInstant
import com.angel.mony.core.time.toJavaLocalDate
import com.angel.mony.core.time.toKotlinInstant
import com.angel.mony.core.time.toKotlinLocalDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.datetime.LocalTime as KotlinLocalTime
import kotlinx.datetime.TimeZone

fun FixedEntry.manualPostingDate(today: LocalDate): LocalDate =
    manualPostingDate(today.toKotlinLocalDate()).toJavaLocalDate()

fun previousFortnightEnd(today: LocalDate): LocalDate =
    previousFortnightEnd(today.toKotlinLocalDate()).toJavaLocalDate()

fun calculateNextRun(
    mode: FixedScheduleMode,
    hour: Int,
    specificDate: LocalDate?,
    after: Instant,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Instant? = calculateNextRun(
    mode = mode,
    hour = hour,
    specificDate = specificDate?.toKotlinLocalDate(),
    after = after.toKotlinInstant(),
    timeZone = TimeZone.of(zoneId.id),
)?.toJavaInstant()

fun pendingReminderInstant(
    date: LocalDate,
    time: LocalTime,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Instant = pendingReminderInstant(
    date = date.toKotlinLocalDate(),
    time = KotlinLocalTime(time.hour, time.minute, time.second, time.nano),
    timeZone = TimeZone.of(zoneId.id),
).toJavaInstant()

fun isPendingReminderInFuture(
    date: LocalDate,
    time: LocalTime,
    now: Instant,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Boolean = isPendingReminderInFuture(
    date = date.toKotlinLocalDate(),
    time = KotlinLocalTime(time.hour, time.minute, time.second, time.nano),
    now = now.toKotlinInstant(),
    timeZone = TimeZone.of(zoneId.id),
)

fun isPendingDateValid(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean =
    isPendingDateValid(date.toKotlinLocalDate(), today.toKotlinLocalDate())
