package com.angel.mony.presentation.components

import androidx.compose.runtime.mutableStateMapOf

class FormState {
    private val _errors = mutableStateMapOf<String, String>()

    fun setError(field: String, message: String) {
        _errors[field] = message
    }

    fun clearError(field: String) {
        _errors.remove(field)
    }

    fun clearAll() {
        _errors.clear()
    }

    fun hasError(field: String) = field in _errors

    operator fun get(field: String) = _errors[field]

    fun isValid() = _errors.isEmpty()
}
