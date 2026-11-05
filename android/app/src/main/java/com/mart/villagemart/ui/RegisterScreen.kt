package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    state: SessionState,
    onLoadStatus: () -> Unit,
    onRegister: (RegisterForm) -> Unit,
    onClearFieldError: (RegisterField) -> Unit,
    onBackToLogin: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadStatus() }
    var fullName by rememberSaveable { mutableStateOf("") }
    var mobile by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var businessName by rememberSaveable { mutableStateOf("") }
    var businessAddress by rememberSaveable { mutableStateOf("") }
    var acceptTerms by rememberSaveable { mutableStateOf(false) }
    var showTerms by rememberSaveable { mutableStateOf(false) }
    val errors = state.fieldErrors
    val closed = state.registrationOpen == false

    fun submit() = onRegister(
        RegisterForm(fullName, mobile, email, password, confirmPassword, businessName, businessAddress, acceptTerms)
    )

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader("Create your owner account", "Set up VillageMart for your business in a minute.", logoSize = 72.dp)
            Card(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-32).dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (closed) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                "An owner account already exists for this business. New registrations are disabled — " +
                                    "please sign in with the owner account.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    SectionLabel("Owner details")
                    RegisterTextField(fullName, { fullName = it; onClearFieldError(RegisterField.FULL_NAME) },
                        "Full name", errors[RegisterField.FULL_NAME], capitalization = KeyboardCapitalization.Words)
                    RegisterTextField(
                        mobile,
                        { value -> mobile = value.filter(Char::isDigit).take(10); onClearFieldError(RegisterField.MOBILE) },
                        "Mobile number", errors[RegisterField.MOBILE], keyboardType = KeyboardType.Phone
                    )
                    RegisterTextField(email, { email = it.trim(); onClearFieldError(RegisterField.EMAIL) },
                        "Email address (optional)", errors[RegisterField.EMAIL], keyboardType = KeyboardType.Email)
                    PasswordField(
                        password, { password = it; onClearFieldError(RegisterField.PASSWORD) }, "Password",
                        error = errors[RegisterField.PASSWORD]
                    )
                    if (errors[RegisterField.PASSWORD] == null) {
                        Text(
                            "At least 8 characters, including a letter and a number.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    PasswordField(
                        confirmPassword, { confirmPassword = it; onClearFieldError(RegisterField.CONFIRM_PASSWORD) },
                        "Confirm password", error = errors[RegisterField.CONFIRM_PASSWORD]
                    )
                    Spacer(Modifier.height(8.dp))
                    SectionLabel("Business details")
                    RegisterTextField(businessName, { businessName = it; onClearFieldError(RegisterField.BUSINESS_NAME) },
                        "Business name", errors[RegisterField.BUSINESS_NAME], capitalization = KeyboardCapitalization.Words)
                    RegisterTextField(
                        businessAddress, { businessAddress = it; onClearFieldError(RegisterField.BUSINESS_ADDRESS) },
                        "Business address", errors[RegisterField.BUSINESS_ADDRESS],
                        capitalization = KeyboardCapitalization.Words, singleLine = false, imeAction = ImeAction.Done
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = acceptTerms,
                            onCheckedChange = { acceptTerms = it; onClearFieldError(RegisterField.TERMS) }
                        )
                        Text("I accept the", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { showTerms = true }) { Text("terms and conditions") }
                    }
                    errors[RegisterField.TERMS]?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 12.dp))
                    }
                    ErrorMessage(state.error)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { submit() },
                        enabled = !state.busy && !closed,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Text("Create Account", fontWeight = FontWeight.SemiBold)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onBackToLogin, enabled = !state.busy) { Text("Login") }
                    }
                }
            }
        }
    }
    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            confirmButton = {
                TextButton(onClick = { acceptTerms = true; onClearFieldError(RegisterField.TERMS); showTerms = false }) {
                    Text("Accept")
                }
            },
            dismissButton = { TextButton(onClick = { showTerms = false }) { Text("Close") } },
            title = { Text("Terms and conditions") },
            text = {
                Text(
                    "• This account is the single owner account for your VillageMart business and gives full access " +
                        "to its sales, stock, customers and reports.\n" +
                        "• Keep your password private. You are responsible for activity performed with your account.\n" +
                        "• Business data is stored on your VillageMart server and is used only to run your shop.\n" +
                        "• Only one owner account can be created; further registrations are blocked by the server."
                )
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun RegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
}
