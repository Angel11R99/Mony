package com.example.personalfinancetracker.domain.repository

import com.example.personalfinancetracker.domain.model.Category
import com.example.personalfinancetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeActive(type: TransactionType): Flow<List<Category>>
    fun observeAll(): Flow<List<Category>>
}
