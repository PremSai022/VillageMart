package com.mart.villagemart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mart.villagemart.data.CustomerDto
import com.mart.villagemart.data.DecimalValue
import com.mart.villagemart.data.PaymentLine
import com.mart.villagemart.data.ProductDto
import com.mart.villagemart.data.PendingSale
import com.mart.villagemart.data.PreferencesStore
import com.mart.villagemart.data.SaleItemRequest
import com.mart.villagemart.data.SaleRequest
import com.mart.villagemart.data.SaleSubmission
import com.mart.villagemart.data.VillageDto
import com.mart.villagemart.data.VillageMartRepository
import com.mart.villagemart.data.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class CartLine(
    val product: ProductDto,
    val quantity: BigDecimal = BigDecimal.ONE,
    val unitPrice: BigDecimal = product.sellingPrice.value
)

data class SalesState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val pendingSales: List<PendingSale> = emptyList(),
    val gstEnabled: Boolean = false,
    val villages: List<VillageDto> = emptyList(),
    val customers: List<CustomerDto> = emptyList(),
    val villageId: Long? = null,
    val customerId: Long? = null,
    val paymentMode: String = "CASH",
    val billDiscount: String = "",
    val splitCash: String = "",
    val splitUpi: String = "",
    val error: String? = null,
    val resultMessage: String? = null,
    val confirmedSaleId: Long? = null
) {
    val subtotal: BigDecimal get() = cart.fold(BigDecimal.ZERO) { sum, line ->
        sum + line.quantity.multiply(line.unitPrice).setScale(2, RoundingMode.HALF_UP)
    }
    val tax: BigDecimal get() {
        if (!gstEnabled || cart.isEmpty()) return BigDecimal.ZERO
        val lineAmounts = cart.map { it.quantity.multiply(it.unitPrice).setScale(2, RoundingMode.HALF_UP) }
        val beforeDiscount = lineAmounts.fold(BigDecimal.ZERO, BigDecimal::add)
        val bill = billDiscount.toBigDecimalOrNull() ?: BigDecimal.ZERO
        var allocated = BigDecimal.ZERO
        return cart.indices.fold(BigDecimal.ZERO) { taxSum, index ->
            val line = cart[index]
            val share = when {
                bill.signum() == 0 || beforeDiscount.signum() == 0 -> BigDecimal.ZERO
                index == cart.lastIndex -> bill.subtract(allocated)
                else -> bill.multiply(lineAmounts[index])
                    .divide(beforeDiscount, 2, RoundingMode.HALF_UP)
                    .also { allocated = allocated.add(it) }
            }
            val taxable = lineAmounts[index].subtract(share)
            taxSum + taxable.multiply(line.product.taxRate.value)
                .divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP)
        }
    }
    val total: BigDecimal get() = subtotal + tax - (billDiscount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
}

private data class SalesLoad(
    val products: List<ProductDto>,
    val villages: List<VillageDto>,
    val customers: List<CustomerDto>,
    val gstEnabled: Boolean
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val repository: VillageMartRepository,
    private val preferencesStore: PreferencesStore
) : ViewModel() {
    private val mutableState = MutableStateFlow(SalesState())
    val state: StateFlow<SalesState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.pendingSales.collect { pending ->
                mutableState.value = mutableState.value.copy(pendingSales = pending)
            }
        }
        viewModelScope.launch {
            preferencesStore.recentVillageId.collect { id ->
                if (id != null && mutableState.value.villageId == null) {
                    mutableState.value = mutableState.value.copy(villageId = id)
                }
            }
        }
    }

    fun load(query: String? = null) {
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val loaded = coroutineScope {
                    val products = async { repository.products(query).content }
                    val villages = async { repository.villages() }
                    val customers = async { repository.customers() }
                    val settings = async { repository.businessSettings() }
                    SalesLoad(products.await(), villages.await(), customers.await(), settings.await().gstEnabled)
                }
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    products = loaded.products.filter { it.active },
                    villages = loaded.villages,
                    customers = loaded.customers,
                    gstEnabled = loaded.gstEnabled
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(loading = false, error = error.userMessage(JSON))
            }
        }
    }

    fun addProduct(product: ProductDto) {
        if (product.availableQuantity.value.signum() <= 0) {
            mutableState.value = mutableState.value.copy(error = "${product.name} is out of stock")
            return
        }
        val cart = mutableState.value.cart.toMutableList()
        val existing = cart.indexOfFirst { it.product.id == product.id }
        if (existing < 0) cart.add(CartLine(product))
        else cart[existing] = cart[existing].copy(quantity = cart[existing].quantity + BigDecimal.ONE)
        mutableState.value = mutableState.value.copy(cart = cart, error = null, resultMessage = null)
    }

    fun updateQuantity(productId: Long, text: String) {
        val quantity = text.toBigDecimalOrNull()
        if (quantity == null || quantity.signum() <= 0) return
        val cart = mutableState.value.cart.map { line ->
            if (line.product.id == productId) line.copy(quantity = quantity) else line
        }
        mutableState.value = mutableState.value.copy(cart = cart)
    }

    fun updatePrice(productId: Long, text: String) {
        val price = text.toBigDecimalOrNull()
        if (price == null || price.signum() <= 0) return
        val cart = mutableState.value.cart.map { line ->
            if (line.product.id == productId) line.copy(unitPrice = price) else line
        }
        mutableState.value = mutableState.value.copy(cart = cart)
    }

    fun removeProduct(productId: Long) {
        mutableState.value = mutableState.value.copy(cart = mutableState.value.cart.filterNot { it.product.id == productId })
    }

    fun chooseVillage(id: Long?) {
        mutableState.value = mutableState.value.copy(villageId = id)
        if (id != null) {
            viewModelScope.launch { preferencesStore.rememberVillage(id) }
        }
    }

    fun chooseCustomer(id: Long?) {
        mutableState.value = mutableState.value.copy(customerId = id)
    }

    fun choosePaymentMode(mode: String) {
        mutableState.value = mutableState.value.copy(paymentMode = mode, error = null)
    }

    fun setBillDiscount(value: String) {
        mutableState.value = mutableState.value.copy(billDiscount = value)
    }

    fun setSplitAmounts(cash: String, upi: String) {
        mutableState.value = mutableState.value.copy(splitCash = cash, splitUpi = upi)
    }

    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    fun submit(priceOverrideConfirmed: Boolean = false) {
        val current = mutableState.value
        if (current.cart.isEmpty()) {
            mutableState.value = current.copy(error = "Add at least one product to the bill")
            return
        }
        val overStock = current.cart.firstOrNull { it.quantity > it.product.availableQuantity.value }
        if (overStock != null) {
            mutableState.value = current.copy(error = "Only ${overStock.product.availableQuantity} ${overStock.product.unitCode} of ${overStock.product.name} are available")
            return
        }
        val belowMinimum = current.cart.firstOrNull {
            it.product.minSellingPrice?.let { minimum -> it.unitPrice < minimum.value } == true
        }
        if (belowMinimum != null) {
            mutableState.value = current.copy(error = "${belowMinimum.product.name} is below its minimum selling price")
            return
        }
        if (current.billDiscount.toBigDecimalOrNull()?.let { it.signum() < 0 || it > current.subtotal + current.tax } == true) {
            mutableState.value = current.copy(error = "Discount cannot be negative or greater than the bill total")
            return
        }
        if (current.paymentMode == "CREDIT" && current.customerId == null) {
            mutableState.value = current.copy(error = "Select a customer before recording a credit sale")
            return
        }
        val paymentLines = when (current.paymentMode) {
            "CASH", "UPI" -> listOf(PaymentLine(current.paymentMode, DecimalValue(current.total)))
            "SPLIT_PAYMENT" -> {
                val cash = current.splitCash.toBigDecimalOrNull() ?: BigDecimal.ZERO
                val upi = current.splitUpi.toBigDecimalOrNull() ?: BigDecimal.ZERO
                if (cash.signum() < 0 || upi.signum() < 0 || cash + upi > current.total ||
                    (cash + upi).signum() == 0
                ) {
                    mutableState.value = current.copy(error = "Split payments must be positive and cannot exceed the bill total")
                    return
                }
                if (cash + upi < current.total && current.customerId == null) {
                    mutableState.value = current.copy(error = "Select a customer to record the remaining amount as credit")
                    return
                }
                buildList {
                    if (cash.signum() > 0) add(PaymentLine("CASH", DecimalValue(cash)))
                    if (upi.signum() > 0) add(PaymentLine("UPI", DecimalValue(upi)))
                }
            }
            else -> emptyList()
        }
        val request = SaleRequest(
            customerId = current.customerId,
            villageId = current.villageId,
            items = current.cart.map {
                SaleItemRequest(
                    it.product.id,
                    DecimalValue(it.quantity),
                    unitPrice = it.unitPrice.takeIf { price -> price.compareTo(it.product.sellingPrice.value) != 0 }?.let(::DecimalValue)
                )
            },
            billDiscount = current.billDiscount.toBigDecimalOrNull()?.let(::DecimalValue),
            paymentMode = current.paymentMode,
            payments = paymentLines,
            priceOverrideConfirmed = priceOverrideConfirmed
        )
        val hasOverride = current.cart.any { it.unitPrice.compareTo(it.product.sellingPrice.value) != 0 }
        if (hasOverride && !priceOverrideConfirmed) {
            mutableState.value = current.copy(error = "Price changed; owner confirmation is required before billing")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(saving = true, error = null, resultMessage = null)
            try {
                when (val result = repository.submitSale(request)) {
                    is SaleSubmission.Confirmed -> mutableState.value = mutableState.value.copy(
                        saving = false,
                        cart = emptyList(),
                        billDiscount = "",
                        splitCash = "",
                        splitUpi = "",
                        resultMessage = "Invoice ${result.sale.invoiceNumber} confirmed",
                        confirmedSaleId = result.sale.id,
                        error = null
                    )
                    is SaleSubmission.Queued -> mutableState.value = mutableState.value.copy(
                        saving = false,
                        resultMessage = "Sale saved for retry. The invoice is not confirmed yet.",
                        error = null
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                mutableState.value = mutableState.value.copy(saving = false, error = error.userMessage(JSON))
            }
        }
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
