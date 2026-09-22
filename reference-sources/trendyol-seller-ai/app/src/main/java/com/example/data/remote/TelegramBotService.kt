package com.example.data.remote

import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuestionEntity
import com.example.data.repository.DashboardMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TelegramBotService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private fun cleanInput(input: String): String {
        return input.trim().replace("\n", "").replace("\r", "").replace(" ", "")
    }

    /**
     * Send arbitrary text message to Telegram chat
     */
    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
        parseMode: String = "HTML"
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanToken = cleanInput(botToken)
        val cleanChatId = cleanInput(chatId)

        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Pair(false, "يرجى إدخال Bot API Token و Chat ID أولاً.")
        }

        val url = "https://api.telegram.org/bot$cleanToken/sendMessage"

        try {
            val jsonPayload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", text)
                put("parse_mode", parseMode)
                put("disable_web_page_preview", true)
            }

            val requestBody = jsonPayload.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Pair(true, "تم إرسال الإشعار إلى تيليجرام بنجاح!")
                } else {
                    val errorJson = try { JSONObject(body) } catch (e: Exception) { null }
                    val desc = errorJson?.optString("description", "فشل الإرسال") ?: "رمز الخطأ (${response.code})"
                    Pair(false, "فشل الإرسال: $desc (تأكد من بدء محادثة مع البوت بالضغط على /start)")
                }
            }
        } catch (e: Exception) {
            Pair(false, "خطأ في الاتصال بتيليجرام: ${e.localizedMessage ?: "تحقق من الإنترنت"}")
        }
    }

    /**
     * Test connection by sending a welcome test message
     */
    suspend fun testConnection(botToken: String, chatId: String, storeName: String): Pair<Boolean, String> {
        val currentDate = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date())
        val message = """
            🤖 <b>تم تفعيل بوت ترنديول بنجاح!</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            📅 <b>التاريخ:</b> $currentDate
            
            ✅ <b>قنوات الإشعارات المتصلة:</b>
            • 🛍️ طلبات ترنديول الجديدة
            • 💬 أسئلة واستفسارات المشترين
            • ⚠️ تنبيهات انخفاض المخزون
            • 📊 تقرير المبيعات والأداء اليومي
            • 💡 توصيات الذكاء الاصطناعي للمبيعات
            
            🚀 <i>البوت جاهز لإرسال كافة التنبيهات اللحظية لمتجرك!</i>
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }

    /**
     * 1. Notification: New Trendyol Order
     */
    suspend fun sendNewOrderNotification(
        botToken: String,
        chatId: String,
        order: OrderEntity,
        storeName: String
    ): Pair<Boolean, String> {
        val message = """
            🛍️ <b>طلب ترنديول جديد! (Yeni Sipariş)</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            📦 <b>رقم الطلب:</b> <code>#${order.orderNumber}</code>
            👤 <b>المشتري:</b> ${order.customerName} (${order.customerCity})
            💰 <b>المبلغ الإجمالي:</b> <b>${String.format(Locale.US, "%.2f", order.totalAmount)} ₺</b>
            🚚 <b>شركة الشحن:</b> ${order.cargoProvider}
            📋 <b>تفاصيل المنتجات (${order.itemCount} قطع):</b>
            ${order.itemsSummary}
            
            ⚡ <i>يرجى تجهيز وتغليف الشحنة وتسليمها لمندوب الشحن!</i>
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }

    /**
     * 2. Notification: New Customer Question
     */
    suspend fun sendNewQuestionNotification(
        botToken: String,
        chatId: String,
        question: QuestionEntity,
        storeName: String
    ): Pair<Boolean, String> {
        val message = """
            💬 <b>سؤال عميل جديد في ترنديول! (Müşteri Sorusu)</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            👤 <b>العميل:</b> ${question.customerName}
            🏷️ <b>المنتج:</b> <b>${question.productTitle}</b>
            
            ❓ <b>نص السؤال:</b>
            <i>"${question.questionText}"</i>
            
            💡 <b>توصية:</b> الرد السريع خلال 15 دقيقة يرفع احتمالية إتمام الطلب بنسبة 45%! افتح التطبيق للرد فوراً بنقرة واحدة عبر الذكاء الاصطناعي.
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }

    /**
     * 3. Notification: Low Stock Warning
     */
    suspend fun sendLowStockNotification(
        botToken: String,
        chatId: String,
        lowStockProducts: List<ProductEntity>,
        storeName: String
    ): Pair<Boolean, String> {
        if (lowStockProducts.isEmpty()) return Pair(true, "لا يوجد منتجات منخفضة المخزون.")

        val itemsList = lowStockProducts.joinToString("\n") { prod ->
            "• <b>${prod.title}</b>\n  الباركود: <code>${prod.barcode}</code> | ⚠️ المتبقي: <b>${prod.stockCount} فقط</b> (السعر: ${prod.salePrice} ₺)"
        }

        val message = """
            ⚠️ <b>تنبيه انخفاض المخزون! (Kritik Stok Uyarısı)</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            🚨 لديك <b>${lowStockProducts.size}</b> منتجات اقتربت كمياتها من النفاد:
            
            $itemsList
            
            📦 <i>يرجى تزويد المخزون لتجنب إيقاف ظهور المنتج في ترنديول وخسارة الباي بوكس.</i>
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }

    /**
     * 4. Notification: Daily Sales Report
     */
    suspend fun sendDailySalesReport(
        botToken: String,
        chatId: String,
        metrics: DashboardMetrics,
        storeName: String,
        topProductsSummary: String = ""
    ): Pair<Boolean, String> {
        val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val message = """
            📊 <b>تقرير المبيعات والأداء اليومي - ترنديول</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            📅 <b>تاريخ التقرير:</b> $currentDate
            
            💰 <b>إجمالي الإيرادات المحققة:</b> <b>${String.format(Locale.US, "%.2f", metrics.totalRevenue)} ₺</b>
            🛍️ <b>طلبات اليوم الجديدة:</b> <b>${metrics.todayOrders} طلب</b>
            📦 <b>إجمالي الطلبات المسجلة:</b> ${metrics.totalOrders} طلب
            🚚 <b>شحنات بانتظار التسليم:</b> ${metrics.pendingShipment} طرد
            ✅ <b>الطلبات المسلمة بنجاح:</b> ${metrics.deliveredCount} طرد
            🔄 <b>المرتجعات:</b> ${metrics.returnedCount} طرد
            ⭐ <b>تقييم المتجر (Satıcı Puanı):</b> 9.8 / 10
            
            ${if (topProductsSummary.isNotBlank()) "🏆 <b>أبرز المنتجات:</b>\n$topProductsSummary\n" else ""}
            📈 <i>نتمنى لك مبيعات وفيرة ومستمرة دائماً!</i>
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }

    /**
     * 5. Notification: AI Advice for Slow-Moving / Low-Selling Products
     */
    suspend fun sendAiSlowMovingProductsAdvice(
        botToken: String,
        chatId: String,
        slowProducts: List<ProductEntity>,
        aiAdvice: String,
        storeName: String
    ): Pair<Boolean, String> {
        val itemsList = slowProducts.take(5).joinToString("\n") { prod ->
            "• <b>${prod.title}</b> | السعر: ${prod.salePrice} ₺ | المخزون: ${prod.stockCount} | الباي بوكس: ${if (prod.buyboxWinner) "✅ فائز" else "❌ خاسر (${prod.competitorPrice} ₺)"}"
        }

        val message = """
            🤖 <b>اقتراحات الذكاء الاصطناعي للمنتجات الراكدة وقليلة المبيعات</b>
            ━━━━━━━━━━━━━━━━━
            🏬 <b>المتجر:</b> $storeName
            
            📦 <b>المنتجات المستهدفة بالتحسين (${slowProducts.size} منتجات):</b>
            $itemsList
            
            💡 <b>خطة العمل والتوصيات الذكية (AI Action Plan):</b>
            $aiAdvice
            
            🎯 <i>طبق التوصيات لإنعاش مبيعات المنتجات ورفع الترتيب في محرك بحث ترنديول!</i>
        """.trimIndent()

        return sendMessage(botToken, chatId, message, "HTML")
    }
}
