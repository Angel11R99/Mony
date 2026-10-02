package com.angel.mony.core.time

import java.time.Instant as JavaInstant
import java.time.LocalDate as JavaLocalDate
import kotlinx.datetime.Instant as KotlinInstant
import kotlinx.datetime.LocalDate as KotlinLocalDate

fun JavaLocalDate.toKotlinLocalDate(): KotlinLocalDate =
    KotlinLocalDate(year, monthValue, dayOfMonth)

fun KotlinLocalDate.toJavaLocalDate(): JavaLocalDate =
    JavaLocalDate.of(year, monthNumber, dayOfMonth)

fun JavaInstant.toKotlinInstant(): KotlinInstant =
    KotlinInstant.fromEpochSeconds(epochSecond, nano)

fun KotlinInstant.toJavaInstant(): JavaInstant =
    JavaInstant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())
