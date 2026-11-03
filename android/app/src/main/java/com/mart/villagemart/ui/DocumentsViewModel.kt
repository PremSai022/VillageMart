package com.mart.villagemart.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.DocumentRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class DocumentState(
    val loading: Boolean = false,
    val uri: Uri? = null,
    val fileName: String? = null,
    val error: String? = null
)

@HiltViewModel
class DocumentsViewModel @Inject constructor(
    private val documents: DocumentRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(DocumentState())
    val state: StateFlow<DocumentState> = mutableState.asStateFlow()

    fun invoice(id: Long) {
        load("invoice-$id.pdf") { documents.invoice(id) }
    }

    fun report(name: String, format: String, period: String = "THIS_MONTH") {
        load("$name.$format") { documents.report(name, format, period) }
    }

    private fun load(name: String, operation: suspend () -> Uri) {
        viewModelScope.launch {
            mutableState.value = DocumentState(loading = true)
            try {
                mutableState.value = DocumentState(uri = operation(), fileName = name)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = DocumentState(error = error.userMessage(JSON))
            }
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
