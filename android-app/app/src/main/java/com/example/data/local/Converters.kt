package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.AiApprovalStatus
import com.example.domain.model.AiTaskType
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.ResponseStyle
import com.example.domain.model.TrendyolOrderStatus

class AppTypeConverters {
    @TypeConverter
    fun fromOrderPrepStatus(value: OrderPrepStatus?): String = value?.name ?: OrderPrepStatus.WAITING.name

    @TypeConverter
    fun toOrderPrepStatus(value: String?): OrderPrepStatus =
        value?.let { runCatching { OrderPrepStatus.valueOf(it) }.getOrNull() } ?: OrderPrepStatus.WAITING

    @TypeConverter
    fun fromTrendyolOrderStatus(value: TrendyolOrderStatus?): String = value?.name ?: TrendyolOrderStatus.NEW.name

    @TypeConverter
    fun toTrendyolOrderStatus(value: String?): TrendyolOrderStatus =
        value?.let { runCatching { TrendyolOrderStatus.valueOf(it) }.getOrNull() } ?: TrendyolOrderStatus.NEW

    @TypeConverter
    fun fromAiTaskType(value: AiTaskType?): String = value?.name ?: AiTaskType.CUSTOMER_REPLY.name

    @TypeConverter
    fun toAiTaskType(value: String?): AiTaskType =
        value?.let { runCatching { AiTaskType.valueOf(it) }.getOrNull() } ?: AiTaskType.CUSTOMER_REPLY

    @TypeConverter
    fun fromAiApprovalStatus(value: AiApprovalStatus?): String = value?.name ?: AiApprovalStatus.BEKLIYOR.name

    @TypeConverter
    fun toAiApprovalStatus(value: String?): AiApprovalStatus =
        value?.let { runCatching { AiApprovalStatus.valueOf(it) }.getOrNull() } ?: AiApprovalStatus.BEKLIYOR

    @TypeConverter
    fun fromResponseStyle(value: ResponseStyle?): String = value?.name ?: ResponseStyle.FORMAL.name

    @TypeConverter
    fun toResponseStyle(value: String?): ResponseStyle =
        value?.let { runCatching { ResponseStyle.valueOf(it) }.getOrNull() } ?: ResponseStyle.FORMAL
}
