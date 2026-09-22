package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.calculator.ProfitCalculatorEngine
import com.example.security.ProfitCalculationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ME Tekstil Aİ", appName)
    }

    @Test
    fun `test profit calculation engine`() {
        val rules = ProfitCalculationRules(
            defaultCommissionRate = 18.5,
            defaultShippingCost = 35.0,
            defaultTaxRate = 10.0,
            returnExpenseBufferPercent = 3.0,
            packagingAndOtherCost = 6.5
        )

        val result = ProfitCalculatorEngine.calculate(
            salePrice = 299.90,
            buyingPrice = 110.00,
            commissionRate = 18.5,
            shippingCost = 35.0,
            rules = rules
        )

        assertTrue("Calculation should result in positive net profit", result.isProfitable)
        assertTrue("Net profit margin should be positive", result.profitMarginPercent > 10.0)
    }
}
