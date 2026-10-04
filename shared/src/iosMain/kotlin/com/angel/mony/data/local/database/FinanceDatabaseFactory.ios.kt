package com.angel.mony.data.local.database

import androidx.room.Room

fun createFinanceDatabase(databasePath: String): FinanceDatabase = configureFinanceDatabase(
    Room.databaseBuilder<FinanceDatabase>(
        name = databasePath,
        factory = FinanceDatabaseConstructor::initialize,
    )
).build()
