package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mart.villagemart.data.ProductDto

@Composable
fun ProductsScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    viewModel: ProductsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<ProductDto?>(null) }
    // Reload on every return to this destination so edits made on the edit screen are visible.
    LaunchedEffect(Unit) { viewModel.load(query) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Products",
            subtitle = if (state.loaded) "${state.products.size} products" else "Loading catalog…"
        ) {
            IconButton(onClick = { viewModel.load(query) }, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Filled.Add, contentDescription = "Add product")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; viewModel.load(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            label = { Text("Search name, SKU or barcode") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
        )
        ErrorMessage(state.error)
        when {
            state.loading && state.products.isEmpty() -> LoadingState()
            state.products.isEmpty() && state.error != null -> RetryButton { viewModel.load(query) }
            state.products.isEmpty() && state.loaded -> EmptyState(
                if (query.isBlank()) "No products yet. Tap + to add your first product." else "No products match \"$query\"."
            )
            else -> LazyColumn(Modifier.weight(1f)) {
                items(state.products, key = { it.id }) { product ->
                    ProductRow(product) { selected = product }
                }
            }
        }
    }
    selected?.let { product ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(product.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("SKU: ${product.sku ?: "—"}")
                    Text("Category: ${product.categoryName}")
                    Text("Selling price: ${money(product.sellingPrice.toString())} / ${product.unitCode}")
                    Text("Purchase price: ${money(product.purchasePrice.toString())}")
                    Text("Available: ${product.availableQuantity} ${product.unitCode}")
                    Text("Status: ${product.stockStatus}${if (product.active) "" else " · INACTIVE"}")
                    product.description?.let { Text(it) }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { selected = null; onEditProduct(product.id) }) { Text("Edit") }
                    TextButton(onClick = { selected = null }) { Text("Done") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setActive(product, !product.active)
                    selected = null
                }) { Text(if (product.active) "Deactivate" else "Activate") }
            }
        )
    }
}

@Composable
private fun ProductRow(product: ProductDto, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text("${product.categoryName} · ${product.sku ?: product.barcode ?: "No SKU"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${product.availableQuantity} ${product.unitCode} available · ${product.stockStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (product.stockStatus == "NORMAL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Text(money(product.sellingPrice.toString()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChoiceDropdown(
    label: String,
    options: List<Pair<Long, String>>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), enabled = options.isNotEmpty()) {
            Text(options.firstOrNull { it.first == selectedId }?.second ?: if (options.isEmpty()) "$label unavailable" else "Select $label")
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.second) },
                    onClick = { onSelect(option.first); expanded = false }
                )
            }
        }
    }
}
