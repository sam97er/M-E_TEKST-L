package com.example.security

import android.content.Context
import android.content.SharedPreferences

data class AiSlotConfiguration(
    val slotNumber: Int,
    val roleName: String,
    val providerName: String,
    val baseUrl: String,
    val apiKey: String,
    val modelName: String,
    val isEnabled: Boolean,
    val timeoutSeconds: Int = 30,
    val usageLimit: Int = 1000,
    val usageCount: Int = 0,
    val fallbackSlot: Int = 0 // 0 = no fallback
)

data class TrendyolApiConfiguration(
    val sellerId: String,
    val apiKey: String,
    val apiSecret: String,
    val isConnected: Boolean,
    val lastSyncTime: Long,
    val lastSyncError: String
)

data class TelegramConfiguration(
    val isEnabled: Boolean,
    val botToken: String,
    val chatId: String,
    val notifyOrders: Boolean = true,
    val notifyQuestions: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyReturns: Boolean = true,
    val notifyErrors: Boolean = true,
    val notifyDailyBrief: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 23,
    val quietHoursEnd: Int = 7
)

data class ProfitCalculationRules(
    val defaultCommissionRate: Double = 18.5, // %
    val defaultShippingCost: Double = 35.0, // TL
    val defaultTaxRate: Double = 10.0, // Tekstil KDV %
    val returnExpenseBufferPercent: Double = 3.0, // % İade maliyet payı
    val packagingAndOtherCost: Double = 6.5 // Paketleme/Etiket/Koli TL
)

class SecureConfigManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("me_tekstil_secure_vault", Context.MODE_PRIVATE)

    companion object {
        fun maskKey(key: String): String {
            if (key.isBlank()) return "Yapılandırılmamış"
            if (key.length <= 8) return "••••••••"
            return "${key.take(4)}••••••••${key.takeLast(4)}"
        }
    }

    // Trendyol Configuration
    fun getTrendyolConfig(): TrendyolApiConfiguration {
        val rawSellerId = prefs.getString("ty_seller_id", "") ?: ""
        val rawApiKey = prefs.getString("ty_api_key", "") ?: ""
        
        // Strip out old dummy credentials if present
        val sellerId = if (rawSellerId == "1304973") "" else rawSellerId
        val apiKey = if (rawApiKey == "iRpZjEtgR8Fxg2odLsIP") "" else rawApiKey

        return TrendyolApiConfiguration(
            sellerId = sellerId,
            apiKey = apiKey,
            apiSecret = prefs.getString("ty_api_secret", "") ?: "",
            isConnected = prefs.getBoolean("ty_is_connected", false),
            lastSyncTime = prefs.getLong("ty_last_sync", 0L),
            lastSyncError = prefs.getString("ty_last_error", "") ?: ""
        )
    }

    fun saveTrendyolConfig(config: TrendyolApiConfiguration) {
        prefs.edit()
            .putString("ty_seller_id", config.sellerId.trim())
            .putString("ty_api_key", config.apiKey.trim())
            .putString("ty_api_secret", config.apiSecret.trim())
            .putBoolean("ty_is_connected", config.isConnected)
            .putLong("ty_last_sync", config.lastSyncTime)
            .putString("ty_last_error", config.lastSyncError)
            .apply()
    }

    fun updateTrendyolSyncStatus(success: Boolean, errorMsg: String = "") {
        prefs.edit()
            .putBoolean("ty_is_connected", success)
            .putLong("ty_last_sync", if (success) System.currentTimeMillis() else prefs.getLong("ty_last_sync", 0L))
            .putString("ty_last_error", errorMsg)
            .apply()
    }

    // 3 Independent AI Provider Configurations
    fun getAiSlotConfig(slotNumber: Int): AiSlotConfiguration {
        val defaultRole = when (slotNumber) {
            1 -> "Müşteri İletişimi AI"
            2 -> "İş ve Kâr AI"
            else -> "Ürün ve Büyüme AI"
        }
        val defaultProvider = when (slotNumber) {
            1 -> "Google Gemini"
            2 -> "OpenAI / Uyumlu"
            else -> "Google Gemini"
        }
        val defaultModel = when (slotNumber) {
            1 -> "gemini-3.5-flash"
            2 -> "gpt-4o-mini"
            else -> "gemini-3.5-flash"
        }
        val defaultBaseUrl = when (slotNumber) {
            2 -> "https://api.openai.com/v1"
            else -> "https://generativelanguage.googleapis.com/v1beta"
        }

        return AiSlotConfiguration(
            slotNumber = slotNumber,
            roleName = prefs.getString("ai_role_$slotNumber", defaultRole) ?: defaultRole,
            providerName = prefs.getString("ai_provider_$slotNumber", defaultProvider) ?: defaultProvider,
            baseUrl = prefs.getString("ai_baseurl_$slotNumber", defaultBaseUrl) ?: defaultBaseUrl,
            apiKey = prefs.getString("ai_apikey_$slotNumber", "") ?: "",
            modelName = prefs.getString("ai_model_$slotNumber", defaultModel) ?: defaultModel,
            isEnabled = prefs.getBoolean("ai_enabled_$slotNumber", true),
            timeoutSeconds = prefs.getInt("ai_timeout_$slotNumber", 30),
            usageLimit = prefs.getInt("ai_limit_$slotNumber", 1000),
            usageCount = prefs.getInt("ai_count_$slotNumber", 0),
            fallbackSlot = prefs.getInt("ai_fallback_$slotNumber", if (slotNumber == 1) 2 else 1)
        )
    }

    fun saveAiSlotConfig(config: AiSlotConfiguration) {
        prefs.edit()
            .putString("ai_role_${config.slotNumber}", config.roleName)
            .putString("ai_provider_${config.slotNumber}", config.providerName)
            .putString("ai_baseurl_${config.slotNumber}", config.baseUrl.trim())
            .putString("ai_apikey_${config.slotNumber}", config.apiKey.trim())
            .putString("ai_model_${config.slotNumber}", config.modelName.trim())
            .putBoolean("ai_enabled_${config.slotNumber}", config.isEnabled)
            .putInt("ai_timeout_${config.slotNumber}", config.timeoutSeconds)
            .putInt("ai_limit_${config.slotNumber}", config.usageLimit)
            .putInt("ai_fallback_${config.slotNumber}", config.fallbackSlot)
            .apply()
    }

    fun incrementAiUsage(slotNumber: Int) {
        val current = prefs.getInt("ai_count_$slotNumber", 0)
        prefs.edit().putInt("ai_count_$slotNumber", current + 1).apply()
    }

    // Telegram Configuration
    fun getTelegramConfig(): TelegramConfiguration {
        return TelegramConfiguration(
            isEnabled = prefs.getBoolean("tg_enabled", false),
            botToken = prefs.getString("tg_bot_token", "") ?: "",
            chatId = prefs.getString("tg_chat_id", "") ?: "",
            notifyOrders = prefs.getBoolean("tg_notify_orders", true),
            notifyQuestions = prefs.getBoolean("tg_notify_questions", true),
            notifyLowStock = prefs.getBoolean("tg_notify_low_stock", true),
            notifyReturns = prefs.getBoolean("tg_notify_returns", true),
            notifyErrors = prefs.getBoolean("tg_notify_errors", true),
            notifyDailyBrief = prefs.getBoolean("tg_notify_daily_brief", true),
            quietHoursEnabled = prefs.getBoolean("tg_quiet_enabled", false),
            quietHoursStart = prefs.getInt("tg_quiet_start", 23),
            quietHoursEnd = prefs.getInt("tg_quiet_end", 7)
        )
    }

    fun saveTelegramConfig(config: TelegramConfiguration) {
        prefs.edit()
            .putBoolean("tg_enabled", config.isEnabled)
            .putString("tg_bot_token", config.botToken.trim())
            .putString("tg_chat_id", config.chatId.trim())
            .putBoolean("tg_notify_orders", config.notifyOrders)
            .putBoolean("tg_notify_questions", config.notifyQuestions)
            .putBoolean("tg_notify_low_stock", config.notifyLowStock)
            .putBoolean("tg_notify_returns", config.notifyReturns)
            .putBoolean("tg_notify_errors", config.notifyErrors)
            .putBoolean("tg_notify_daily_brief", config.notifyDailyBrief)
            .putBoolean("tg_quiet_enabled", config.quietHoursEnabled)
            .putInt("tg_quiet_start", config.quietHoursStart)
            .putInt("tg_quiet_end", config.quietHoursEnd)
            .apply()
    }

    // Profit Calculation Rules
    fun getProfitRules(): ProfitCalculationRules {
        return ProfitCalculationRules(
            defaultCommissionRate = prefs.getFloat("calc_commission", 18.5f).toDouble(),
            defaultShippingCost = prefs.getFloat("calc_shipping", 35.0f).toDouble(),
            defaultTaxRate = prefs.getFloat("calc_tax", 10.0f).toDouble(),
            returnExpenseBufferPercent = prefs.getFloat("calc_return_buffer", 3.0f).toDouble(),
            packagingAndOtherCost = prefs.getFloat("calc_other", 6.5f).toDouble()
        )
    }

    fun saveProfitRules(rules: ProfitCalculationRules) {
        prefs.edit()
            .putFloat("calc_commission", rules.defaultCommissionRate.toFloat())
            .putFloat("calc_shipping", rules.defaultShippingCost.toFloat())
            .putFloat("calc_tax", rules.defaultTaxRate.toFloat())
            .putFloat("calc_return_buffer", rules.returnExpenseBufferPercent.toFloat())
            .putFloat("calc_other", rules.packagingAndOtherCost.toFloat())
            .apply()
    }

    // App Language & System Settings
    fun getAppLanguage(): String = prefs.getString("app_language", "tr") ?: "tr"
    fun setAppLanguage(lang: String) = prefs.edit().putString("app_language", lang).apply()

    fun getAutoSyncIntervalMinutes(): Int = prefs.getInt("auto_sync_interval", 30)
    fun setAutoSyncIntervalMinutes(minutes: Int) = prefs.edit().putInt("auto_sync_interval", minutes).apply()

    fun getLowStockThreshold(): Int = prefs.getInt("low_stock_threshold", 5)
    fun setLowStockThreshold(threshold: Int) = prefs.edit().putInt("low_stock_threshold", threshold).apply()
}
