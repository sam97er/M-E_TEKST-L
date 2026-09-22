package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuditLogItemDto(
    @Json(name = "id") val id: Int,
    @Json(name = "event_type") val eventType: String,
    @Json(name = "entity_name") val entityName: String,
    @Json(name = "entity_id") val entityId: String? = null,
    @Json(name = "action") val action: String,
    @Json(name = "details") val details: String? = null,
    @Json(name = "actor") val actor: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class AuditLogListResponseDto(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "logs") val logs: List<AuditLogItemDto> = emptyList()
)
