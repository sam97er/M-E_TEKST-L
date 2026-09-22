package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AIProviderSlotDto(
    @Json(name = "slot") val slot: Int,
    @Json(name = "provider_name") val providerName: String,
    @Json(name = "model") val model: String,
    @Json(name = "is_enabled") val isEnabled: Boolean,
    @Json(name = "base_url") val baseUrl: String?,
    @Json(name = "assigned_tasks") val assignedTasks: List<String>,
    @Json(name = "timeout_seconds") val timeoutSeconds: Int,
    @Json(name = "masked_key") val maskedKey: String,
    @Json(name = "key_configured") val keyConfigured: Boolean,
    @Json(name = "last_health_status") val lastHealthStatus: String
)

@JsonClass(generateAdapter = true)
data class AIProviderConfigUpdateDto(
    @Json(name = "provider_name") val providerName: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "is_enabled") val isEnabled: Boolean? = null,
    @Json(name = "base_url") val baseUrl: String? = null,
    @Json(name = "assigned_tasks") val assignedTasks: List<String>? = null,
    @Json(name = "timeout_seconds") val timeoutSeconds: Int? = null,
    @Json(name = "api_key") val apiKey: String? = null
)

@JsonClass(generateAdapter = true)
data class TestConnectionResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "provider") val provider: String,
    @Json(name = "model") val model: String,
    @Json(name = "reply") val reply: String?,
    @Json(name = "error") val error: String?,
    @Json(name = "latency_ms") val latencyMs: Double
)

@JsonClass(generateAdapter = true)
data class GeneralSettingsDto(
    @Json(name = "app_name") val appName: String,
    @Json(name = "app_env") val appEnv: String,
    @Json(name = "database_url") val databaseUrl: String,
    @Json(name = "trendyol_configured") val trendyolConfigured: Boolean,
    @Json(name = "trendyol_supplier_id") val trendyolSupplierId: String?,
    @Json(name = "telegram_enabled") val telegramEnabled: Boolean,
    @Json(name = "telegram_configured") val telegramConfigured: Boolean,
    @Json(name = "active_ai_slots_count") val activeAiSlotsCount: Int
)

@JsonClass(generateAdapter = true)
data class SystemHealthDto(
    @Json(name = "status") val status: String,
    @Json(name = "app") val app: String,
    @Json(name = "version") val version: String,
    @Json(name = "database_status") val databaseStatus: String,
    @Json(name = "db_latency_ms") val dbLatencyMs: Double,
    @Json(name = "trendyol_configured") val trendyolConfigured: Boolean,
    @Json(name = "telegram_configured") val telegramConfigured: Boolean,
    @Json(name = "active_ai_providers") val activeAiProviders: Int
)

@JsonClass(generateAdapter = true)
data class StatusResponseDto(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null
)

