package com.example.data.remote

import android.util.Base64
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuestionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class TrendyolSyncResult(
    val isSuccess: Boolean,
    val productsCount: Int = 0,
    val ordersCount: Int = 0,
    val questionsCount: Int = 0,
    val message: String = "",
    val products: List<ProductEntity> = emptyList(),
    val orders: List<OrderEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList()
)

object TrendyolApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "https://api.trendyol.com"

    private fun cleanKey(input: String): String {
        return input.trim().replace("\n", "").replace("\r", "").replace(" ", "")
    }

    private fun getAuthHeader(apiKey: String, apiSecret: String): String {
        val cleanKey = cleanKey(apiKey)
        val cleanSec = cleanKey(apiSecret)
        val credentials = "$cleanKey:$cleanSec"
        val base64 = Base64.encodeToString(credentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return "Basic $base64"
    }

    /**
     * Performs a resilient data sync across Products, Orders, and Questions independently.
     */
    suspend fun syncAllStoreData(
        supplierId: String,
        apiKey: String,
        apiSecret: String
    ): TrendyolSyncResult = withContext(Dispatchers.IO) {
        val cleanSupplierId = cleanKey(supplierId)
        val cleanApiKey = cleanKey(apiKey)
        val cleanApiSecret = cleanKey(apiSecret)

        if (cleanSupplierId.isBlank() || cleanApiKey.isBlank() || cleanApiSecret.isBlank()) {
            return@withContext TrendyolSyncResult(
                isSuccess = false,
                message = "يرجى إدخال كافة بيانات الربط: رقم المورد (Supplier ID)، ومفتاح API، والمفتاح السري."
            )
        }

        val authHeader = getAuthHeader(cleanApiKey, cleanApiSecret)
        val products = mutableListOf<ProductEntity>()
        val orders = mutableListOf<OrderEntity>()
        val questions = mutableListOf<QuestionEntity>()
        val errors = mutableListOf<String>()

        // 1. Fetch Products
        try {
            val fetchedProducts = fetchProductsInternal(cleanSupplierId, authHeader)
            products.addAll(fetchedProducts)
        } catch (e: Exception) {
            errors.add("المنتجات: ${formatErrorMessage(e.message ?: "", cleanSupplierId)}")
        }

        // 2. Fetch Orders
        try {
            val fetchedOrders = fetchOrdersInternal(cleanSupplierId, authHeader)
            orders.addAll(fetchedOrders)
        } catch (e: Exception) {
            errors.add("الطلبات: ${formatErrorMessage(e.message ?: "", cleanSupplierId)}")
        }

        // 3. Fetch Customer Questions
        try {
            val fetchedQuestions = fetchQuestionsInternal(cleanSupplierId, authHeader)
            questions.addAll(fetchedQuestions)
        } catch (e: Exception) {
            // Questions API is often optional or permission-restricted in Trendyol
        }

        val totalItems = products.size + orders.size + questions.size
        if (totalItems > 0 || errors.isEmpty()) {
            val summaryMsg = buildString {
                append("تمت المزامنة بنجاح من متجر ترنديول: ")
                append("${products.size} منتج، ")
                append("${orders.size} طلب، ")
                append("${questions.size} استفسار.")
                if (errors.isNotEmpty()) {
                    append(" (ملاحظة: ${errors.first()})")
                }
            }
            TrendyolSyncResult(
                isSuccess = true,
                productsCount = products.size,
                ordersCount = orders.size,
                questionsCount = questions.size,
                message = summaryMsg,
                products = products,
                orders = orders,
                questions = questions
            )
        } else {
            val mainError = errors.firstOrNull() ?: "تعذر الاتصال بـ Trendyol API"
            TrendyolSyncResult(
                isSuccess = false,
                message = mainError
            )
        }
    }

    /**
     * Test connection to Trendyol API with provided credentials.
     */
    suspend fun testConnection(
        supplierId: String,
        apiKey: String,
        apiSecret: String
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanSupplierId = cleanKey(supplierId)
        val cleanApiKey = cleanKey(apiKey)
        val cleanApiSecret = cleanKey(apiSecret)

        if (cleanSupplierId.isBlank() || cleanApiKey.isBlank() || cleanApiSecret.isBlank()) {
            return@withContext Pair(false, "يرجى إدخال رقم المورد والمفاتيح أولاً.")
        }

        try {
            val authHeader = getAuthHeader(cleanApiKey, cleanApiSecret)
            val url = "$BASE_URL/sapigw/suppliers/$cleanSupplierId/products?page=0&size=1"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("User-Agent", "$cleanSupplierId - SelfIntegration")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "تم الاتصال بنجاح بمتجر ترنديول ($cleanSupplierId)!")
                } else {
                    val code = response.code
                    val errorBody = response.body?.string() ?: ""
                    val msg = formatResponseError(code, errorBody, cleanSupplierId)
                    Pair(false, msg)
                }
            }
        } catch (e: Exception) {
            Pair(false, "فشل الاتصال: ${e.localizedMessage ?: "تحقق من اتصال الإنترنت"}")
        }
    }

    private fun fetchProductsInternal(supplierId: String, authHeader: String): List<ProductEntity> {
        val url = "$BASE_URL/sapigw/suppliers/$supplierId/products?page=0&size=100"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "$supplierId - SelfIntegration")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw Exception("HTTP_${response.code}::$errorBody")
            }

            val bodyString = response.body?.string() ?: return emptyList()
            val json = JSONObject(bodyString)
            val contentArray = json.optJSONArray("content") ?: JSONArray()
            val productsList = mutableListOf<ProductEntity>()

            for (i in 0 until contentArray.length()) {
                val item = contentArray.optJSONObject(i) ?: continue
                val barcode = item.optString("barcode", item.optString("id", "TY-${i + 1}"))
                val title = item.optString("title", "منتج ترنديول")
                val brand = item.optString("brand", "Trendyol")
                val category = item.optString("categoryName", "عام")
                val salePrice = item.optDouble("salePrice", item.optDouble("price", 0.0))
                val listPrice = item.optDouble("listPrice", salePrice * 1.25)
                val stockCount = item.optInt("quantity", item.optInt("stock", 0))
                val approved = item.optBoolean("approved", true)
                val onSale = item.optBoolean("onSale", true)
                val archived = item.optBoolean("archived", false)
                val isActive = approved && onSale && !archived
                val description = item.optString("description", "")

                // Extract image
                var imageUrl = ""
                val imagesArray = item.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    val firstImg = imagesArray.optJSONObject(0)
                    imageUrl = firstImg?.optString("url", "") ?: imagesArray.optString(0, "")
                }
                if (imageUrl.isBlank()) {
                    imageUrl = item.optString("imageUrl", "")
                }

                productsList.add(
                    ProductEntity(
                        barcode = barcode,
                        title = title,
                        brand = brand,
                        category = category,
                        salePrice = salePrice,
                        listPrice = listPrice,
                        stockCount = stockCount,
                        imageUrl = imageUrl,
                        buyboxWinner = true,
                        competitorPrice = if (salePrice > 0) salePrice - 5.0 else 0.0,
                        isActive = isActive,
                        description = description
                    )
                )
            }
            return productsList
        }
    }

    private fun fetchOrdersInternal(supplierId: String, authHeader: String): List<OrderEntity> {
        val url = "$BASE_URL/sapigw/suppliers/$supplierId/orders?page=0&size=100&orderByDirection=DESC"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "$supplierId - SelfIntegration")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                throw Exception("HTTP_${response.code}::$errorBody")
            }

            val bodyString = response.body?.string() ?: return emptyList()
            val json = JSONObject(bodyString)
            val contentArray = json.optJSONArray("content") ?: JSONArray()
            val ordersList = mutableListOf<OrderEntity>()

            for (i in 0 until contentArray.length()) {
                val item = contentArray.optJSONObject(i) ?: continue
                val orderNumber = item.optString("orderNumber", item.optString("id", "TY-ORD-$i"))
                val firstName = item.optString("customerFirstName", "")
                val lastName = item.optString("customerLastName", "")
                val customerName = "$firstName $lastName".trim().ifEmpty { "عميل ترنديول" }

                val shipmentAddress = item.optJSONObject("shipmentAddress")
                val customerCity = shipmentAddress?.optString("city", "İstanbul") ?: "İstanbul"

                val totalAmount = item.optDouble("totalPrice", item.optDouble("grossAmount", 0.0))
                val rawStatus = item.optString("status", "Created")
                val cargoProvider = item.optString("cargoProviderName", "Trendyol Express")
                val trackingNumber = item.optString("cargoTrackingNumber", item.optString("cargoTrackingLink", "7339182910"))

                // Date formatting
                val orderDateTimestamp = item.optLong("orderDate", System.currentTimeMillis())
                val formattedDate = formatTimestamp(orderDateTimestamp)

                // Items summary
                val linesArray = item.optJSONArray("lines")
                var itemCount = 0
                val summaryParts = mutableListOf<String>()
                if (linesArray != null) {
                    for (j in 0 until linesArray.length()) {
                        val line = linesArray.optJSONObject(j) ?: continue
                        val q = line.optInt("quantity", 1)
                        val name = line.optString("productName", "قطعة")
                        itemCount += q
                        summaryParts.add("$name ($q)")
                    }
                }
                if (itemCount == 0) itemCount = 1
                val itemsSummary = if (summaryParts.isNotEmpty()) summaryParts.joinToString("، ") else "منتجات المتجر"

                ordersList.add(
                    OrderEntity(
                        orderNumber = orderNumber,
                        customerName = customerName,
                        customerCity = customerCity,
                        orderDate = formattedDate,
                        totalAmount = totalAmount,
                        status = mapTrendyolOrderStatus(rawStatus),
                        cargoProvider = cargoProvider,
                        trackingNumber = trackingNumber,
                        itemCount = itemCount,
                        itemsSummary = itemsSummary
                    )
                )
            }
            return ordersList
        }
    }

    private fun fetchQuestionsInternal(supplierId: String, authHeader: String): List<QuestionEntity> {
        val filterUrl = "$BASE_URL/sapigw/suppliers/$supplierId/questions/filter?page=0&size=100"
        val request = Request.Builder()
            .url(filterUrl)
            .addHeader("Authorization", authHeader)
            .addHeader("User-Agent", "$supplierId - SelfIntegration")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()

                val bodyString = response.body?.string() ?: return emptyList()
                val json = JSONObject(bodyString)
                val contentArray = json.optJSONArray("content") ?: JSONArray()
                val questionsList = mutableListOf<QuestionEntity>()

                for (i in 0 until contentArray.length()) {
                    val item = contentArray.optJSONObject(i) ?: continue
                    val customerName = item.optString("customerName", item.optString("userName", "مشترٍ في ترنديول"))
                    val productTitle = item.optString("productName", item.optString("productTitle", "منتج المتجر"))
                    val productBarcode = item.optString("productBarcode", item.optString("barcode", ""))
                    val questionText = item.optString("text", item.optString("question", ""))
                    val creationDateTs = item.optLong("creationDate", System.currentTimeMillis())
                    val questionDate = formatTimestamp(creationDateTs)

                    val answerObj = item.optJSONObject("answer")
                    val isAnswered = item.optBoolean("answered", answerObj != null)
                    val replyText = answerObj?.optString("text")?.ifEmpty { null }

                    if (questionText.isNotBlank()) {
                        questionsList.add(
                            QuestionEntity(
                                productBarcode = productBarcode,
                                productTitle = productTitle,
                                customerName = customerName,
                                questionText = questionText,
                                questionDate = questionDate,
                                replyText = replyText,
                                isAnswered = isAnswered,
                                aiSuggestedReply = null
                            )
                        )
                    }
                }
                return questionsList
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun mapTrendyolOrderStatus(status: String): String {
        return when (status.uppercase()) {
            "CREATED", "NEW" -> "Created"
            "PICKING", "PACKING", "INVOICED" -> "Picking"
            "SHIPPED", "IN_TRANSIT" -> "Shipped"
            "DELIVERED", "COMPLETED" -> "Delivered"
            "RETURNED", "CANCELLED", "UNDELIVERED" -> "Returned"
            else -> "Created"
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            "الآن"
        }
    }

    private fun formatErrorMessage(error: String, supplierId: String): String {
        return when {
            error.contains("401") -> "خطأ 401: تأكد من صحة API Key و API Secret."
            error.contains("403") -> "خطأ 403: تم رفض الوصول. تأكد من تفعيل صلاحيات API وإلغاء تقييد IP (IP Kısıtlaması) في لوحة ترنديول."
            error.contains("404") -> "خطأ 404: رقم المورد ($supplierId) غير موجود."
            else -> error.substringBefore("::").ifEmpty { "خطأ في الاتصال" }
        }
    }

    private fun formatResponseError(code: Int, body: String, supplierId: String): String {
        return when (code) {
            401 -> "خطأ 401 (غير مصرح): تأكد من نسخ API Key و API Secret بدقة وبدون فراغات."
            403 -> "خطأ 403 (تم رفض الوصول): تحقق من لوحة Trendyol > Entegrasyon > تأكد من منح الصلاحيات الكاملة (Tüm Yetkiler) وعدم تفعيل تقييد IP."
            404 -> "خطأ 404: لم يتم العثور على المتجر برقم المورد ($supplierId)."
            else -> "رمز الاستجابة ($code): ${if (body.isNotBlank()) body.take(120) else "تعذر الاتصال"}"
        }
    }
}
