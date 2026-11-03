package com.mart.villagemart.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.DocumentRepository
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

const val DEFAULT_REPORT = "daily-sales"

data class ReportType(val key: String, val label: String, val periodic: Boolean = true)

val REPORT_TYPES = listOf(
    ReportType("daily-sales", "Sales"),
    ReportType("profit", "Profit"),
    ReportType("product-sales", "Products"),
    ReportType("village-sales", "Villages"),
    ReportType("category-sales", "Categories"),
    ReportType("expenses", "Expenses"),
    ReportType("purchases", "Purchases"),
    ReportType("payment-collections", "Collections"),
    ReportType("stock", "Stock", periodic = false),
    ReportType("customer-outstanding", "Credit due", periodic = false)
)

val REPORT_PERIODS = listOf(
    "TODAY" to "Today",
    "LAST_7_DAYS" to "7 days",
    "THIS_MONTH" to "This month",
    "LAST_MONTH" to "Last month",
    "THIS_YEAR" to "This year"
)

data class ReportSummaryRow(val label: String, val value: String)
data class ReportListRow(val title: String, val subtitle: String, val trailing: String)
data class ReportSection(val title: String, val rows: List<ReportListRow>)

data class ReportsState(
    val type: String = DEFAULT_REPORT,
    val period: String = "THIS_MONTH",
    val loading: Boolean = false,
    val error: String? = null,
    val summary: List<ReportSummaryRow> = emptyList(),
    val sections: List<ReportSection> = emptyList(),
    val loaded: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VillageMartRepository
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        ReportsState(
            type = savedStateHandle.get<String>("type")
                ?.takeIf { requested -> REPORT_TYPES.any { it.key == requested } } ?: DEFAULT_REPORT
        )
    )
    val state: StateFlow<ReportsState> = mutableState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun selectType(type: String) {
        if (type == mutableState.value.type) return
        mutableState.value = mutableState.value.copy(type = type, summary = emptyList(), sections = emptyList(), loaded = false)
        load()
    }

    fun selectPeriod(period: String) {
        if (period == mutableState.value.period) return
        mutableState.value = mutableState.value.copy(period = period)
        load()
    }

    fun load() {
        loadJob?.cancel()
        val current = mutableState.value
        val periodic = REPORT_TYPES.firstOrNull { it.key == current.type }?.periodic ?: true
        loadJob = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val report = repository.report(current.type, if (periodic) current.period else null)
                val (summary, sections) = flattenReport(report)
                mutableState.value = mutableState.value.copy(
                    loading = false, loaded = true, summary = summary, sections = sections
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, loaded = true, error = error.userMessage(JSON))
            }
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

private val TITLE_KEYS = listOf(
    "productName", "villageName", "categoryName", "customerName", "supplierName", "name",
    "invoiceNumber", "purchaseNumber", "date", "period", "month", "method", "paymentMethod", "category"
)
private val MONEY_KEY = Regex("amount|revenue|profit|sales|cost|value|expense|outstanding|collect|refund|total|price|paid|balance|charges|discount|tax", RegexOption.IGNORE_CASE)
private val NON_MONEY_KEY = Regex("count|quantity|products|qty|percent|number|invoices|days", RegexOption.IGNORE_CASE)

private fun humanize(key: String): String =
    key.replace(Regex("([a-z0-9])([A-Z])"), "$1 $2").replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)

private fun formatValue(key: String, primitive: JsonPrimitive): String {
    val raw = primitive.content
    if (primitive.isString) {
        return if (raw.matches(Regex("[A-Z_]+"))) humanize(raw.lowercase()) else raw
    }
    if (raw == "true" || raw == "false") return if (raw == "true") "Yes" else "No"
    return when {
        key.contains("percent", ignoreCase = true) -> "$raw%"
        MONEY_KEY.containsMatchIn(key) && !NON_MONEY_KEY.containsMatchIn(key) -> money(raw)
        else -> raw.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: raw
    }
}

private fun isIdKey(key: String) = key == "id" || key.endsWith("Id")

internal fun flattenReport(report: JsonObject): Pair<List<ReportSummaryRow>, List<ReportSection>> {
    val summary = mutableListOf<ReportSummaryRow>()
    val sections = mutableListOf<ReportSection>()
    fun visit(prefix: String?, obj: JsonObject) {
        obj.forEach { (key, element) ->
            if (isIdKey(key) || element is JsonNull) return@forEach
            when (element) {
                is JsonPrimitive -> summary += ReportSummaryRow(
                    listOfNotNull(prefix, humanize(key)).joinToString(" · "), formatValue(key, element)
                )
                is JsonObject -> visit(if (key in setOf("totals", "summary")) null else humanize(key), element)
                is JsonArray -> {
                    val rows = element.mapNotNull { item -> (item as? JsonObject)?.let(::toListRow) }
                    sections += ReportSection(humanize(key), rows)
                }
                else -> Unit
            }
        }
    }
    visit(null, report)
    return summary to sections
}

private fun toListRow(item: JsonObject): ReportListRow {
    val primitives = item.filter { (key, value) -> value is JsonPrimitive && !isIdKey(key) }
        .mapValues { it.value as JsonPrimitive }
    val titleKey = TITLE_KEYS.firstOrNull { it in primitives } ?: primitives.keys.firstOrNull()
    val title = titleKey?.let { primitives.getValue(it).content } ?: "—"
    val moneyKeys = primitives.keys.filter { it != titleKey && MONEY_KEY.containsMatchIn(it) && !NON_MONEY_KEY.containsMatchIn(it) }
    val trailingKey = listOf("netRevenue", "totalAmount", "netSales", "revenue", "amount", "stockValue", "outstandingBalance", "netProfit", "total")
        .firstOrNull { it in primitives } ?: moneyKeys.lastOrNull()
    val trailing = trailingKey?.let { formatValue(it, primitives.getValue(it)) }.orEmpty()
    val subtitle = primitives.filterKeys { it != titleKey && it != trailingKey }
        .entries.take(4)
        .joinToString(" · ") { (key, value) -> "${humanize(key)}: ${formatValue(key, value)}" }
    return ReportListRow(title, subtitle, trailing)
}

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val documentsViewModel: DocumentsViewModel = hiltViewModel()
    val documentState by documentsViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val type = REPORT_TYPES.firstOrNull { it.key == state.type } ?: REPORT_TYPES.first()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader("Reports", "Live business reports", onBack = onBack) {
            IconButton(onClick = viewModel::load, enabled = !state.loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            REPORT_TYPES.forEach { option ->
                FilterChip(
                    selected = option.key == state.type,
                    onClick = { viewModel.selectType(option.key) },
                    label = { Text(option.label) }
                )
            }
        }
        if (type.periodic) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                REPORT_PERIODS.forEach { (value, label) ->
                    FilterChip(
                        selected = value == state.period,
                        onClick = { viewModel.selectPeriod(value) },
                        label = { Text(label) }
                    )
                }
            }
        }
        ErrorMessage(state.error)
        when {
            state.loading && state.summary.isEmpty() && state.sections.isEmpty() -> LoadingState()
            state.error != null && state.summary.isEmpty() && state.sections.isEmpty() -> RetryButton(viewModel::load)
            state.loaded && state.summary.isEmpty() && state.sections.all { it.rows.isEmpty() } ->
                EmptyState("No data for this report and period.")
            else -> {
                if (state.summary.isNotEmpty()) {
                    Card(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${type.label} summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            state.summary.forEachIndexed { index, row ->
                                if (index > 0) HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(row.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(row.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                state.sections.forEach { section ->
                    SectionTitle(section.title)
                    if (section.rows.isEmpty()) {
                        EmptyState("No ${section.title.lowercase()} in this period.")
                    } else {
                        section.rows.forEach { row -> DataRow(row.title, row.subtitle, row.trailing) }
                    }
                }
            }
        }
        if (state.type in DocumentRepository.EXPORTABLE_REPORTS) {
            Spacer(Modifier.height(8.dp))
            ErrorMessage(documentState.error)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { documentsViewModel.report(state.type, "pdf", state.period) },
                    enabled = !documentState.loading,
                    modifier = Modifier.weight(1f)
                ) { Text("Export PDF") }
                OutlinedButton(
                    onClick = { documentsViewModel.report(state.type, "xlsx", state.period) },
                    enabled = !documentState.loading,
                    modifier = Modifier.weight(1f)
                ) { Text("Export Excel") }
            }
            if (documentState.loading) LoadingState()
            documentState.uri?.let { uri ->
                Button(
                    onClick = { shareFile(context, uri, documentState.fileName.orEmpty()) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) { Text("Share ${documentState.fileName ?: "report"}") }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}
