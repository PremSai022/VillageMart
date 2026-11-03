package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.BuildConfig
import com.mart.villagemart.data.BusinessSettingsDto
import com.mart.villagemart.data.BusinessSettingsRequest
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

data class SettingsForm(
    val businessName: String = "",
    val address: String = "",
    val mobile: String = "",
    val email: String = "",
    val gstEnabled: Boolean = false,
    val gstin: String = "",
    val stateName: String = "",
    val invoiceFooter: String = "",
    val lowStockPushEnabled: Boolean = false
)

data class SettingsState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val form: SettingsForm = SettingsForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: VillageMartRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, loadError = null)
            try {
                val settings = repository.businessSettings()
                mutableState.value = mutableState.value.copy(loading = false, form = settings.toForm())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, loadError = error.userMessage(JSON))
            }
        }
    }

    fun update(transform: (SettingsForm) -> SettingsForm) {
        mutableState.value = mutableState.value.copy(form = transform(mutableState.value.form), message = null, error = null)
    }

    fun save() {
        val current = mutableState.value
        if (current.saving) return
        val form = current.form
        val errors = linkedMapOf<String, String>()
        if (form.businessName.isBlank()) errors["businessName"] = "Business name is required"
        if (form.mobile.isNotBlank() && !form.mobile.trim().matches(Regex("[0-9+]{10,15}"))) {
            errors["mobile"] = "Enter 10–15 digits"
        }
        if (form.email.isNotBlank() && !form.email.trim().matches(Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            errors["email"] = "Enter a valid email"
        }
        if (form.gstin.isNotBlank() && !form.gstin.trim().uppercase().matches(Regex("[0-9A-Z]{15}"))) {
            errors["gstin"] = "GSTIN must be 15 letters/digits"
        }
        mutableState.value = current.copy(fieldErrors = errors, message = null, error = null)
        if (errors.isNotEmpty()) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true)
            try {
                val saved = repository.updateBusinessSettings(
                    BusinessSettingsRequest(
                        businessName = form.businessName.trim(),
                        address = form.address.trim().ifBlank { null },
                        mobile = form.mobile.trim().ifBlank { null },
                        email = form.email.trim().ifBlank { null },
                        gstEnabled = form.gstEnabled,
                        gstin = form.gstin.trim().uppercase().ifBlank { null },
                        stateName = form.stateName.trim().ifBlank { null },
                        invoiceFooter = form.invoiceFooter.trim().ifBlank { null },
                        lowStockPushEnabled = form.lowStockPushEnabled
                    )
                )
                mutableState.value = mutableState.value.copy(saving = false, form = saved.toForm(), message = "Settings saved")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    private fun BusinessSettingsDto.toForm() = SettingsForm(
        businessName = businessName,
        address = address.orEmpty(),
        mobile = mobile.orEmpty(),
        email = email.orEmpty(),
        gstEnabled = gstEnabled,
        gstin = gstin.orEmpty(),
        stateName = stateName.orEmpty(),
        invoiceFooter = invoiceFooter.orEmpty(),
        lowStockPushEnabled = lowStockPushEnabled
    )

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

@Composable
fun SettingsScreen(
    ownerName: String,
    signingOut: Boolean,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Settings", "Business, account and app", onBack = onBack)
        SettingsCard("Business details") {
            when {
                state.loading -> LoadingState()
                state.loadError != null -> {
                    ErrorMessage(state.loadError)
                    RetryButton(viewModel::load)
                }
                else -> {
                    val form = state.form
                    val errors = state.fieldErrors
                    FormField(form.businessName, { v -> viewModel.update { it.copy(businessName = v) } }, "Business name")
                    SettingsFieldError(errors["businessName"])
                    FormField(form.address, { v -> viewModel.update { it.copy(address = v) } }, "Address", singleLine = false)
                    FormField(form.mobile, { v -> viewModel.update { it.copy(mobile = v) } }, "Mobile", numeric = true)
                    SettingsFieldError(errors["mobile"])
                    FormField(form.email, { v -> viewModel.update { it.copy(email = v) } }, "Email")
                    SettingsFieldError(errors["email"])
                    FormField(form.stateName, { v -> viewModel.update { it.copy(stateName = v) } }, "State")
                    SwitchRow("GST billing", form.gstEnabled) { v -> viewModel.update { it.copy(gstEnabled = v) } }
                    if (form.gstEnabled) {
                        FormField(form.gstin, { v -> viewModel.update { it.copy(gstin = v) } }, "GSTIN")
                        SettingsFieldError(errors["gstin"])
                    }
                    FormField(form.invoiceFooter, { v -> viewModel.update { it.copy(invoiceFooter = v) } }, "Invoice footer", singleLine = false)
                    SwitchRow("Low-stock push alerts", form.lowStockPushEnabled) { v ->
                        viewModel.update { it.copy(lowStockPushEnabled = v) }
                    }
                    ErrorMessage(state.error)
                    state.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Button(
                        onClick = viewModel::save,
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (state.saving) "Saving…" else "Save settings") }
                }
            }
        }
        SettingsCard("Account") {
            Text("Signed in as", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(ownerName.ifBlank { "Owner" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = { confirmLogout = true },
                enabled = !signingOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(if (signingOut) "Signing out…" else "Sign out") }
        }
        SettingsCard("App") {
            InfoRow("Version", BuildConfig.VERSION_NAME)
            InfoRow("Server", BuildConfig.DEFAULT_API_BASE_URL)
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sign out?") },
            text = { Text("Unsynced offline sales stay on this device and will sync after you sign in again.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsFieldError(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
