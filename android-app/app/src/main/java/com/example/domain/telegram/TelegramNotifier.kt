package com.example.domain.telegram

import com.example.data.local.AuditLogDao
import com.example.data.local.AuditLogEntity
import com.example.security.SecureConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TelegramNotifier(
    private val configManager: SecureConfigManager,
    private val auditLogDao: AuditLogDao
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // De-duplication cache: messageHash -> timestamp sent (suppress duplicate within 10 minutes)
    private val recentMessageHashes = ConcurrentHashMap<String, Long>()
    private val duplicateSuppressionWindowMs = 10 * 60 * 1000L // 10 minutes

    suspend fun sendTelegramMessage(text: String, category: String = "GENEL"): Result<String> = withContext(Dispatchers.IO) {
        val config = configManager.getTelegramConfig()
        if (!config.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Telegram bildirimleri kapalı."))
        }
        if (config.botToken.isBlank() || config.chatId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Bot Token veya Chat ID yapılandırılmamış."))
        }

        // Validate category filter
        val isCategoryEnabled = when (category) {
            "SİPARİŞ" -> config.notifyOrders
            "MÜŞTERİ_SORUSU" -> config.notifyQuestions
            "DÜŞÜK_STOK" -> config.notifyLowStock
            "İADE" -> config.notifyReturns
            "HATA", "KRİTİK_HATA" -> config.notifyErrors
            "GÜNLÜK_RAPOR" -> config.notifyDailyBrief
            "TEST" -> true
            else -> true
        }
        if (!isCategoryEnabled) {
            return@withContext Result.failure(IllegalStateException("Kategori ($category) bildirimleri kullanıcı tarafından devre dışı bırakılmış."))
        }

        // De-duplication check
        val messageHash = "$category:${text.hashCode()}"
        val now = System.currentTimeMillis()
        val lastSent = recentMessageHashes[messageHash]
        if (category != "TEST" && lastSent != null && (now - lastSent) < duplicateSuppressionWindowMs) {
            return@withContext Result.failure(IllegalStateException("Tekrarlanan bildirim engellendi (Son 10 dakika içinde iletildi)."))
        }

        // Check quiet hours
        if (config.quietHoursEnabled) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isQuiet = if (config.quietHoursStart > config.quietHoursEnd) {
                hour >= config.quietHoursStart || hour < config.quietHoursEnd
            } else {
                hour in config.quietHoursStart until config.quietHoursEnd
            }
            if (isQuiet && category != "KRİTİK_HATA" && category != "TEST") {
                return@withContext Result.failure(IllegalStateException("Sessiz saatler devrede ($hour:00). Bildirim iletimi ertelendi."))
            }
        }

        try {
            val url = "https://api.telegram.org/bot${config.botToken}/sendMessage"
            val payload = JSONObject().apply {
                put("chat_id", config.chatId)
                put("text", "🛍️ <b>[M&E Tekstil Trendyol]</b>\n\n$text")
                put("parse_mode", "HTML")
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                if (response.isSuccessful) {
                    recentMessageHashes[messageHash] = now
                    // Clean up old hashes
                    if (recentMessageHashes.size > 200) {
                        recentMessageHashes.entries.removeIf { (now - it.value) > duplicateSuppressionWindowMs }
                    }

                    auditLogDao.insertLog(
                        AuditLogEntity(
                            actionType = "TELEGRAM_SENT",
                            moduleName = "Telegram Bildirim",
                            details = "Bildirim başarıyla gönderildi: $category"
                        )
                    )
                    Result.success("Telegram bildirimi başarıyla iletildi.")
                } else if (code == 429) {
                    auditLogDao.insertLog(
                        AuditLogEntity(
                            actionType = "TELEGRAM_RATE_LIMITED",
                            moduleName = "Telegram Bildirim",
                            details = "Telegram HTTP 429: Hız sınırı aşıldı."
                        )
                    )
                    Result.failure(Exception("Telegram API Hız Sınırı (HTTP 429). Lütfen kısa süre bekleyin."))
                } else {
                    auditLogDao.insertLog(
                        AuditLogEntity(
                            actionType = "TELEGRAM_FAILED",
                            moduleName = "Telegram Bildirim",
                            details = "HTTP $code hatası"
                        )
                    )
                    Result.failure(Exception("Telegram API Hatası: HTTP $code"))
                }
            }
        } catch (e: Exception) {
            val err = e.message ?: "Ağ hatası"
            Result.failure(Exception("Telegram gönderilemedi: $err"))
        }
    }
}
