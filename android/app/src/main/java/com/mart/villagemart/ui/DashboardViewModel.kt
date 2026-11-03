package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.DashboardSummaryDto
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class LoadState<T>(
    val loading: Boolean = false,
    val data: T? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: VillageMartRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoadState<DashboardSummaryDto>())
    val state: StateFlow<LoadState<DashboardSummaryDto>> = mutableState.asStateFlow()
    private val mutablePeriod = MutableStateFlow("TODAY")
    val period: StateFlow<String> = mutablePeriod.asStateFlow()
    private var loadJob: Job? = null

    fun refresh(period: String = mutablePeriod.value) {
        mutablePeriod.value = period
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                mutableState.value = LoadState(data = repository.dashboard(period))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, error = error.userMessage(JSON))
            }
        }
    }

    init {
        refresh()
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
