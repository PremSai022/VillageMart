package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.Intent
import com.mart.villagemart.data.DecimalValue
import com.mart.villagemart.data.SaleDto
import com.mart.villagemart.data.SalesReturnItemRequest
import com.mart.villagemart.data.SalesReturnRequest

private data class MoreItem(val route: String, val title: String, val description: String, val icon: ImageVector)

@Composable
fun MoreScreen(onOpen: (String) -> Unit) {
    val groups = listOf(
        MoreItem(Routes.purchases(), "Purchases", "Wholesale records and stock intake", Icons.AutoMirrored.Filled.ReceiptLong),
        MoreItem(Routes.SALES_HISTORY, "Sales and billing history", "Invoices, returns and cancellations", Icons.Filled.Payments),
        MoreItem(Routes.INVENTORY, "Inventory", "Current trolley stock and value", Icons.Filled.Inventory2),
        MoreItem(Routes.LOW_STOCK, "Low-stock alerts", "Products that need replenishing", Icons.Filled.Notifications),
        MoreItem(Routes.OUT_OF_STOCK, "Out of stock", "Unavailable products", Icons.Filled.Notifications),
        MoreItem(Routes.STOCK_MOVEMENTS, "Stock history", "Audited inventory movements", Icons.Filled.History),
        MoreItem(Routes.STOCK_ADJUSTMENT, "Stock adjustment", "Record a reasoned stock change", Icons.Filled.Tune),
        MoreItem(Routes.CUSTOMERS, "Customers", "Customer accounts and balances", Icons.Filled.Groups),
        MoreItem(Routes.VILLAGES, "Villages", "Villages on your sales routes", Icons.Filled.LocationCity),
        MoreItem(Routes.SUPPLIERS, "Suppliers", "Wholesale shop contacts", Icons.Filled.Store),
        MoreItem(Routes.EXPENSES, "Expenses", "Fuel, maintenance and other costs", Icons.Filled.Payments),
        MoreItem(Routes.reports(), "Reports", "Sales, profit, stock and village reports", Icons.Filled.Assessment),
        MoreItem(Routes.NOTIFICATIONS, "Notifications", "Business and stock alerts", Icons.Filled.Notifications),
        MoreItem(Routes.CATEGORIES, "Categories", "Organize product catalog", Icons.Filled.Category),
        MoreItem(Routes.SETTINGS, "Settings", "Business details, account and sign out", Icons.Filled.Settings)
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("More", "Business tools")
        groups.forEach { item ->
            Card(
                onClick = { onOpen(item.route) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(item.description, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun featureTitle(feature: String): String = when (feature) {
    "purchases" -> "Purchases"
    "inventory" -> "Inventory"
    "low-stock" -> "Low-stock alerts"
    "out-of-stock" -> "Out of stock"
    "stock-movements" -> "Stock history"
    "stock-adjustment" -> "Stock adjustment"
    "sales" -> "Sales and billing"
    "suppliers" -> "Suppliers"
    "customers" -> "Customers"
    "villages" -> "Villages"
    "expenses" -> "Expenses"
    "notifications" -> "Notifications"
    "categories" -> "Categories"
    else -> feature.replace('-', ' ').replaceFirstChar(Char::uppercase)
}

private fun emptyMessage(feature: String): String = when (feature) {
    "purchases" -> "No purchases yet. Tap + to record stock bought from a supplier."
    "inventory" -> "No stock records yet. Add products and record a purchase."
    "low-stock" -> "No products are running low."
    "out-of-stock" -> "Every product is in stock."
    "stock-movements" -> "No stock movements recorded yet."
    "stock-adjustment" -> "No products available to adjust."
    "sales" -> "No sales yet. Use Sell to create your first bill."
    "suppliers" -> "No suppliers yet. Tap + to add one."
    "customers" -> "No customers yet. Tap + to add one."
    "villages" -> "No villages yet. Tap + to add the villages you serve."
    "expenses" -> "No expenses recorded yet."
    "notifications" -> "You're all caught up."
    "categories" -> "No categories yet. Tap + to add one."
    else -> "No records available."
}

@Composable
fun ManagementScreen(
    feature: String,
    openAddOnStart: Boolean = false,
    onBack: (() -> Unit)? = null,
    onOpen: (String) -> Unit = {},
    viewModel: ManagementViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val documentsViewModel: DocumentsViewModel = hiltViewModel()
    val documentState by documentsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by rememberSaveable(feature) { mutableStateOf("") }
    var showAdd by rememberSaveable(feature) { mutableStateOf(openAddOnStart) }
    var selected by remember { mutableStateOf<FeatureRecord?>(null) }
    var confirmCancel by remember { mutableStateOf<FeatureRecord?>(null) }
    var collectCustomer by remember { mutableStateOf<FeatureRecord?>(null) }
    var returnSale by remember { mutableStateOf<SaleDto?>(null) }
    LaunchedEffect(feature) { viewModel.load(feature) }
    LaunchedEffect(state.savedCount) { if (state.savedCount > 0) showAdd = false }
    LaunchedEffect(state.message) {
        if (state.message?.startsWith("Payment recorded.") == true) collectCustomer = null
        if (state.message?.startsWith("Return recorded.") == true) returnSale = null
    }
    val title = featureTitle(feature)
    val canAdd = feature in setOf("purchases", "stock-adjustment", "suppliers", "customers", "villages", "expenses", "categories")
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = title,
            subtitle = if (feature == "low-stock" || feature == "out-of-stock") "Live status from current inventory"
            else "Live information from your business account",
            onBack = onBack
        ) {
            IconButton(onClick = { viewModel.load(feature, query) }, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            if (canAdd) {
                FloatingActionButton(onClick = { viewModel.clearFeedback(); showAdd = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add $title")
                }
            }
        }
        if (feature == "inventory") {
            state.inventorySummary?.let { summary ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Stock value", money(summary.totalValue.toString()), Modifier.weight(1f),
                        supporting = "${summary.totalQuantity} units · ${summary.totalProducts} products")
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricCard("Low stock", summary.lowStockCount.toString(), Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary, onClick = { onOpen(Routes.LOW_STOCK) })
                    MetricCard("Out of stock", summary.outOfStockCount.toString(), Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.error, onClick = { onOpen(Routes.OUT_OF_STOCK) })
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(onClick = { onOpen(Routes.STOCK_ADJUSTMENT) }, label = { Text("Adjust stock") })
                AssistChip(onClick = { onOpen(Routes.STOCK_MOVEMENTS) }, label = { Text("Stock history") })
                AssistChip(onClick = { onOpen(Routes.purchases(add = true)) }, label = { Text("Add purchase") })
            }
        }
        if (feature in setOf("suppliers", "customers", "villages", "inventory", "sales")) {
            OutlinedTextField(
                query,
                onValueChange = { query = it; viewModel.load(feature, it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                label = { Text("Search") },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            )
        }
        if (!showAdd && collectCustomer == null && returnSale == null) ErrorMessage(state.error)
        state.message?.let {
            Text(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
        when {
            state.loading && state.records.isEmpty() -> LoadingState()
            state.records.isEmpty() && state.error != null && state.loaded -> RetryButton { viewModel.load(feature, query) }
            state.records.isEmpty() && state.loaded -> EmptyState(emptyMessage(feature))
            else -> LazyColumn(Modifier.weight(1f)) {
                items(state.records, key = { "${feature}-${it.id}" }) { record ->
                    val action: (() -> Unit)? = when {
                        feature == "notifications" && record.status == "UNREAD" -> ({ viewModel.markRead(record.id) })
                        feature == "notifications" -> null
                        else -> ({
                            selected = record
                            if (feature == "sales") viewModel.loadSaleDetails(record.id)
                        })
                    }
                    DataRow(record.title, record.subtitle, record.trailing, record.status, action)
                }
            }
        }
    }
    if (showAdd) {
        FeatureFormDialog(
            feature = feature,
            options = state.options,
            secondaryOptions = state.secondaryOptions,
            saving = state.saving,
            error = state.error,
            onDismiss = { showAdd = false; viewModel.clearFeedback() },
            onSave = { viewModel.create(feature, it) }
        )
    }
    selected?.let { record ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(record.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listOf(record.subtitle, record.trailing, record.status).filter(String::isNotBlank).joinToString("\n"))
                    if (feature == "sales") {
                        if (state.saleDetailLoading) LoadingState()
                        state.saleDetail?.takeIf { it.id == record.id }?.let { sale ->
                            sale.items.forEach { item ->
                                Text("${item.productName}: ${item.quantity} ${item.unitCode} · ${item.returnableQuantity} returnable")
                            }
                            sale.returns.forEach { ret ->
                                Text("${ret.returnNumber}: returned ${money(ret.refundAmount.toString())}")
                            }
                        }
                        ErrorMessage(state.error)
                        ErrorMessage(documentState.error)
                        if (documentState.loading) LoadingState()
                        documentState.uri?.let { uri ->
                            OutlinedButton(onClick = { shareFile(context, uri, documentState.fileName.orEmpty()) }) {
                                Text("Share invoice PDF")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (feature == "sales" && record.status == "COMPLETED") {
                    TextButton(onClick = {
                        documentsViewModel.invoice(record.id)
                    }) { Text("Get invoice") }
                }
            },
            dismissButton = {
                Row {
                    if (feature == "sales" && record.status == "COMPLETED") {
                        TextButton(onClick = { selected = null; confirmCancel = record }) { Text("Cancel sale") }
                    }
                    if (feature == "sales" && record.status == "COMPLETED" &&
                        state.saleDetail?.let { sale ->
                            sale.id == record.id && sale.items.any { it.returnableQuantity.value.signum() > 0 }
                        } == true
                    ) {
                        TextButton(onClick = { returnSale = state.saleDetail; selected = null }) { Text("Return items") }
                    }
                    if (feature == "customers" && (record.trailing.toBigDecimalOrNull()?.signum() ?: 0) > 0) {
                        TextButton(onClick = { collectCustomer = record; selected = null }) { Text("Collect payment") }
                    }
                    TextButton(onClick = { selected = null }) { Text("Close") }
                }
            }
        )
    }
    confirmCancel?.let { record ->
        AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text("Cancel ${record.title}?") },
            text = { Text("This reverses the sale and restores stock. The action is recorded in history.") },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelSale(record.id); confirmCancel = null }) { Text("Cancel sale") }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text("Keep sale") } }
        )
    }
    collectCustomer?.let { record ->
        CreditCollectionDialog(
            customer = record,
            saving = state.saving,
            error = state.error,
            onDismiss = { collectCustomer = null },
            onCollect = { amount, method, key ->
                viewModel.collectCredit(record.id, amount, method, key)
            }
        )
    }
    returnSale?.let { sale ->
        SalesReturnDialog(
            sale = sale,
            saving = state.saving,
            error = state.error,
            onDismiss = { returnSale = null },
            onSubmit = { request, key -> viewModel.createSalesReturn(sale.id, key, request) }
        )
    }
}


@Composable
private fun CreditCollectionDialog(
    customer: FeatureRecord,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCollect: (amount: String, method: String, idempotencyKey: String) -> Unit
) {
    var amount by rememberSaveable(customer.id) { mutableStateOf("") }
    var method by rememberSaveable(customer.id) { mutableStateOf("CASH") }
    var idempotencyKey by rememberSaveable(customer.id) {
        mutableStateOf(java.util.UUID.randomUUID().toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect credit payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${customer.title}: ₹${customer.trailing} outstanding")
                FormField(amount, { amount = it }, "Amount (₹)", numeric = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = method == "CASH", onClick = { method = "CASH" }, label = { Text("Cash") })
                    FilterChip(selected = method == "UPI", onClick = { method = "UPI" }, label = { Text("UPI") })
                }
                ErrorMessage(error)
            }
        },
        confirmButton = {
            Button(enabled = !saving && (amount.toBigDecimalOrNull()?.signum() ?: 0) > 0, onClick = {
                onCollect(amount, method, idempotencyKey)
            }) { Text(if (saving) "Saving..." else "Record payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SalesReturnDialog(
    sale: SaleDto,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (SalesReturnRequest, String) -> Unit
) {
    val quantities = remember(sale.id) { mutableStateMapOf<Long, String>() }
    val saleable = remember(sale.id) { mutableStateMapOf<Long, Boolean>() }
    var reason by rememberSaveable(sale.id) { mutableStateOf("") }
    var refundMethod by rememberSaveable(sale.id) { mutableStateOf<Long?>(1L) }
    val idempotencyKey = rememberSaveable(sale.id) {
        mutableStateOf(java.util.UUID.randomUUID().toString())
    }.value
    val selectedItems = sale.items.mapNotNull { item ->
        val quantity = quantities[item.id]?.toBigDecimalOrNull()
        if (quantity != null && quantity.signum() > 0 && quantity <= item.returnableQuantity.value) {
            SalesReturnItemRequest(item.id, DecimalValue(quantity), saleable[item.id] ?: true)
        } else {
            null
        }
    }
    val invalidQuantity = quantities.any { (id, value) ->
        val quantity = value.toBigDecimalOrNull()
        val returnable = sale.items.firstOrNull { it.id == id }?.returnableQuantity?.value
        value.isNotBlank() && (quantity == null || quantity.signum() <= 0 ||
            returnable == null || quantity > returnable)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return items from ${sale.invoiceNumber}") },
        text = {
            Column(
                Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sale.items.filter { it.returnableQuantity.value.signum() > 0 }.forEach { item ->
                    Text("${item.productName} · ${item.returnableQuantity} ${item.unitCode} remaining")
                    FormField(
                        quantities[item.id].orEmpty(),
                        { quantities[item.id] = it },
                        "Return quantity",
                        numeric = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = saleable[item.id] ?: true,
                            onCheckedChange = { saleable[item.id] = it }
                        )
                        Text("Saleable; restore to stock", Modifier.padding(top = 12.dp))
                    }
                }
                FormField(reason, { reason = it }, "Return reason")
                ChoiceDropdown("Refund method", listOf(1L to "Cash", 2L to "UPI"), refundMethod) {
                    refundMethod = it
                }
                ErrorMessage(error)
            }
        },
        confirmButton = {
            Button(
                enabled = !saving && reason.isNotBlank() && selectedItems.isNotEmpty() && !invalidQuantity,
                onClick = {
                    onSubmit(
                        SalesReturnRequest(
                            items = selectedItems,
                            reason = reason.trim(),
                            refundMethod = if (refundMethod == 2L) "UPI" else "CASH"
                        ),
                        idempotencyKey
                    )
                }
            ) { Text(if (saving) "Saving..." else "Record return") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FeatureFormDialog(
    feature: String,
    options: List<Pair<Long, String>>,
    secondaryOptions: List<Pair<Long, String>>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit
) {
    val values = remember(feature) { mutableStateMapOf<String, String>() }
    var optionId by remember(feature) { mutableStateOf<Long?>(null) }
    var secondaryId by remember(feature) { mutableStateOf<Long?>(null) }
    var direction by remember(feature) { mutableStateOf("INCREASE") }
    val title = when (feature) {
        "purchases" -> "Record purchase"
        "stock-adjustment" -> "Adjust stock"
        "suppliers" -> "Add supplier"
        "customers" -> "Add customer"
        "villages" -> "Add village"
        "expenses" -> "Add expense"
        else -> "Add category"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (feature) {
                    "suppliers" -> {
                        Field(values, "name", "Supplier name")
                        Field(values, "mobile", "Mobile number")
                        Field(values, "address", "Address")
                        Field(values, "gstNumber", "GST number (optional)")
                    }
                    "customers" -> {
                        Field(values, "name", "Customer name")
                        Field(values, "mobile", "Mobile number")
                        Field(values, "address", "Address")
                    }
                    "villages" -> {
                        Field(values, "name", "Village name")
                        Field(values, "district", "District (optional)")
                        Field(values, "state", "State")
                    }
                    "categories" -> {
                        Field(values, "name", "Category name")
                        Field(values, "description", "Description (optional)")
                    }
                    "expenses" -> {
                        ChoiceDropdown("Expense category", options, optionId) { optionId = it }
                        Field(values, "amount", "Amount (₹)", numeric = true)
                        Field(values, "date", "Date (YYYY-MM-DD, optional)")
                        Field(values, "description", "Description")
                    }
                    "purchases" -> {
                        ChoiceDropdown("Supplier", options, optionId) { optionId = it }
                        ChoiceDropdown("Product", secondaryOptions, secondaryId) { secondaryId = it }
                        Field(values, "quantity", "Quantity", numeric = true)
                        Field(values, "unitPrice", "Unit purchase price", numeric = true)
                        Field(values, "discount", "Line discount (optional)", numeric = true)
                        Field(values, "transport", "Transport cost (optional)", numeric = true)
                    }
                    "stock-adjustment" -> {
                        ChoiceDropdown("Product", options, optionId) { optionId = it }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { direction = "INCREASE" }, modifier = Modifier.weight(1f)) {
                                Text(if (direction == "INCREASE") "✓ Increase" else "Increase")
                            }
                            OutlinedButton(onClick = { direction = "DECREASE" }, modifier = Modifier.weight(1f)) {
                                Text(if (direction == "DECREASE") "✓ Decrease" else "Decrease")
                            }
                        }
                        Field(values, "quantity", "Quantity", numeric = true)
                        Field(values, "unitCost", "Unit cost if adding (optional)", numeric = true)
                        Field(values, "reason", "Reason (required)")
                    }
                }
                ErrorMessage(error)
            }
        },
        confirmButton = {
            Button(enabled = !saving, onClick = {
                val payload = values.toMutableMap()
                if (optionId != null) payload[if (feature == "expenses") "categoryId" else if (feature == "purchases") "supplierId" else "productId"] = optionId.toString()
                if (feature == "purchases" && secondaryId != null) payload["productId"] = secondaryId.toString()
                if (feature == "stock-adjustment") payload["direction"] = direction
                onSave(payload)
            }) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun Field(values: MutableMap<String, String>, key: String, label: String, numeric: Boolean = false) {
    FormField(values[key].orEmpty(), { values[key] = it }, label, numeric = numeric)
}

internal fun shareFile(context: android.content.Context, uri: android.net.Uri, title: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (title.endsWith(".pdf")) "application/pdf"
        else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = ClipData.newUri(context.contentResolver, title, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share $title"))
}
