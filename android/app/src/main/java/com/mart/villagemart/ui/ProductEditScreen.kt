package com.mart.villagemart.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.CategoryDto
import com.mart.villagemart.data.DecimalValue
import com.mart.villagemart.data.ProductDto
import com.mart.villagemart.data.ProductRequest
import com.mart.villagemart.data.UnitDto
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import javax.inject.Inject

data class ProductForm(
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val categoryId: Long? = null,
    val unitId: Long? = null,
    val purchasePrice: String = "",
    val sellingPrice: String = "",
    val minSellingPrice: String = "",
    val minStock: String = "0",
    val taxRate: String = "0",
    val hsnCode: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val active: Boolean = true
)

data class ProductEditState(
    val productId: Long? = null,
    val loading: Boolean = true,
    val loadError: String? = null,
    val categories: List<CategoryDto> = emptyList(),
    val units: List<UnitDto> = emptyList(),
    val form: ProductForm = ProductForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: VillageMartRepository
) : ViewModel() {
    private val productId: Long? = savedStateHandle.get<Long>("productId")?.takeIf { it > 0 }
    private val mutableState = MutableStateFlow(ProductEditState(productId = productId))
    val state: StateFlow<ProductEditState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, loadError = null)
            try {
                val (options, product) = coroutineScope {
                    val options = async { repository.productOptions() }
                    val product = async { productId?.let { repository.product(it) } }
                    options.await() to product.await()
                }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    categories = options.first,
                    units = options.second,
                    form = product?.toForm() ?: mutableState.value.form
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, loadError = error.userMessage(JSON))
            }
        }
    }

    fun update(transform: (ProductForm) -> ProductForm) {
        mutableState.value = mutableState.value.copy(form = transform(mutableState.value.form), error = null)
    }

    fun save() {
        val current = mutableState.value
        if (current.saving) return
        val form = current.form
        val errors = linkedMapOf<String, String>()
        val purchase = form.purchasePrice.decimal()
        val selling = form.sellingPrice.decimal()
        val minPrice = form.minSellingPrice.decimal()
        val minStock = form.minStock.decimal()
        val tax = form.taxRate.decimal()
        if (form.name.isBlank()) errors["name"] = "Product name is required"
        if (form.categoryId == null) errors["category"] = "Select a category"
        if (form.unitId == null) errors["unit"] = "Select a unit"
        if (purchase == null || purchase.signum() < 0) errors["purchasePrice"] = "Enter a valid purchase price"
        if (selling == null || selling.signum() < 0) errors["sellingPrice"] = "Enter a valid selling price"
        if (form.minSellingPrice.isNotBlank() && (minPrice == null || minPrice.signum() < 0)) {
            errors["minSellingPrice"] = "Enter a valid minimum price"
        }
        if (minPrice != null && selling != null && minPrice > selling) {
            errors["minSellingPrice"] = "Minimum price cannot exceed selling price"
        }
        if (minStock == null || minStock.signum() < 0) errors["minStock"] = "Enter a valid minimum stock"
        if (form.taxRate.isNotBlank() && (tax == null || tax.signum() < 0 || tax > BigDecimal(100))) {
            errors["taxRate"] = "Tax rate must be between 0 and 100"
        }
        mutableState.value = current.copy(fieldErrors = errors, error = null)
        if (errors.isNotEmpty()) return
        val request = ProductRequest(
            name = form.name.trim(),
            sku = form.sku.trim().ifBlank { null },
            barcode = form.barcode.trim().ifBlank { null },
            categoryId = form.categoryId!!,
            unitId = form.unitId!!,
            description = form.description.trim().ifBlank { null },
            imageUrl = form.imageUrl.trim().ifBlank { null },
            purchasePrice = DecimalValue(purchase!!),
            sellingPrice = DecimalValue(selling!!),
            minSellingPrice = minPrice?.let(::DecimalValue),
            minStockQuantity = DecimalValue(minStock!!),
            taxRate = tax?.let(::DecimalValue),
            hsnCode = form.hsnCode.trim().ifBlank { null },
            active = form.active
        )
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null)
            try {
                if (productId == null) repository.createProduct(request) else repository.updateProduct(productId, request)
                mutableState.value = mutableState.value.copy(saving = false, saved = true)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    private fun String.decimal(): BigDecimal? = trim().takeIf(String::isNotEmpty)?.toBigDecimalOrNull()

    private fun ProductDto.toForm() = ProductForm(
        name = name,
        sku = sku.orEmpty(),
        barcode = barcode.orEmpty(),
        categoryId = categoryId,
        unitId = unitId,
        purchasePrice = purchasePrice.toString(),
        sellingPrice = sellingPrice.toString(),
        minSellingPrice = minSellingPrice?.toString().orEmpty(),
        minStock = minStockQuantity.toString(),
        taxRate = taxRate?.toString() ?: "0",
        hsnCode = hsnCode.orEmpty(),
        imageUrl = imageUrl.orEmpty(),
        description = description.orEmpty(),
        active = active
    )

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

@Composable
fun ProductEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProductEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    val isNew = state.productId == null
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = if (isNew) "Add product" else "Edit product",
            subtitle = if (isNew) "Create a new catalog item" else state.form.name.ifBlank { null },
            onBack = onBack
        )
        when {
            state.loading -> LoadingState()
            state.loadError != null -> {
                ErrorMessage(state.loadError)
                RetryButton(viewModel::load)
            }
            else -> ProductFormContent(state, viewModel)
        }
    }
}

@Composable
private fun ProductFormContent(state: ProductEditState, viewModel: ProductEditViewModel) {
    val form = state.form
    val errors = state.fieldErrors
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FormField(form.name, { v -> viewModel.update { it.copy(name = v) } }, "Product name")
        FieldError(errors["name"])
        FormField(form.sku, { v -> viewModel.update { it.copy(sku = v) } }, "SKU (optional)")
        FormField(form.barcode, { v -> viewModel.update { it.copy(barcode = v) } }, "Barcode (optional)")
        Text("Category", style = MaterialTheme.typography.labelLarge)
        ChoiceDropdown("category", state.categories.map { it.id to it.name }, form.categoryId) { id ->
            viewModel.update { it.copy(categoryId = id) }
        }
        FieldError(errors["category"])
        Text("Unit", style = MaterialTheme.typography.labelLarge)
        ChoiceDropdown("unit", state.units.map { it.id to "${it.name} (${it.code})" }, form.unitId) { id ->
            viewModel.update { it.copy(unitId = id) }
        }
        FieldError(errors["unit"])
        if (state.categories.isEmpty()) {
            Text("No categories found. Add a category from More → Categories first.",
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        FormField(form.purchasePrice, { v -> viewModel.update { it.copy(purchasePrice = v) } }, "Purchase price (₹)", numeric = true)
        FieldError(errors["purchasePrice"])
        FormField(form.sellingPrice, { v -> viewModel.update { it.copy(sellingPrice = v) } }, "Selling price (₹)", numeric = true)
        FieldError(errors["sellingPrice"])
        FormField(form.minSellingPrice, { v -> viewModel.update { it.copy(minSellingPrice = v) } }, "Minimum selling price (optional)", numeric = true)
        FieldError(errors["minSellingPrice"])
        FormField(form.minStock, { v -> viewModel.update { it.copy(minStock = v) } }, "Low-stock alert level", numeric = true)
        FieldError(errors["minStock"])
        FormField(form.taxRate, { v -> viewModel.update { it.copy(taxRate = v) } }, "Tax rate (%)", numeric = true)
        FieldError(errors["taxRate"])
        FormField(form.hsnCode, { v -> viewModel.update { it.copy(hsnCode = v) } }, "HSN code (optional)")
        FormField(form.imageUrl, { v -> viewModel.update { it.copy(imageUrl = v) } }, "Image URL (optional)")
        FormField(form.description, { v -> viewModel.update { it.copy(description = v) } }, "Description (optional)", singleLine = false)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Active product", Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = form.active, onCheckedChange = { v -> viewModel.update { it.copy(active = v) } })
        }
        ErrorMessage(state.error)
        Button(
            onClick = viewModel::save,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (state.saving) "Saving…" else if (state.productId == null) "Add product" else "Save changes") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FieldError(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
