package com.angel.mony.data.local.database

import android.content.Context
import androidx.room.Room

fun createFinanceDatabase(context: Context): FinanceDatabase = configureFinanceDatabase(
    Room.databaseBuilder<FinanceDatabase>(
        context = context.applicationContext,
        name = FINANCE_DATABASE_NAME,
        factory = FinanceDatabaseConstructor::initialize,
    )
).build()
