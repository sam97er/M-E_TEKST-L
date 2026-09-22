package com.example.domain

import com.example.domain.calculator.ProfitCalculatorEngine
import com.example.security.ProfitCalculationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfitCalculatorEngineTest {

    private val testRules = ProfitCalculationRules(
        defaultCommissionRate = 20.0,
        defaultShippingCost = 35.0,
        defaultTaxRate = 10.0,
        returnExpenseBufferPercent = 5.0,
        packagingAndOtherCost = 10.0
    )

    @Test
    fun testProfitableCalculation() {
        val result = ProfitCalculatorEngine.calculate(
            salePrice = 500.0,
            buyingPrice = 150.0,
            commissionRate = 20.0,
            shippingCost = 35.0,
            rules = testRules
        )

        // Commission = 500 * 0.20 = 100.0
        assertEquals(100.0, result.commissionAmount, 0.01)
        // Tax = 500 * 0.10 = 50.0
        assertEquals(50.0, result.taxAmount, 0.01)
        // Return Buffer = 500 * 0.05 = 25.0
        assertEquals(25.0, result.returnBufferAmount, 0.01)
        // Other Expenses = 10.0
        assertEquals(10.0, result.otherExpenses, 0.01)
        // Total Expenses = 150 + 100 + 35 + 50 + 25 + 10 = 370.0
        assertEquals(370.0, result.totalExpenses, 0.01)
        // Net Profit = 500 - 370 = 130.0
        assertEquals(130.0, result.netProfit, 0.01)
        // Margin = (130 / 500) * 100 = 26.0%
        assertEquals(26.0, result.profitMarginPercent, 0.01)
        assertTrue(result.isProfitable)
    }

    @Test
    fun testUnprofitableCalculation() {
        val result = ProfitCalculatorEngine.calculate(
            salePrice = 200.0,
            buyingPrice = 160.0,
            commissionRate = 20.0,
            shippingCost = 35.0,
            rules = testRules
        )

        // Total Expenses = 160 + 40 (comm) + 35 (ship) + 20 (tax) + 10 (return) + 10 (other) = 275.0
        // Net Profit = 200 - 275 = -75.0
        assertTrue(result.netProfit < 0)
        assertFalse(result.isProfitable)
    }

    @Test
    fun testZeroSalePriceSafety() {
        val result = ProfitCalculatorEngine.calculate(
            salePrice = 0.0,
            buyingPrice = 50.0,
            commissionRate = 20.0,
            shippingCost = 35.0,
            rules = testRules
        )

        assertEquals(0.0, result.profitMarginPercent, 0.01)
        assertFalse(result.isProfitable)
    }
}
