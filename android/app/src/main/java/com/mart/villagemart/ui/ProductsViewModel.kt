package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.ProductDto
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

data class ProductsState(
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val repository: VillageMartRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProductsState())
    val state: StateFlow<ProductsState> = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun load(query: String? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val products = repository.products(query?.trim()?.ifBlank { null }).content
                mutableState.value = mutableState.value.copy(loading = false, loaded = true, products = products)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, loaded = true, error = error.userMessage(JSON))
            }
        }
    }

    fun setActive(product: ProductDto, active: Boolean) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(error = null)
            try {
                repository.setProductStatus(product.id, active)
                load()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(error = error.userMessage(JSON))
            }
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
