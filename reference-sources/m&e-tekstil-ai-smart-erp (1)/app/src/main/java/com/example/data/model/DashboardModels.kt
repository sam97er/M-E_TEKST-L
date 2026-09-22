package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemServiceStatusDto(
    @Json(name = "service_name") val serviceName: String = "",
    @Json(name = "is_operational") val isOperational: Boolean = false,
    @Json(name = "status_label") val statusLabel: String = "",
    @Json(name = "details") val details: String? = null
)

@JsonClass(generateAdapter = true)
data class SystemPulseDto(
    @Json(name = "overall_health") val overallHealth: String = "HEALTHY",
    @Json(name = "active_ai_provider") val activeAiProvider: String = "Gemini",
    @Json(name = "ai_model_name") val aiModelName: String = "gemini-1.5-flash",
    @Json(name = "ai_is_active") val aiIsActive: Boolean = true,
    @Json(name = "trendyol_connected") val trendyolConnected: Boolean = false,
    @Json(name = "trendyol_last_sync") val trendyolLastSync: String? = null,
    @Json(name = "telegram_bot_active") val telegramBotActive: Boolean = false,
    @Json(name = "telegram_pending_queue_count") val telegramPendingQueueCount: Int = 0,
    @Json(name = "services") val services: List<SystemServiceStatusDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SalesKpisDto(
    @Json(name = "today_orders_count") val todayOrdersCount: Int = 0,
    @Json(name = "today_gross_revenue_tl") val todayGrossRevenueTl: Double = 0.0,
    @Json(name = "today_net_profit_tl") val todayNetProfitTl: Double = 0.0,
    @Json(name = "today_profit_margin_percent") val todayProfitMarginPercent: Double = 0.0,
    @Json(name = "seven_day_orders_count") val sevenDayOrdersCount: Int = 0,
    @Json(name = "seven_day_gross_revenue_tl") val sevenDayGrossRevenueTl: Double = 0.0,
    @Json(name = "seven_day_net_profit_tl") val sevenDayNetProfitTl: Double = 0.0,
    @Json(name = "seven_day_profit_margin_percent") val sevenDayProfitMarginPercent: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class QuestionKpisDto(
    @Json(name = "unanswered_count") val unansweredCount: Int = 0,
    @Json(name = "draft_ready_for_approval_count") val draftReadyForApprovalCount: Int = 0,
    @Json(name = "sent_today_count") val sentTodayCount: Int = 0,
    @Json(name = "avg_response_time_minutes") val avgResponseTimeMinutes: Int = 0
)

@JsonClass(generateAdapter = true)
data class InventoryKpisDto(
    @Json(name = "total_active_products") val totalActiveProducts: Int = 0,
    @Json(name = "total_variant_skus") val totalVariantSkus: Int = 0,
    @Json(name = "total_physical_units") val totalPhysicalUnits: Int = 0,
    @Json(name = "out_of_stock_skus_count") val outOfStockSkusCount: Int = 0,
    @Json(name = "low_stock_skus_count") val lowStockSkusCount: Int = 0,
    @Json(name = "stagnant_tied_capital_tl") val stagnantTiedCapitalTl: Double = 0.0,
    @Json(name = "stagnant_units_count") val stagnantUnitsCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class DashboardAlertItemDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "alert_type") val alertType: String = "INFO",
    @Json(name = "title") val title: String = "",
    @Json(name = "message") val message: String = "",
    @Json(name = "action_target_section") val actionTargetSection: String = "",
    @Json(name = "action_label") val actionLabel: String = ""
)

@JsonClass(generateAdapter = true)
data class ActivityFeedItemDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "activity_type") val activityType: String = "ORDER",
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String = "",
    @Json(name = "timestamp") val timestamp: String = "",
    @Json(name = "icon_hint") val iconHint: String = "order",
    @Json(name = "status_tag") val statusTag: String? = null
)

@JsonClass(generateAdapter = true)
data class DashboardSummaryResponseDto(
    @Json(name = "generated_at") val generatedAt: String = "",
    @Json(name = "system_pulse") val systemPulse: SystemPulseDto = SystemPulseDto(),
    @Json(name = "sales_kpis") val salesKpis: SalesKpisDto = SalesKpisDto(),
    @Json(name = "question_kpis") val questionKpis: QuestionKpisDto = QuestionKpisDto(),
    @Json(name = "inventory_kpis") val inventoryKpis: InventoryKpisDto = InventoryKpisDto(),
    @Json(name = "critical_alerts") val criticalAlerts: List<DashboardAlertItemDto> = emptyList(),
    @Json(name = "recent_activities") val recentActivities: List<ActivityFeedItemDto> = emptyList()
)
