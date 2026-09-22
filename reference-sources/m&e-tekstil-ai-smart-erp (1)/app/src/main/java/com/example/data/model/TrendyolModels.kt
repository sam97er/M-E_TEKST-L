package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TrendyolConfigDto(
    @Json(name = "configured") val configured: Boolean,
    @Json(name = "supplier_id") val supplierId: String?,
    @Json(name = "base_url") val baseUrl: String,
    @Json(name = "api_key_masked") val apiKeyMasked: String,
    @Json(name = "api_secret_masked") val apiSecretMasked: String,
    @Json(name = "mock_mode") val mockMode: Boolean,
    @Json(name = "rate_limit_remaining") val rateLimitRemaining: Int,
    @Json(name = "rate_limit_window_seconds") val rateLimitWindowSeconds: Int
)

@JsonClass(generateAdapter = true)
data class TrendyolConfigUpdateRequest(
    @Json(name = "supplier_id") val supplierId: String,
    @Json(name = "api_key") val apiKey: String,
    @Json(name = "api_secret") val apiSecret: String,
    @Json(name = "mock_mode") val mockMode: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TrendyolTestConnectionDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "status_code") val statusCode: Int,
    @Json(name = "message") val message: String,
    @Json(name = "supplier_id") val supplierId: String?,
    @Json(name = "latency_ms") val latencyMs: Double,
    @Json(name = "is_mock") val isMock: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TrendyolSyncRequest(
    @Json(name = "sync_type") val syncType: String = "ALL",
    @Json(name = "force_mock") val forceMock: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TrendyolSyncResponseDto(
    @Json(name = "sync_id") val syncId: Int,
    @Json(name = "sync_type") val syncType: String,
    @Json(name = "status") val status: String,
    @Json(name = "items_processed") val itemsProcessed: Int,
    @Json(name = "orders_synced") val ordersSynced: Int,
    @Json(name = "questions_synced") val questionsSynced: Int,
    @Json(name = "products_synced") val productsSynced: Int,
    @Json(name = "duration_ms") val durationMs: Double,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class TrendyolDashboardDto(
    @Json(name = "is_connected") val isConnected: Boolean,
    @Json(name = "supplier_id") val supplierId: String?,
    @Json(name = "mock_mode") val mockMode: Boolean,
    @Json(name = "last_sync_time") val lastSyncTime: String?,
    @Json(name = "last_sync_status") val lastSyncStatus: String?,
    @Json(name = "total_orders_synced") val totalOrdersSynced: Int,
    @Json(name = "total_questions_synced") val totalQuestionsSynced: Int,
    @Json(name = "active_trendyol_products_count") val activeTrendyolProductsCount: Int,
    @Json(name = "rate_limit_info") val rateLimitInfo: String
)

@JsonClass(generateAdapter = true)
data class TrendyolOrderSummaryDto(
    @Json(name = "order_number") val orderNumber: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "city") val city: String,
    @Json(name = "status") val status: String,
    @Json(name = "total_gross_amount") val totalGrossAmount: Double,
    @Json(name = "net_amount") val netAmount: Double,
    @Json(name = "estimated_profit") val estimatedProfit: Double,
    @Json(name = "order_date") val orderDate: String,
    @Json(name = "item_count") val itemCount: Int
)

@JsonClass(generateAdapter = true)
data class TrendyolQuestionSummaryDto(
    @Json(name = "question_id") val questionId: String,
    @Json(name = "product_title") val productTitle: String,
    @Json(name = "customer_name") val customerName: String,
    @Json(name = "question_text") val questionText: String,
    @Json(name = "status") val status: String,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class TrendyolStockPriceItemDto(
    @Json(name = "barcode") val barcode: String,
    @Json(name = "quantity") val quantity: Int,
    @Json(name = "sale_price") val salePrice: Double,
    @Json(name = "list_price") val listPrice: Double? = null
)

@JsonClass(generateAdapter = true)
data class TrendyolStockPriceBatchRequest(
    @Json(name = "items") val items: List<TrendyolStockPriceItemDto>
)

@JsonClass(generateAdapter = true)
data class TrendyolBatchResultDto(
    @Json(name = "batch_request_id") val batchRequestId: String,
    @Json(name = "status") val status: String,
    @Json(name = "item_count") val itemCount: Int,
    @Json(name = "message") val message: String
)
