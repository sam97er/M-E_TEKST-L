package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StagnantProductItemDto(
    @Json(name = "product_id") val productId: Int,
    @Json(name = "variant_id") val variantId: Int,
    @Json(name = "product_code") val productCode: String,
    @Json(name = "title") val title: String,
    @Json(name = "color") val color: String?,
    @Json(name = "size") val size: String?,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "stock_quantity") val stockQuantity: Int,
    @Json(name = "purchase_cost") val purchaseCost: Double,
    @Json(name = "current_selling_price") val currentSellingPrice: Double,
    @Json(name = "tied_capital_tl") val tiedCapitalTl: Double,
    @Json(name = "days_without_sale") val daysWithoutSale: Int,
    @Json(name = "urgency_level") val urgencyLevel: String,
    @Json(name = "ai_recommendation") val aiRecommendation: String,
    @Json(name = "suggested_discount_percent") val suggestedDiscountPercent: Double,
    @Json(name = "suggested_clearance_price") val suggestedClearancePrice: Double,
    @Json(name = "projected_capital_recovered_tl") val projectedCapitalRecoveredTl: Double
)

@JsonClass(generateAdapter = true)
data class StagnantStockReportResponseDto(
    @Json(name = "total_stagnant_variants_count") val totalStagnantVariantsCount: Int = 0,
    @Json(name = "total_stagnant_units") val totalStagnantUnits: Int = 0,
    @Json(name = "total_tied_capital_tl") val totalTiedCapitalTl: Double = 0.0,
    @Json(name = "potential_cash_recovery_tl") val potentialCashRecoveryTl: Double = 0.0,
    @Json(name = "critical_items_count") val criticalItemsCount: Int = 0,
    @Json(name = "items") val items: List<StagnantProductItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QualityDeficiencyDto(
    @Json(name = "severity") val severity: String,
    @Json(name = "field") val field: String,
    @Json(name = "message") val message: String,
    @Json(name = "suggestion") val suggestion: String
)

@JsonClass(generateAdapter = true)
data class ProductQualityAuditItemDto(
    @Json(name = "product_id") val productId: Int,
    @Json(name = "product_code") val productCode: String,
    @Json(name = "title") val title: String,
    @Json(name = "category_name") val categoryName: String?,
    @Json(name = "quality_score") val qualityScore: Int,
    @Json(name = "grade") val grade: String,
    @Json(name = "title_quality_score") val titleQualityScore: Int,
    @Json(name = "description_quality_score") val descriptionQualityScore: Int,
    @Json(name = "image_quality_score") val imageQualityScore: Int,
    @Json(name = "has_primary_image") val hasPrimaryImage: Boolean,
    @Json(name = "images_count") val imagesCount: Int,
    @Json(name = "has_fabric_composition") val hasFabricComposition: Boolean,
    @Json(name = "has_washing_instructions") val hasWashingInstructions: Boolean,
    @Json(name = "has_size_chart_info") val hasSizeChartInfo: Boolean,
    @Json(name = "deficiencies") val deficiencies: List<QualityDeficiencyDto> = emptyList(),
    @Json(name = "ai_quick_fix_summary") val aiQuickFixSummary: String
)

@JsonClass(generateAdapter = true)
data class ListingAuditsResponseDto(
    @Json(name = "total_audited_products") val totalAuditedProducts: Int = 0,
    @Json(name = "average_quality_score") val averageQualityScore: Double = 0.0,
    @Json(name = "high_quality_count") val highQualityCount: Int = 0,
    @Json(name = "medium_quality_count") val mediumQualityCount: Int = 0,
    @Json(name = "needs_improvement_count") val needsImprovementCount: Int = 0,
    @Json(name = "products") val products: List<ProductQualityAuditItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PricingAdviceRequestDto(
    @Json(name = "target_margin_percent") val targetMarginPercent: Double = 25.0,
    @Json(name = "market_demand_level") val marketDemandLevel: String = "NORMAL"
)

@JsonClass(generateAdapter = true)
data class PricingAdviceResponseDto(
    @Json(name = "product_id") val productId: Int,
    @Json(name = "product_code") val productCode: String,
    @Json(name = "title") val title: String,
    @Json(name = "current_selling_price") val currentSellingPrice: Double,
    @Json(name = "unit_purchase_cost") val unitPurchaseCost: Double,
    @Json(name = "estimated_trendyol_commission_tl") val estimatedTrendyolCommissionTl: Double,
    @Json(name = "shipping_cost_tl") val shippingCostTl: Double,
    @Json(name = "tax_cost_tl") val taxCostTl: Double,
    @Json(name = "packaging_cost_tl") val packagingCostTl: Double,
    @Json(name = "current_estimated_net_profit_tl") val currentEstimatedNetProfitTl: Double,
    @Json(name = "current_profit_margin_percent") val currentProfitMarginPercent: Double,
    @Json(name = "break_even_minimum_price") val breakEvenMinimumPrice: Double,
    @Json(name = "suggested_optimal_price") val suggestedOptimalPrice: Double,
    @Json(name = "suggested_optimal_margin_percent") val suggestedOptimalMarginPercent: Double,
    @Json(name = "suggested_flash_deal_price") val suggestedFlashDealPrice: Double,
    @Json(name = "ai_pricing_strategy") val aiPricingStrategy: String,
    @Json(name = "reasoning") val reasoning: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ContentOptimizationRequestDto(
    @Json(name = "focus_keywords") val focusKeywords: List<String>? = null,
    @Json(name = "target_audience") val targetAudience: String = "Genç & Dinamik Günlük Giyim",
    @Json(name = "tone_of_voice") val toneOfVoice: String = "Trend, Şık ve Bilgilendirici"
)

@JsonClass(generateAdapter = true)
data class ContentOptimizationResponseDto(
    @Json(name = "product_id") val productId: Int,
    @Json(name = "product_code") val productCode: String,
    @Json(name = "original_title") val originalTitle: String,
    @Json(name = "original_description") val originalDescription: String?,
    @Json(name = "optimized_title") val optimizedTitle: String,
    @Json(name = "optimized_bullet_points") val optimizedBulletPoints: List<String> = emptyList(),
    @Json(name = "optimized_full_description") val optimizedFullDescription: String,
    @Json(name = "suggested_tags") val suggestedTags: List<String> = emptyList(),
    @Json(name = "seo_score_improvement") val seoScoreImprovement: String,
    @Json(name = "status") val status: String = "DRAFT"
)

@JsonClass(generateAdapter = true)
data class ExecutiveSummaryMetricDto(
    @Json(name = "title") val title: String,
    @Json(name = "value") val value: String,
    @Json(name = "trend") val trend: String,
    @Json(name = "status_type") val statusType: String
)

@JsonClass(generateAdapter = true)
data class ExecutiveReportResponseDto(
    @Json(name = "report_date") val reportDate: String,
    @Json(name = "generated_by_model") val generatedByModel: String,
    @Json(name = "overall_health_score") val overallHealthScore: Int,
    @Json(name = "headline") val headline: String,
    @Json(name = "executive_summary_text") val executiveSummaryText: String,
    @Json(name = "key_metrics") val keyMetrics: List<ExecutiveSummaryMetricDto> = emptyList(),
    @Json(name = "top_strengths") val topStrengths: List<String> = emptyList(),
    @Json(name = "critical_bottlenecks") val criticalBottlenecks: List<String> = emptyList(),
    @Json(name = "recommended_next_actions") val recommendedNextActions: List<String> = emptyList()
)
