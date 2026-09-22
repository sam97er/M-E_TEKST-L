package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OrderItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "order_id") val orderId: Int,
    @Json(name = "variant_id") val variantId: Int? = null,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "product_name") val productName: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "unit_price") val unitPrice: Double,
    @Json(name = "unit_purchase_cost") val unitPurchaseCost: Double,
    @Json(name = "commission_rate") val commissionRate: Double,
    @Json(name = "vat_rate") val vatRate: Double,
    @Json(name = "total_price") val totalPrice: Double = 0.0,
    @Json(name = "total_cost") val totalCost: Double = 0.0,
    @Json(name = "item_gross_profit") val itemGrossProfit: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class ProfitBreakdownDto(
    @Json(name = "gross_amount") val grossAmount: Double,
    @Json(name = "discount_amount") val discountAmount: Double,
    @Json(name = "net_sales_amount") val netSalesAmount: Double,
    @Json(name = "product_purchase_cost") val productPurchaseCost: Double,
    @Json(name = "trendyol_commission_amount") val trendyolCommissionAmount: Double,
    @Json(name = "trendyol_commission_rate_avg") val trendyolCommissionRateAvg: Double,
    @Json(name = "shipping_cost") val shippingCost: Double,
    @Json(name = "service_fee") val serviceFee: Double,
    @Json(name = "tax_amount") val taxAmount: Double,
    @Json(name = "packaging_and_handling") val packagingAndHandling: Double,
    @Json(name = "total_expenses") val totalExpenses: Double,
    @Json(name = "estimated_net_profit") val estimatedNetProfit: Double,
    @Json(name = "realized_net_profit") val realizedNetProfit: Double? = null,
    @Json(name = "profit_margin_percent") val profitMarginPercent: Double,
    @Json(name = "return_on_cost_percent") val returnOnCostPercent: Double,
    @Json(name = "is_profitable") val isProfitable: Boolean
)

@JsonClass(generateAdapter = true)
data class OrderDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "trendyol_order_number") val trendyolOrderNumber: String,
    @Json(name = "package_id") val packageId: String? = null,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "city") val city: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "order_date") val orderDate: String? = null,
    @Json(name = "item_count") val itemCount: Int = 0,
    @Json(name = "breakdown") val breakdown: ProfitBreakdownDto,
    @Json(name = "items") val items: List<OrderItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OrderSummaryItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "trendyol_order_number") val trendyolOrderNumber: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "city") val city: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "order_date") val orderDate: String? = null,
    @Json(name = "item_count") val itemCount: Int,
    @Json(name = "net_amount") val netAmount: Double,
    @Json(name = "estimated_profit") val estimatedProfit: Double,
    @Json(name = "realized_profit") val realizedProfit: Double? = null,
    @Json(name = "profit_margin_percent") val profitMarginPercent: Double
)

@JsonClass(generateAdapter = true)
data class SalesSummaryDto(
    @Json(name = "total_orders_count") val totalOrdersCount: Int = 0,
    @Json(name = "delivered_orders_count") val deliveredOrdersCount: Int = 0,
    @Json(name = "cancelled_or_returned_count") val cancelledOrReturnedCount: Int = 0,
    @Json(name = "total_gross_revenue") val totalGrossRevenue: Double = 0.0,
    @Json(name = "total_net_sales") val totalNetSales: Double = 0.0,
    @Json(name = "total_product_cogs") val totalProductCogs: Double = 0.0,
    @Json(name = "total_commission_paid") val totalCommissionPaid: Double = 0.0,
    @Json(name = "total_shipping_paid") val totalShippingPaid: Double = 0.0,
    @Json(name = "total_tax_paid") val totalTaxPaid: Double = 0.0,
    @Json(name = "total_other_expenses") val totalOtherExpenses: Double = 0.0,
    @Json(name = "total_estimated_net_profit") val totalEstimatedNetProfit: Double = 0.0,
    @Json(name = "total_realized_net_profit") val totalRealizedNetProfit: Double = 0.0,
    @Json(name = "overall_profit_margin_percent") val overallProfitMarginPercent: Double = 0.0,
    @Json(name = "average_order_value") val averageOrderValue: Double = 0.0,
    @Json(name = "return_rate_percent") val returnRatePercent: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class DailySalesPointDto(
    @Json(name = "date") val date: String,
    @Json(name = "order_count") val orderCount: Int,
    @Json(name = "gross_revenue") val grossRevenue: Double,
    @Json(name = "net_profit") val netProfit: Double,
    @Json(name = "profit_margin_percent") val profitMarginPercent: Double
)

@JsonClass(generateAdapter = true)
data class ProductProfitRankingDto(
    @Json(name = "product_id") val productId: Int? = null,
    @Json(name = "product_name") val productName: String,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "total_units_sold") val totalUnitsSold: Int,
    @Json(name = "total_revenue") val totalRevenue: Double,
    @Json(name = "total_cost") val totalCost: Double,
    @Json(name = "total_profit") val totalProfit: Double,
    @Json(name = "profit_margin_percent") val profitMarginPercent: Double,
    @Json(name = "is_low_margin") val isLowMargin: Boolean
)

@JsonClass(generateAdapter = true)
data class ProfitSimulationRequest(
    @Json(name = "sale_price") val salePrice: Double,
    @Json(name = "purchase_cost") val purchaseCost: Double,
    @Json(name = "commission_rate_percent") val commissionRatePercent: Double = 20.0,
    @Json(name = "shipping_cost") val shippingCost: Double = 38.50,
    @Json(name = "packaging_cost") val packagingCost: Double = 12.00,
    @Json(name = "vat_rate_percent") val vatRatePercent: Double = 10.0,
    @Json(name = "service_fee") val serviceFee: Double = 8.49
)

@JsonClass(generateAdapter = true)
data class ProfitSimulationResponse(
    @Json(name = "sale_price") val salePrice: Double,
    @Json(name = "purchase_cost") val purchaseCost: Double,
    @Json(name = "commission_amount") val commissionAmount: Double,
    @Json(name = "shipping_cost") val shippingCost: Double,
    @Json(name = "service_fee") val serviceFee: Double,
    @Json(name = "tax_amount") val taxAmount: Double,
    @Json(name = "packaging_cost") val packagingCost: Double,
    @Json(name = "total_cost") val totalCost: Double,
    @Json(name = "net_profit") val netProfit: Double,
    @Json(name = "profit_margin_percent") val profitMarginPercent: Double,
    @Json(name = "roi_percent") val roiPercent: Double,
    @Json(name = "breakeven_price") val breakevenPrice: Double,
    @Json(name = "target_price_for_20_percent_margin") val targetPriceFor20PercentMargin: Double,
    @Json(name = "is_profitable") val isProfitable: Boolean,
    @Json(name = "recommendation") val recommendation: String
)

@JsonClass(generateAdapter = true)
data class OrderStatusUpdateRequest(
    @Json(name = "status") val status: String,
    @Json(name = "custom_realized_profit") val customRealizedProfit: Double? = null
)
