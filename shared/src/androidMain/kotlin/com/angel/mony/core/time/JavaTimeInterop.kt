package com.angel.mony.core.time

import java.time.Instant as JavaInstant
import java.time.LocalDate as JavaLocalDate
import java.time.LocalTime as JavaLocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlinx.datetime.Instant as KotlinInstant
import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.LocalTime as KotlinLocalTime

fun JavaLocalDate.toKotlinLocalDate(): KotlinLocalDate =
    KotlinLocalDate(year, monthValue, dayOfMonth)

fun KotlinLocalDate.toJavaLocalDate(): JavaLocalDate =
    JavaLocalDate.of(year, monthNumber, dayOfMonth)

fun JavaLocalTime.toKotlinLocalTime(): KotlinLocalTime =
    KotlinLocalTime(hour, minute, second, nano)

fun KotlinLocalTime.toJavaLocalTime(): JavaLocalTime =
    JavaLocalTime.of(hour, minute, second, nanosecond)

fun KotlinLocalDate.format(formatter: DateTimeFormatter): String =
    toJavaLocalDate().format(formatter)

fun KotlinLocalTime.format(formatter: DateTimeFormatter): String =
    toJavaLocalTime().format(formatter)

fun JavaInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochSeconds(epochSecond, nano)

fun KotlinInstant.toJavaInstant(): JavaInstant =
    JavaInstant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

fun KotlinInstant.atZone(zoneId: ZoneId): ZonedDateTime =
    toJavaInstant().atZone(zoneId)
