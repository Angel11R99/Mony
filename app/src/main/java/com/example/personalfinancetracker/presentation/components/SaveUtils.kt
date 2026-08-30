package com.example.personalfinancetracker.presentation.components

import androidx.compose.runtime.saveable.Saver
import java.time.LocalDate

/**
 * Saver para [LocalDate] dentro de `rememberSaveable`, guardado como epoch day.
 */
val localDateSaver = Saver<LocalDate, Long>(
    save = { it.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)

/**
 * Saver para [LocalDate]? (nullable) dentro de `rememberSaveable`.
 * Guarda una lista vacía cuando no hay fecha y el epoch day en caso contrario.
 */
val localDateNullableSaver = Saver<LocalDate?, List<Long>>(
    save = { listOfNotNull(it?.toEpochDay()) },
    restore = { list -> list.firstOrNull()?.let(LocalDate::ofEpochDay) },
)