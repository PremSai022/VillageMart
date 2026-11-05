package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.ApiError
import com.mart.villagemart.data.AuthRepository
import com.mart.villagemart.data.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

data class SessionState(
    val checking: Boolean = true,
    val signedIn: Boolean = false,
    val ownerName: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    /** null = unknown (status could not be loaded); the backend always enforces the rule. */
    val registrationOpen: Boolean? = null,
    val fieldErrors: Map<RegisterField, String> = emptyMap()
)

enum class RegisterField(val apiName: String) {
    FULL_NAME("fullName"),
    MOBILE("mobileNumber"),
    EMAIL("email"),
    PASSWORD("password"),
    CONFIRM_PASSWORD("confirmPassword"),
    BUSINESS_NAME("businessName"),
    BUSINESS_ADDRESS("businessAddress"),
    TERMS("acceptTerms")
}

data class RegisterForm(
    val fullName: String = "",
    val mobile: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val businessName: String = "",
    val businessAddress: String = "",
    val acceptTerms: Boolean = false
)

private val MOBILE_REGEX = Regex("^[6-9]\\d{9}$")
private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
private val PASSWORD_REGEX = Regex("^(?=.*[A-Za-z])(?=.*\\d).{8,100}$")

/** Client-side validation mirroring the backend rules; returns an empty map when the form is valid. */
fun validateRegistration(form: RegisterForm): Map<RegisterField, String> = buildMap {
    val name = form.fullName.trim()
    if (name.isEmpty()) put(RegisterField.FULL_NAME, "Enter your full name")
    else if (name.length < 2) put(RegisterField.FULL_NAME, "Name must be at least 2 characters")
    val mobile = form.mobile.trim()
    if (mobile.isEmpty()) put(RegisterField.MOBILE, "Enter your mobile number")
    else if (!MOBILE_REGEX.matches(mobile)) put(RegisterField.MOBILE, "Enter a valid 10-digit mobile number")
    val email = form.email.trim()
    if (email.isNotEmpty() && (email.length > 120 || !EMAIL_REGEX.matches(email))) {
        put(RegisterField.EMAIL, "Enter a valid email address")
    }
    if (form.password.isEmpty()) put(RegisterField.PASSWORD, "Create a password")
    else if (!PASSWORD_REGEX.matches(form.password)) {
        put(RegisterField.PASSWORD, "Use at least 8 characters with a letter and a number")
    }
    if (form.confirmPassword.isEmpty()) put(RegisterField.CONFIRM_PASSWORD, "Confirm your password")
    else if (form.confirmPassword != form.password) put(RegisterField.CONFIRM_PASSWORD, "Passwords do not match")
    val business = form.businessName.trim()
    if (business.isEmpty()) put(RegisterField.BUSINESS_NAME, "Enter your business name")
    else if (business.length > 150) put(RegisterField.BUSINESS_NAME, "Business name is too long")
    if (form.businessAddress.trim().length > 300) put(RegisterField.BUSINESS_ADDRESS, "Address is too long")
    if (!form.acceptTerms) put(RegisterField.TERMS, "Please accept the terms and conditions")
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = mutableState.asStateFlow()

    init {
        checkSession()
    }

    fun signIn(login: String, password: String) {
        if (login.isBlank() || password.isBlank()) {
            mutableState.update { it.copy(error = "Enter your mobile number, email or username and password") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try {
                val response = repository.login(login, password)
                mutableState.value = SessionState(checking = false, signedIn = true, ownerName = response.user.fullName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val failure = describe(error)
                mutableState.update { it.copy(checking = false, busy = false, error = failure.message) }
            }
        }
    }

    fun register(form: RegisterForm) {
        val errors = validateRegistration(form)
        if (errors.isNotEmpty()) {
            mutableState.update { it.copy(fieldErrors = errors, error = "Please correct the highlighted fields") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null, fieldErrors = emptyMap()) }
            try {
                val response = repository.register(
                    RegisterRequest(
                        fullName = form.fullName.trim(),
                        mobileNumber = form.mobile.trim(),
                        email = form.email.trim().ifEmpty { null },
                        password = form.password,
                        businessName = form.businessName.trim(),
                        businessAddress = form.businessAddress.trim().ifEmpty { null },
                        acceptTerms = form.acceptTerms
                    )
                )
                mutableState.value = SessionState(checking = false, signedIn = true, ownerName = response.user.fullName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val failure = describe(error)
                mutableState.update {
                    it.copy(
                        busy = false,
                        error = failure.message,
                        fieldErrors = failure.fieldErrors,
                        registrationOpen = if (failure.code == "REGISTRATION_CLOSED") false else it.registrationOpen
                    )
                }
            }
        }
    }

    fun loadRegistrationStatus() {
        viewModelScope.launch {
            val open = try {
                repository.registrationOpen()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                null
            }
            mutableState.update { it.copy(registrationOpen = open) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, error = null) }
            try {
                repository.logout()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                repository.clearSession()
            } finally {
                mutableState.value = SessionState(checking = false)
            }
        }
    }

    fun clearError() {
        mutableState.update { it.copy(error = null) }
    }

    fun clearFieldError(field: RegisterField) {
        mutableState.update { it.copy(fieldErrors = it.fieldErrors - field, error = null) }
    }

    fun resetMessages() {
        mutableState.update { it.copy(error = null, fieldErrors = emptyMap()) }
    }

    private fun checkSession() {
        viewModelScope.launch {
            if (!repository.hasSession()) {
                mutableState.value = SessionState(checking = false)
                return@launch
            }
            try {
                val user = repository.me()
                mutableState.value = SessionState(checking = false, signedIn = true, ownerName = user.fullName)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (error is HttpException && error.code() == 401) repository.clearSession()
                // Offline: keep the stored session so the owner can keep working with cached/queued data.
                mutableState.value = SessionState(checking = false, signedIn = repository.hasSession())
            }
        }
    }

    private data class Failure(
        val message: String,
        val code: String? = null,
        val fieldErrors: Map<RegisterField, String> = emptyMap()
    )

    private fun describe(error: Exception): Failure = when (error) {
        is IOException -> Failure("Cannot reach the VillageMart server. Check your internet connection and try again.")
        is HttpException -> {
            val api = parseApiError(error)
            val fields = api?.fieldErrors.orEmpty().mapNotNull { violation ->
                RegisterField.entries.firstOrNull { it.apiName == violation.field }?.let { it to violation.message }
            }.toMap()
            val message = when {
                error.code() >= 500 -> "The VillageMart server is temporarily unavailable. Please try again shortly."
                api?.code == "INVALID_CREDENTIALS" -> "Incorrect mobile number/username or password."
                api?.code == "ACCOUNT_DISABLED" -> "This account is disabled."
                api?.code == "REGISTRATION_CLOSED" ->
                    "An owner account already exists for this business. Please sign in instead."
                api?.code == "MOBILE_ALREADY_REGISTERED" -> "This mobile number is already registered. Please sign in."
                api?.code == "EMAIL_ALREADY_REGISTERED" -> "This email address is already registered."
                api?.code == "VALIDATION_FAILED" -> "Please correct the highlighted fields."
                api != null -> api.message
                error.code() == 401 -> "Incorrect mobile number/username or password."
                else -> "Request failed (HTTP ${error.code()}). Please try again."
            }
            Failure(message, api?.code, fields)
        }
        else -> Failure(error.message?.takeIf(String::isNotBlank) ?: "Something went wrong. Please try again.")
    }

    private fun parseApiError(error: HttpException): ApiError? {
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull() ?: return null
        return try {
            errorJson.decodeFromString(ApiError.serializer(), body)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        private val errorJson = Json { ignoreUnknownKeys = true }
    }
}
