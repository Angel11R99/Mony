package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.FixedEntry
import kotlinx.coroutines.flow.Flow

interface FixedEntryRepository {
    fun observeAll(): Flow<List<FixedEntry>>
    suspend fun save(entry: FixedEntry): Long
    suspend fun delete(id: Long)
}
