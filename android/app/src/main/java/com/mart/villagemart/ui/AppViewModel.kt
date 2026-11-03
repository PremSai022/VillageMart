package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.TokenStore
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionState(
    val checking: Boolean = true,
    val signedIn: Boolean = false,
    val ownerName: String = "",
    val busy: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: VillageMartRepository,
    private val tokenStore: TokenStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        checkSession()
    }

    fun signIn(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            mutableState.value = mutableState.value.copy(error = "Enter your username or mobile number and password")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            try {
                val response = repository.login(login, password)
                mutableState.value = SessionState(checking = false, signedIn = true, ownerName = response.user.fullName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(checking = false, busy = false, error = error.userMessage(errorJson))
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(busy = true, error = null)
            try {
                repository.logout()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                tokenStore.clear()
            } finally {
                mutableState.value = SessionState(checking = false)
            }
        }
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private fun checkSession() {
        viewModelScope.launch {
            val accessToken = tokenStore.accessToken()
            if (accessToken == null) {
                mutableState.value = SessionState(checking = false)
                return@launch
            }
            try {
                val user = repository.me()
                mutableState.value = SessionState(checking = false, signedIn = true, ownerName = user.fullName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error is retrofit2.HttpException && error.code() == 401) tokenStore.clear()
                mutableState.value = SessionState(checking = false, signedIn = tokenStore.accessToken() != null)
            }
        }
    }

    companion object {
        private val errorJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    }
}
