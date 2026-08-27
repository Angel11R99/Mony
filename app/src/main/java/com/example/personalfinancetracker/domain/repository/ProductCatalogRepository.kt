package com.example.personalfinancetracker.domain.repository

sealed interface ProductCatalogResult {
    data class Found(
        val name: String,
        val brand: String?,
        val source: String,
    ) : ProductCatalogResult
    data object NotFound : ProductCatalogResult
    data object Unavailable : ProductCatalogResult
}

interface ProductCatalogRepository {
    suspend fun lookup(barcode: String): ProductCatalogResult
}
