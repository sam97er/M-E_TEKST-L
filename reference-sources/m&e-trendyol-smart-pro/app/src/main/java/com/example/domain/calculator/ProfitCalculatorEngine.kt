package com.example.domain.calculator

import com.example.security.ProfitCalculationRules
import java.util.Locale

data class ProfitCalculationResult(
    val salePrice: Double,
    val buyingPrice: Double,
    val commissionRate: Double,
    val commissionAmount: Double,
    val shippingCost: Double,
    val taxAmount: Double,
    val returnBufferAmount: Double,
    val otherExpenses: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val profitMarginPercent: Double,
    val isProfitable: Boolean,
    val explanation: String
)

object ProfitCalculatorEngine {

    fun calculate(
        salePrice: Double,
        buyingPrice: Double,
        commissionRate: Double,
        shippingCost: Double,
        rules: ProfitCalculationRules
    ): ProfitCalculationResult {
        val safeSalePrice = if (salePrice < 0) 0.0 else salePrice
        val safeBuyingPrice = if (buyingPrice < 0) 0.0 else buyingPrice
        val safeCommissionRate = if (commissionRate < 0) 0.0 else commissionRate

        val commissionAmount = safeSalePrice * (safeCommissionRate / 100.0)
        val taxAmount = safeSalePrice * (rules.defaultTaxRate / 100.0)
        val returnBufferAmount = safeSalePrice * (rules.returnExpenseBufferPercent / 100.0)
        val otherExpenses = rules.packagingAndOtherCost

        val totalExpenses = safeBuyingPrice + commissionAmount + shippingCost + taxAmount + returnBufferAmount + otherExpenses
        val netProfit = safeSalePrice - totalExpenses
        val margin = if (safeSalePrice > 0) (netProfit / safeSalePrice) * 100.0 else 0.0

        val formattedProfit = String.format(Locale.US, "%.2f", netProfit)
        val formattedMargin = String.format(Locale.US, "%.1f", margin)

        val explanation = buildString {
            append("Satış: ${String.format(Locale.US, "%.2f", safeSalePrice)} ₺ | ")
            append("Maliyet: ${String.format(Locale.US, "%.2f", safeBuyingPrice)} ₺ | ")
            append("Komisyon (%$safeCommissionRate): ${String.format(Locale.US, "%.2f", commissionAmount)} ₺ | ")
            append("Kargo: ${String.format(Locale.US, "%.2f", shippingCost)} ₺ | ")
            append("KDV/Stopaj: ${String.format(Locale.US, "%.2f", taxAmount)} ₺ | ")
            append("İade Payı: ${String.format(Locale.US, "%.2f", returnBufferAmount)} ₺ | ")
            append("Paketleme: ${String.format(Locale.US, "%.2f", otherExpenses)} ₺ -> ")
            append("Net Kâr: $formattedProfit ₺ (%$formattedMargin)")
        }

        return ProfitCalculationResult(
            salePrice = safeSalePrice,
            buyingPrice = safeBuyingPrice,
            commissionRate = safeCommissionRate,
            commissionAmount = commissionAmount,
            shippingCost = shippingCost,
            taxAmount = taxAmount,
            returnBufferAmount = returnBufferAmount,
            otherExpenses = otherExpenses,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMarginPercent = margin,
            isProfitable = netProfit > 0,
            explanation = explanation
        )
    }
}
