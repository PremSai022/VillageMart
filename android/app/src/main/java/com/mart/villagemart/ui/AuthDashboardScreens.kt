package com.mart.villagemart.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(104.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "VillageMart",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Mobile village retail, simplified",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(36.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun LoginScreen(
    busy: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp))
            Text("VillageMart", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Your business, in your hands.", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            FormField(login, { login = it; onClearError() }, "Username or mobile number")
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; onClearError() },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            ErrorMessage(error)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onLogin(login, password) },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (busy) CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                else Text("Sign in")
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Sign in with the owner account configured for your VillageMart server.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val dashboardPeriods = listOf("TODAY" to "Today", "LAST_7_DAYS" to "7 days", "THIS_MONTH" to "This month")

@Composable
fun DashboardScreen(
    ownerName: String,
    onNavigate: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val periodLabel = when (period) {
        "TODAY" -> "Today's"
        "LAST_7_DAYS" -> "7-day"
        else -> "This month's"
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Good day, ${ownerName.ifBlank { "Owner" }}", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("VillageMart dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            if (state.loading && state.data != null) {
                CircularProgressIndicator(Modifier.size(24.dp).padding(end = 4.dp), strokeWidth = 2.dp)
            }
            IconButton(onClick = { viewModel.refresh() }, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh dashboard")
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dashboardPeriods.forEach { (value, label) ->
                FilterChip(selected = period == value, onClick = { viewModel.refresh(value) }, label = { Text(label) })
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { onNavigate(Routes.NEW_SALE) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.PointOfSale, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New sale")
            }
            FilledTonalButton(
                onClick = { onNavigate(Routes.purchases(add = true)) },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.AddShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add purchase")
            }
        }
        ErrorMessage(state.error)
        val data = state.data
        when {
            data == null && state.loading -> LoadingState()
            data == null && state.error != null -> RetryButton { viewModel.refresh() }
            data == null -> EmptyState("Dashboard data is not available yet.")
        }
        if (data != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("$periodLabel sales", money(data.salesAmount.toString()), Modifier.weight(1f),
                    supporting = "Collected ${money(data.netCollected.toString())}",
                    onClick = { onNavigate(Routes.SALES_HISTORY) })
                MetricCard("$periodLabel profit", money(data.netProfit.toString()), Modifier.weight(1f),
                    accent = if (data.netProfit.value.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    supporting = "Gross ${money(data.grossProfit.toString())}",
                    onClick = { onNavigate(Routes.reports("profit")) })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Total products", data.totalProducts.toString(), Modifier.weight(1f),
                    supporting = "Active catalog",
                    onClick = { onNavigate(Routes.PRODUCTS) })
                MetricCard("Available stock", money(data.totalInventoryValue.toString()), Modifier.weight(1f),
                    supporting = "${data.totalInventoryQuantity} units on hand",
                    onClick = { onNavigate(Routes.INVENTORY) })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Low-stock alerts", data.lowStockCount.toString(), Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.tertiary,
                    supporting = "Need restocking",
                    onClick = { onNavigate(Routes.LOW_STOCK) })
                MetricCard("Out-of-stock alerts", data.outOfStockCount.toString(), Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.error,
                    supporting = "Unavailable to sell",
                    onClick = { onNavigate(Routes.OUT_OF_STOCK) })
            }
            if (data.totalOutstanding.value.signum() > 0) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                    MetricCard("Customer credit outstanding", money(data.totalOutstanding.toString()), Modifier.weight(1f),
                        accent = MaterialTheme.colorScheme.tertiary,
                        supporting = "Credit given in period ${money(data.creditGiven.toString())}",
                        onClick = { onNavigate(Routes.CUSTOMERS) })
                }
            }
            SectionHeading("Stock alerts", "View all") { onNavigate(Routes.LOW_STOCK) }
            val alerts = data.outOfStockItems.take(3) + data.lowStockItems.take(3)
            if (alerts.isEmpty()) EmptyState("Stock levels are healthy.")
            alerts.forEach { item ->
                DataRow(
                    item.productName,
                    "${item.availableQuantity} ${item.unitCode} · minimum ${item.minStockQuantity}",
                    trailing = "Restock ${item.suggestedReorderQuantity}",
                    status = item.stockStatus,
                    onClick = { onNavigate(if (item.stockStatus == "OUT_OF_STOCK") Routes.OUT_OF_STOCK else Routes.LOW_STOCK) }
                )
            }
            SectionHeading("Best sellers", "Reports") { onNavigate(Routes.reports("product-sales")) }
            if (data.bestSellers.isEmpty()) EmptyState("No products sold in this period.")
            data.bestSellers.take(5).forEach { row ->
                DataRow(row.productName, "Qty sold ${row.quantitySold}", money(row.netRevenue.toString()))
            }
            SectionHeading("Recent sales", "See all") { onNavigate(Routes.SALES_HISTORY) }
            if (data.recentSales.isEmpty()) EmptyState("No sales recorded in this period.")
            data.recentSales.take(5).forEach { sale ->
                DataRow(sale.invoiceNumber, sale.customerName ?: sale.villageName ?: "Direct sale",
                    money(sale.totalAmount.toString()), sale.status)
            }
            SectionHeading("Recent purchases", "See all") { onNavigate(Routes.purchases()) }
            if (data.recentPurchases.isEmpty()) EmptyState("No purchases recorded in this period.")
            data.recentPurchases.take(5).forEach { purchase ->
                DataRow(purchase.purchaseNumber, "${purchase.supplierName} · ${purchase.purchaseDate}",
                    money(purchase.totalAmount.toString()), purchase.status)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

fun money(value: String): String = runCatching {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value.toBigDecimal())
}.getOrElse { "₹$value" }
