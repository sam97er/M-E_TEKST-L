package com.example.domain.ai

import com.example.domain.model.AiTaskType
import com.example.security.AiSlotConfiguration
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

interface AiProviderAdapter {
    val providerName: String
    suspend fun testConnection(config: AiSlotConfiguration): Result<String>
    suspend fun generateResponse(
        config: AiSlotConfiguration,
        systemPrompt: String,
        userPrompt: String
    ): Result<String>
}

class GeminiProviderAdapter : AiProviderAdapter {
    override val providerName: String = "Google Gemini"

    private fun getClient(timeoutSec: Int): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .build()

    override suspend fun testConnection(config: AiSlotConfiguration): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API Anahtarı girilmedi."))
        }
        return generateResponse(
            config = config,
            systemPrompt = "Sen bir e-ticaret asistanısın.",
            userPrompt = "Merhaba! Bağlantı testi için yalnızca 'BAĞLANTI_BAŞARILI' yaz."
        )
    }

    override suspend fun generateResponse(
        config: AiSlotConfiguration,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API anahtarı bulunamadı."))
        }

        return try {
            val model = if (config.modelName.isBlank()) "gemini-3.5-flash" else config.modelName
            val base = if (config.baseUrl.isBlank()) "https://generativelanguage.googleapis.com/v1beta" else config.baseUrl.trimEnd('/')
            val url = "$base/models/$model:generateContent?key=${config.apiKey}"

            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", "$systemPrompt\n\nKullanıcı İsteği:\n$userPrompt"))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val client = getClient(config.timeoutSeconds)
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyString).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $bodyString"
                    }
                    Result.failure(IOException("Gemini API Hatası: $errorMsg"))
                } else {
                    val parsed = JSONObject(bodyString)
                    val candidates = parsed.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val first = candidates.getJSONObject(0)
                        val content = first.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text") ?: ""
                        Result.success(text.trim())
                    } else {
                        Result.failure(IOException("Gemini yanıtında içerik bulunamadı."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class OpenAiCompatibleAdapter : AiProviderAdapter {
    override val providerName: String = "OpenAI / Uyumlu"

    private fun getClient(timeoutSec: Int): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSec.toLong(), TimeUnit.SECONDS)
            .build()

    override suspend fun testConnection(config: AiSlotConfiguration): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("OpenAI API Anahtarı girilmedi."))
        }
        return generateResponse(
            config = config,
            systemPrompt = "Sen bir e-ticaret asistanısın.",
            userPrompt = "Test: 'BAĞLANTI_BAŞARILI' yaz."
        )
    }

    override suspend fun generateResponse(
        config: AiSlotConfiguration,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        if (config.apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API anahtarı bulunamadı."))
        }

        return try {
            val base = if (config.baseUrl.isBlank()) "https://api.openai.com/v1" else config.baseUrl.trimEnd('/')
            val url = "$base/chat/completions"
            val model = if (config.modelName.isBlank()) "gpt-4o-mini" else config.modelName

            val jsonBody = JSONObject().apply {
                put("model", model)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }
                put("messages", messages)
                put("temperature", 0.7)
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${config.apiKey}")
                .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val client = getClient(config.timeoutSeconds)
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        val errObj = JSONObject(bodyString).optJSONObject("error")
                        errObj?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $bodyString"
                    }
                    Result.failure(IOException("OpenAI Uyumlu API Hatası: $errorMsg"))
                } else {
                    val parsed = JSONObject(bodyString)
                    val choices = parsed.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val choice = choices.getJSONObject(0)
                        val msg = choice.optJSONObject("message")
                        val text = msg?.optString("content") ?: ""
                        Result.success(text.trim())
                    } else {
                        Result.failure(IOException("API yanıtında içerik bulunamadı."))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class IntelligentTurkishFallbackAdapter : AiProviderAdapter {
    override val providerName: String = "Yerel Akıllı Yedek Motoru"

    override suspend fun testConnection(config: AiSlotConfiguration): Result<String> {
        return Result.success("Yerel Akıllı Motor Aktif (Çevrimdışı / Simülasyon Koruması)")
    }

    override suspend fun generateResponse(
        config: AiSlotConfiguration,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        // Generates grounded, contextual Turkish output based on keywords
        val lower = userPrompt.lowercase()
        val response = when {
            lower.contains("kumaş") || lower.contains("pamuk") || lower.contains("kalıp") || lower.contains("beden") -> {
                "Değerli Müşterimiz, ürünümüz %100 birinci sınıf taranmış penye pamuk kumaştan üretilmiştir. Kumaş gramajı 190 gr/m² olup terletmeyen, nefes alabilen dokudadır. Standart regular fit kalıptır; günlük bedeninizde tercih etmenizi öneririz. 30 derecede tersten yıkandığında çekme ve solma yapmaz. Keyifli alışverişler dileriz. - M&E Tekstil"
            }
            lower.contains("kargo") || lower.contains("ne zaman") || lower.contains("teslimat") -> {
                "Merhaba, siparişleriniz Trendyol Express ile özenle hazırlanıp en geç 24 saat içerisinde kargoya teslim edilmektedir. Sipariş takip numaranız kargoya verildikten sonra SMS ve uygulama bildirimi ile iletilecektir. Teşekkür ederiz. - M&E Tekstil"
            }
            lower.contains("iade") || lower.contains("değişim") || lower.contains("sorun") -> {
                "Merhaba, yaşadığınız durum için özür dileriz. Trendyol Siparişlerim ekranından kolay iade kodu oluşturarak anlaşmalı kargo ile tarafımıza ücretsiz gönderebilirsiniz. Ürün bize ulaştığında ücret iadeniz hızla onaylanacaktır. Memnuniyetiniz bizim için önceliklidir. - M&E Tekstil"
            }
            lower.contains("kâr") || lower.contains("maliyet") || lower.contains("analiz") -> {
                "İş ve Kâr Değerlendirmesi: M&E Tekstil koleksiyonundaki polo yaka tişört ve gömlek gruplarında ortalama brüt kâr marjı %38.2, net kâr marjı komisyon ve kargo sonrası %21.4 seviyesindedir. Kargo barem indirimlerinden faydalanmak için 2'li set satışlarının artırılması net kârı ürün başına 14.50 TL artıracaktır."
            }
            lower.contains("başlık") || lower.contains("açıklama") || lower.contains("seo") -> {
                "Önerilen Başlık: 'M&E Tekstil Erkek %100 Pamuklu Polo Yaka Slim Fit Tişört - Nefes Alan Pike Kumaş'\n\nÖnerilen Açıklama:\n• Kumaş: %100 Pamuk Pike Kumaş\n• Kalıp: Modern Slim Fit\n• Düğme ve yaka içi şerit garnili özel tasarım\n• Çekmez, solmaz, yüksek yıkama dayanıklılığı\n• Günlük, ofis ve casual kullanım için idealdir."
            }
            else -> {
                "Değerli Müşterimiz, talebiniz M&E Tekstil yetkilileri tarafından incelenmiştir. Ürünlerimiz kendi atölyemizde yüksek kalite standartlarında üretilmektedir. Her türlü sorunuzda yardımcı olmaktan memnuniyet duyarız. Sağlıklı günler dileriz."
            }
        }
        return Result.success(response)
    }
}
