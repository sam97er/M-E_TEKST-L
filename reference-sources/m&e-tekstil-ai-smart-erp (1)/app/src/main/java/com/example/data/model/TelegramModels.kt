package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TelegramConfigDto(
    @Json(name = "enabled") val enabled: Boolean = false,
    @Json(name = "bot_token_masked") val botTokenMasked: String = "Yapılandırılmamış",
    @Json(name = "chat_id") val chatId: String? = null,
    @Json(name = "is_configured") val isConfigured: Boolean = false,
    @Json(name = "notify_new_order") val notifyNewOrder: Boolean = true,
    @Json(name = "notify_low_stock") val notifyLowStock: Boolean = true,
    @Json(name = "notify_new_question") val notifyNewQuestion: Boolean = true,
    @Json(name = "notify_daily_digest") val notifyDailyDigest: Boolean = true,
    @Json(name = "notify_system_error") val notifySystemError: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TelegramConfigUpdateRequest(
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "bot_token") val botToken: String? = null,
    @Json(name = "chat_id") val chatId: String? = null,
    @Json(name = "notify_new_order") val notifyNewOrder: Boolean? = null,
    @Json(name = "notify_low_stock") val notifyLowStock: Boolean? = null,
    @Json(name = "notify_new_question") val notifyNewQuestion: Boolean? = null,
    @Json(name = "notify_daily_digest") val notifyDailyDigest: Boolean? = null,
    @Json(name = "notify_system_error") val notifySystemError: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class TelegramEventItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "message") val message: String,
    @Json(name = "status") val status: String,
    @Json(name = "retry_count") val retryCount: Int,
    @Json(name = "sent_at") val sentAt: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class TelegramEventsListResponseDto(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "pending_count") val pendingCount: Int = 0,
    @Json(name = "failed_count") val failedCount: Int = 0,
    @Json(name = "sent_count") val sentCount: Int = 0,
    @Json(name = "events") val events: List<TelegramEventItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TelegramSendTestRequest(
    @Json(name = "message") val message: String? = null,
    @Json(name = "event_type") val eventType: String = "SYSTEM_TEST"
)

@JsonClass(generateAdapter = true)
data class TelegramSendTestResponseDto(
    @Json(name = "success") val success: Boolean,
    @Json(name = "event_id") val eventId: Int,
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramRetryResponseDto(
    @Json(name = "retried_count") val retriedCount: Int,
    @Json(name = "success_count") val successCount: Int,
    @Json(name = "failed_count") val failedCount: Int,
    @Json(name = "message") val message: String
)
