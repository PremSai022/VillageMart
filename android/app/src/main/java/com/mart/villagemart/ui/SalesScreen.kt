package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal

@Composable
fun SalesScreen(onOpenHistory: () -> Unit = {}, viewModel: SalesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var confirmPriceOverride by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Sales and billing", "Build a bill and collect payment") {
            androidx.compose.material3.TextButton(onClick = onOpenHistory) {
                Icon(androidx.compose.material.icons.Icons.Filled.History, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("History")
            }
        }
        state.pendingSales.filter { it.status != "FAILED" }.take(3).forEach { pending ->
            DataRow(
                title = pending.invoiceNumber ?: "Sale pending sync",
                subtitle = pending.lastError ?: if (pending.status == "CONFIRMED") "Server confirmed" else "Waiting for a connection",
                status = if (pending.status == "CONFIRMED") "CONFIRMED" else pending.status
            )
        }
        state.pendingSales.filter { it.status == "FAILED" }.take(2).forEach { pending ->
            ErrorMessage("Sale draft needs attention: ${pending.lastError ?: "submission failed"}")
        }
        OutlinedTextField(
            query,
            onValueChange = { query = it; viewModel.load(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text("Search products") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        )
        ErrorMessage(state.error)
        if (!state.resultMessage.isNullOrBlank()) {
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(state.resultMessage!!, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        SectionHeading("Choose products")
        if (state.loading && state.products.isEmpty()) LoadingState()
        else if (state.products.isEmpty()) EmptyState("No available products. Add stock through a purchase.")
        state.products.take(20).forEach { product ->
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold)
                        Text("${product.availableQuantity} ${product.unitCode} · ${money(product.sellingPrice.toString())}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.addProduct(product) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add ${product.name}")
                    }
                }
            }
        }
        SectionHeading("Bill (${state.cart.size})")
        if (state.cart.isEmpty()) EmptyState("Add products to start a bill.")
        state.cart.forEach { line ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, fontWeight = FontWeight.Medium)
                        Text("${money(line.unitPrice.toPlainString())} / ${line.product.unitCode}",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        val next = line.quantity - BigDecimal.ONE
                        if (next.signum() <= 0) viewModel.removeProduct(line.product.id)
                        else viewModel.updateQuantity(line.product.id, next.toPlainString())
                    }) { Icon(Icons.Filled.Remove, contentDescription = "Reduce quantity") }
                    OutlinedTextField(
                        value = line.quantity.stripTrailingZeros().toPlainString(),
                        onValueChange = { viewModel.updateQuantity(line.product.id, it) },
                        modifier = Modifier.width(80.dp),
                        label = { Text("Qty") },
                        singleLine = true
                    )
                    IconButton(onClick = { viewModel.addProduct(line.product) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                    }
                }
                OutlinedTextField(
                    value = line.unitPrice.toPlainString(),
                    onValueChange = { viewModel.updatePrice(line.product.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Unit price (confirm changes at checkout)") },
                    singleLine = true
                )
            }
        }
        if (state.cart.isNotEmpty()) {
            SectionHeading("Village")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.villageId == null, onClick = { viewModel.chooseVillage(null) }, label = { Text("No village") })
                ChoiceDropdown("Village", state.villages.map { it.id to it.name }, state.villageId, viewModel::chooseVillage)
            }
            SectionHeading("Customer")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.customerId == null, onClick = { viewModel.chooseCustomer(null) }, label = { Text("Direct sale") })
                ChoiceDropdown("Customer", state.customers.map { it.id to it.name }, state.customerId, viewModel::chooseCustomer)
            }
            SectionHeading("Payment")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("CASH", "UPI", "CREDIT", "SPLIT_PAYMENT").forEach { mode ->
                    FilterChip(
                        selected = state.paymentMode == mode,
                        onClick = { viewModel.choosePaymentMode(mode) },
                        label = { Text(mode.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)) }
                    )
                }
            }
            if (state.paymentMode == "SPLIT_PAYMENT") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(state.splitCash, { viewModel.setSplitAmounts(it, state.splitUpi) },
                        modifier = Modifier.weight(1f), label = { Text("Cash") }, singleLine = true)
                    OutlinedTextField(state.splitUpi, { viewModel.setSplitAmounts(state.splitCash, it) },
                        modifier = Modifier.weight(1f), label = { Text("UPI") }, singleLine = true)
                }
                OutlinedTextField(
                    value = state.billDiscount,
                    onValueChange = viewModel::setBillDiscount,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    label = { Text("Bill discount (optional)") },
                    singleLine = true
                )
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal + tax", style = MaterialTheme.typography.bodyMedium)
                    Text(money((state.subtotal + state.tax).toPlainString()))
                }
                if (state.tax.signum() > 0) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Included tax", style = MaterialTheme.typography.bodySmall)
                        Text(money(state.tax.toPlainString()), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Grand total", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(money(state.total.toPlainString()), style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = {
                    confirmPriceOverride = state.cart.any {
                        it.unitPrice.compareTo(it.product.sellingPrice.value) != 0
                    }
                    if (confirmPriceOverride) viewModel.clearError() else viewModel.submit()
                },
                enabled = !state.saving && !state.loading,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(if (state.saving) "Submitting…" else "Confirm sale")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmPriceOverride) {
        AlertDialog(
            onDismissRequest = { confirmPriceOverride = false },
            title = { Text("Confirm changed prices") },
            text = { Text("One or more product prices differ from the default selling price. Confirm this price override before generating the bill.") },
            confirmButton = {
                Button(onClick = {
                    confirmPriceOverride = false
                    viewModel.submit(priceOverrideConfirmed = true)
                }) { Text("Confirm price and bill") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmPriceOverride = false }) { Text("Review") } }
        )
    }
}
