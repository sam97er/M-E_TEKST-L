package com.example.data.remote

import com.example.data.model.AIProviderSlotDto
import com.example.data.model.BarcodeGenerateResponseDto
import com.example.data.model.GeneralSettingsDto
import com.example.data.model.ProductCreateRequest
import com.example.data.model.ProductDto
import com.example.data.model.SystemHealthDto
import com.example.data.model.TestConnectionResponseDto
import com.example.data.model.TrendyolTestConnectionDto
import com.example.data.model.TelegramSendTestResponseDto
import com.example.data.model.StatusResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/v1/health")
    suspend fun getHealth(): Response<SystemHealthDto>

    @GET("api/v1/settings")
    suspend fun getGeneralSettings(): Response<GeneralSettingsDto>

    @GET("api/v1/ai/providers")
    suspend fun getAiProviders(): Response<List<AIProviderSlotDto>>

    @GET("api/v1/ai/providers/{slot}")
    suspend fun getAiProviderSlot(@Path("slot") slot: Int): Response<AIProviderSlotDto>

    @retrofit2.http.PUT("api/v1/ai/providers/{slot}")
    suspend fun updateAiProvider(
        @Path("slot") slot: Int,
        @Body request: com.example.data.model.AIProviderConfigUpdateDto
    ): Response<AIProviderSlotDto>

    @POST("api/v1/ai/providers/{slot}/test")
    suspend fun testProviderConnection(@Path("slot") slot: Int): Response<TestConnectionResponseDto>

    @POST("api/v1/settings/trendyol/test")
    suspend fun testTrendyolSettings(): Response<TrendyolTestConnectionDto>

    @POST("api/v1/settings/telegram/test")
    suspend fun testTelegramSettings(): Response<TelegramSendTestResponseDto>

    // Products & Variants (Phase 3)
    @GET("api/v1/products")
    suspend fun getProducts(
        @Query("search") search: String? = null,
        @Query("category") category: String? = null,
        @Query("status") status: String? = null
    ): Response<List<ProductDto>>

    @GET("api/v1/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Response<ProductDto>

    @POST("api/v1/products")
    suspend fun createProduct(@Body request: ProductCreateRequest): Response<ProductDto>

    @DELETE("api/v1/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int): Response<StatusResponseDto>

    @GET("api/v1/products/categories")
    suspend fun getCategories(): Response<List<String>>

    @GET("api/v1/products/generate-barcode")
    suspend fun generateBarcode(@Query("prefix") prefix: String = "868"): Response<BarcodeGenerateResponseDto>

    // Trendyol Integration (Phase 4)
    @GET("api/v1/trendyol/config")
    suspend fun getTrendyolConfig(): Response<com.example.data.model.TrendyolConfigDto>

    @POST("api/v1/trendyol/config")
    suspend fun updateTrendyolConfig(
        @Body request: com.example.data.model.TrendyolConfigUpdateRequest
    ): Response<com.example.data.model.TrendyolConfigDto>

    @POST("api/v1/trendyol/test")
    suspend fun testTrendyolApi(): Response<com.example.data.model.TrendyolTestConnectionDto>

    @POST("api/v1/trendyol/sync")
    suspend fun triggerTrendyolSync(
        @Body request: com.example.data.model.TrendyolSyncRequest
    ): Response<com.example.data.model.TrendyolSyncResponseDto>

    @GET("api/v1/trendyol/dashboard")
    suspend fun getTrendyolDashboard(): Response<com.example.data.model.TrendyolDashboardDto>

    @GET("api/v1/trendyol/orders")
    suspend fun getTrendyolOrders(
        @Query("limit") limit: Int = 50
    ): Response<List<com.example.data.model.TrendyolOrderSummaryDto>>

    @GET("api/v1/trendyol/questions")
    suspend fun getTrendyolQuestions(
        @Query("limit") limit: Int = 50
    ): Response<List<com.example.data.model.TrendyolQuestionSummaryDto>>

    @POST("api/v1/trendyol/stock-price")
    suspend fun updateTrendyolStockPrice(
        @Body request: com.example.data.model.TrendyolStockPriceBatchRequest
    ): Response<com.example.data.model.TrendyolBatchResultDto>

    // Customer Questions & AI Reply Workflow (Phase 5)
    @GET("api/v1/questions")
    suspend fun getQuestions(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<List<com.example.data.model.CustomerQuestionDto>>

    @GET("api/v1/questions/summary")
    suspend fun getQuestionsSummary(): Response<com.example.data.model.CustomerQuestionsSummaryDto>

    @POST("api/v1/questions/sync")
    suspend fun syncCustomerQuestions(): Response<com.example.data.model.QuestionActionResponseDto>

    @GET("api/v1/questions/{id}")
    suspend fun getQuestionDetail(@Path("id") questionId: Int): Response<com.example.data.model.CustomerQuestionDto>

    @POST("api/v1/questions/{id}/draft")
    suspend fun generateAiDraft(
        @Path("id") questionId: Int,
        @Body request: com.example.data.model.GenerateDraftRequest
    ): Response<com.example.data.model.AIDraftDto>

    @retrofit2.http.PUT("api/v1/questions/drafts/{draft_id}")
    suspend fun editDraft(
        @Path("draft_id") draftId: Int,
        @Body request: com.example.data.model.EditDraftRequest
    ): Response<com.example.data.model.AIDraftDto>

    @POST("api/v1/questions/drafts/{draft_id}/approve")
    suspend fun approveDraft(
        @Path("draft_id") draftId: Int,
        @Body request: com.example.data.model.ApproveDraftRequest
    ): Response<com.example.data.model.AIDraftDto>

    @POST("api/v1/questions/{id}/send")
    suspend fun sendApprovedReply(
        @Path("id") questionId: Int,
        @Body request: com.example.data.model.SendReplyRequest
    ): Response<com.example.data.model.QuestionActionResponseDto>

    @POST("api/v1/questions/drafts/{draft_id}/reject")
    suspend fun rejectDraft(
        @Path("draft_id") draftId: Int,
        @Body request: com.example.data.model.RejectDraftRequest
    ): Response<com.example.data.model.AIDraftDto>

    // Phase 6: Orders, Sales & Net Profit Analysis
    @GET("api/v1/orders")
    suspend fun getOrders(
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("sort_by") sortBy: String = "date_desc"
    ): Response<List<com.example.data.model.OrderSummaryItemDto>>

    @GET("api/v1/orders/summary")
    suspend fun getSalesSummary(): Response<com.example.data.model.SalesSummaryDto>

    @GET("api/v1/orders/analytics/daily")
    suspend fun getDailySalesAnalytics(
        @Query("days") days: Int = 7
    ): Response<List<com.example.data.model.DailySalesPointDto>>

    @GET("api/v1/orders/analytics/product-ranking")
    suspend fun getProductProfitRanking(
        @Query("limit") limit: Int = 20
    ): Response<List<com.example.data.model.ProductProfitRankingDto>>

    @POST("api/v1/orders/simulate")
    suspend fun simulatePricing(
        @Body request: com.example.data.model.ProfitSimulationRequest
    ): Response<com.example.data.model.ProfitSimulationResponse>

    @GET("api/v1/orders/{id}")
    suspend fun getOrderDetail(@Path("id") orderId: Int): Response<com.example.data.model.OrderDetailDto>

    @retrofit2.http.PUT("api/v1/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: Int,
        @Body request: com.example.data.model.OrderStatusUpdateRequest
    ): Response<com.example.data.model.OrderDetailDto>

    // Phase 7: Telegram Bot Notifications & Audit Logs
    @GET("api/v1/telegram/config")
    suspend fun getTelegramConfig(): Response<com.example.data.model.TelegramConfigDto>

    @retrofit2.http.PUT("api/v1/telegram/config")
    suspend fun updateTelegramConfig(
        @Body request: com.example.data.model.TelegramConfigUpdateRequest
    ): Response<com.example.data.model.TelegramConfigDto>

    @GET("api/v1/telegram/events")
    suspend fun getTelegramEvents(
        @Query("status") status: String? = null,
        @Query("event_type") eventType: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<com.example.data.model.TelegramEventsListResponseDto>

    @POST("api/v1/telegram/send-test")
    suspend fun sendTelegramTest(
        @Body request: com.example.data.model.TelegramSendTestRequest
    ): Response<com.example.data.model.TelegramSendTestResponseDto>

    @POST("api/v1/telegram/retry")
    suspend fun retryTelegramEvents(
        @Query("max_retries") maxRetries: Int = 5
    ): Response<com.example.data.model.TelegramRetryResponseDto>

    @POST("api/v1/telegram/send-daily-digest")
    suspend fun sendDailyDigestNow(): Response<com.example.data.model.TelegramSendTestResponseDto>

    @DELETE("api/v1/telegram/events")
    suspend fun clearTelegramEvents(
        @Query("status") status: String? = null
    ): Response<StatusResponseDto>

    @GET("api/v1/audit/logs")
    suspend fun getAuditLogs(
        @Query("event_type") eventType: String? = null,
        @Query("entity_name") entityName: String? = null,
        @Query("actor") actor: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<com.example.data.model.AuditLogListResponseDto>

    // Phase 8: AI Smart Reports, Listing Quality & Pricing Advisory
    @GET("api/v1/reports/stagnant-stock")
    suspend fun getStagnantStockReport(
        @Query("min_days") minDays: Int = 20
    ): Response<com.example.data.model.StagnantStockReportResponseDto>

    @GET("api/v1/reports/listing-audits")
    suspend fun getListingQualityAudits(): Response<com.example.data.model.ListingAuditsResponseDto>

    @POST("api/v1/reports/pricing-advice/{product_id}")
    suspend fun getPricingAdvice(
        @Path("product_id") productId: Int,
        @Body request: com.example.data.model.PricingAdviceRequestDto = com.example.data.model.PricingAdviceRequestDto()
    ): Response<com.example.data.model.PricingAdviceResponseDto>

    @POST("api/v1/reports/optimize-content/{product_id}")
    suspend fun optimizeProductContent(
        @Path("product_id") productId: Int,
        @Body request: com.example.data.model.ContentOptimizationRequestDto = com.example.data.model.ContentOptimizationRequestDto()
    ): Response<com.example.data.model.ContentOptimizationResponseDto>

    @GET("api/v1/reports/executive-summary")
    suspend fun getExecutiveSummary(): Response<com.example.data.model.ExecutiveReportResponseDto>

    // Phase 9: Consolidated Executive Dashboard
    @GET("api/v1/dashboard/summary")
    suspend fun getDashboardSummary(): Response<com.example.data.model.DashboardSummaryResponseDto>
}
