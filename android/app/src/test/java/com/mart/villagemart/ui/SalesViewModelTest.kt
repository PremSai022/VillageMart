package com.mart.villagemart.ui

import com.mart.villagemart.data.DecimalValue
import com.mart.villagemart.data.ProductDto
import com.mart.villagemart.data.PreferencesStore
import com.mart.villagemart.data.SaleDto
import com.mart.villagemart.data.SaleSubmission
import com.mart.villagemart.data.VillageMartRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {
    private val repository = mockk<VillageMartRepository>()
    private val preferencesStore = mockk<PreferencesStore>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.pendingSales } returns flowOf(emptyList())
        every { preferencesStore.recentVillageId } returns flowOf(null)
        coEvery { preferencesStore.rememberVillage(any()) } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun rejectsQuantityAboveAvailableStockBeforeSubmitting() = runTest(dispatcher) {
        val viewModel = SalesViewModel(repository, preferencesStore)
        val product = product(available = "2")
        viewModel.addProduct(product)
        viewModel.updateQuantity(product.id, "3")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Only 2 PCS of Rice are available", viewModel.state.value.error)
        coVerify(exactly = 0) { repository.submitSale(any()) }
    }

    @Test
    fun requiresExplicitConfirmationForPriceOverrideAndThenConfirmsSale() = runTest(dispatcher) {
        coEvery { repository.submitSale(any()) } returns SaleSubmission.Confirmed(sale())
        val viewModel = SalesViewModel(repository, preferencesStore)
        val product = product(available = "10")
        viewModel.addProduct(product)
        viewModel.updatePrice(product.id, "12.00")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Price changed; owner confirmation is required before billing", viewModel.state.value.error)
        coVerify(exactly = 0) { repository.submitSale(any()) }

        viewModel.submit(priceOverrideConfirmed = true)
        advanceUntilIdle()
        assertEquals("Invoice VM-100 confirmed", viewModel.state.value.resultMessage)
        assertEquals(0, viewModel.state.value.cart.size)
        coVerify(exactly = 1) {
            repository.submitSale(match {
                it.priceOverrideConfirmed &&
                    it.items.single().unitPrice == DecimalValue("12.00")
            })
        }
    }

    @Test
    fun creditSaleRequiresCustomer() = runTest(dispatcher) {
        val viewModel = SalesViewModel(repository, preferencesStore)
        viewModel.addProduct(product(available = "10"))
        viewModel.choosePaymentMode("CREDIT")
        viewModel.submit()
        advanceUntilIdle()

        assertNotNull(viewModel.state.value.error)
        coVerify(exactly = 0) { repository.submitSale(any()) }
    }

    private fun product(available: String) = ProductDto(
        id = 1,
        name = "Rice",
        categoryId = 1,
        categoryName = "Grains",
        unitId = 1,
        unitCode = "PCS",
        unitName = "Piece",
        purchasePrice = DecimalValue("8.00"),
        sellingPrice = DecimalValue("10.00"),
        minSellingPrice = DecimalValue("9.00"),
        minStockQuantity = DecimalValue("2"),
        availableQuantity = DecimalValue(available),
        active = true
    )

    private fun sale() = SaleDto(
        id = 10,
        invoiceNumber = "VM-100",
        saleDate = "2026-11-03",
        saleType = "DIRECT",
        status = "COMPLETED",
        paymentMode = "CASH",
        subtotal = DecimalValue("12.00"),
        taxableAmount = DecimalValue("12.00"),
        totalAmount = DecimalValue("12.00"),
        paidAmount = DecimalValue("12.00"),
        outstandingAmount = DecimalValue.ZERO
    )
}
