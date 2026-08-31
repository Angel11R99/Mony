package com.angel.mony.data.repository

import com.angel.mony.domain.repository.ProductCatalogRepository
import com.angel.mony.domain.repository.ProductCatalogResult
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpenFoodFactsProductCatalogRepository @Inject constructor() : ProductCatalogRepository {

    override suspend fun lookup(barcode: String): ProductCatalogResult = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(barcode.trim(), "UTF-8")
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$encoded.json?fields=code,product_name,product_name_es,brands")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
            }
            try {
                val responseCode = conn.responseCode
                when (responseCode) {
                    HTTP_OK -> {
                        val body = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                        parseResponse(body)
                    }
                    HTTP_NOT_FOUND -> ProductCatalogResult.NotFound
                    HTTP_TOO_MANY_REQUESTS, in 500..599 -> ProductCatalogResult.Unavailable
                    else -> ProductCatalogResult.Unavailable
                }
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            when (it) {
                is java.net.SocketTimeoutException,
                is java.net.ConnectException,
                is java.io.IOException -> ProductCatalogResult.Unavailable
                else -> ProductCatalogResult.Unavailable
            }
        }
    }

    internal fun parseResponse(json: String): ProductCatalogResult {
        val obj = org.json.JSONObject(json)
        val status = obj.optInt("status", 0)
        if (status != 1) return ProductCatalogResult.NotFound

        val product = obj.optJSONObject("product") ?: return ProductCatalogResult.NotFound
        val name = product.optString("product_name_es", "")
            .ifBlank { product.optString("product_name", "") }
            .ifBlank { return ProductCatalogResult.NotFound }

        val brand = product.optString("brands", "").ifBlank { null }
        return ProductCatalogResult.Found(name = name, brand = brand, source = "Open Food Facts")
    }

    private companion object {
        const val CONNECT_TIMEOUT_MS = 3_000
        const val READ_TIMEOUT_MS = 5_000
        const val HTTP_OK = 200
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val USER_AGENT = "PersonalFinanceTracker/1.0 (github.com/PersonalFinanceTracker)"
    }
}
