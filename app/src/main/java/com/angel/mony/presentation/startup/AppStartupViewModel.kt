package com.angel.mony.presentation.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.angel.mony.core.FinanceDataCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppStartupState {
    data object Loading : AppStartupState
    data object Ready : AppStartupState
    data class Error(val message: String) : AppStartupState
}

@HiltViewModel
class AppStartupViewModel @Inject constructor(
    private val dataCache: FinanceDataCache,
) : ViewModel() {
    private val _state = MutableStateFlow<AppStartupState>(AppStartupState.Loading)
    val state: StateFlow<AppStartupState> = _state.asStateFlow()

    init {
        prepare()
    }

    fun retry() {
        if (_state.value == AppStartupState.Loading) return
        dataCache.retryFailedLoads()
        prepare()
    }

    private fun prepare() {
        _state.value = AppStartupState.Loading
        viewModelScope.launch {
            runCatching { preloadLocalData() }
                .onSuccess { _state.value = AppStartupState.Ready }
                .onFailure {
                    _state.value = AppStartupState.Error(
                        "No pudimos preparar tus datos. Revisa el almacenamiento e inténtalo de nuevo.",
                    )
                }
        }
    }

    private suspend fun preloadLocalData() = coroutineScope {
        listOf(
            async { dataCache.awaitInitialLoad() },
            async { delay(650L) },
        ).awaitAll()
    }
}
