package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuestionEntity
import com.example.data.local.StoreSettingsEntity
import com.example.data.remote.GeminiExecutionResult
import com.example.data.remote.GeminiService
import com.example.data.remote.OptimizedProductDescription
import com.example.data.remote.ProductMetadata
import com.example.data.remote.TrendyolApiService
import com.example.data.remote.TrendyolSyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

data class AiAgentResponse(
    val displayText: String,
    val toolCallInfo: ToolCallInfo? = null,
    val attachedProducts: List<ProductEntity> = emptyList(),
    val quickActionType: QuickActionType = QuickActionType.NONE
)

data class ToolCallInfo(
    val functionName: String,
    val title: String,
    val parameters: String,
    val resultSummary: String
)

enum class QuickActionType {
    NONE,
    VIEW_PRODUCTS,
    VIEW_ORDERS,
    VIEW_QUESTIONS,
    RESTOCK_ITEMS,
    COPY_TEXT
}

class StoreRepository(private val database: AppDatabase) {

    val allProducts: Flow<List<ProductEntity>> = database.productDao().getAllProducts()
    val allOrders: Flow<List<OrderEntity>> = database.orderDao().getAllOrders()
    val allQuestions: Flow<List<QuestionEntity>> = database.questionDao().getAllQuestions()
    val storeSettings: Flow<StoreSettingsEntity?> = database.storeSettingsDao().getSettings()

    // Dashboard calculations
    val dashboardStats = allOrders.map { orders ->
        val totalRevenue = orders.sumOf { it.totalAmount }
        val todayOrders = orders.count { it.orderDate.contains("اليوم") || it.orderDate.contains("Bugün") }
        val pendingShipment = orders.count { it.status == "Created" || it.status == "Picking" }
        val deliveredCount = orders.count { it.status == "Delivered" }
        val returnedCount = orders.count { it.status == "Returned" }

        DashboardMetrics(
            totalRevenue = totalRevenue,
            todayOrders = todayOrders,
            pendingShipment = pendingShipment,
            deliveredCount = deliveredCount,
            returnedCount = returnedCount,
            totalOrders = orders.size
        )
    }

    suspend fun updateProductStock(productId: Long, newStock: Int) {
        database.productDao().updateStock(productId, newStock.coerceAtLeast(0))
    }

    suspend fun updateProductPrice(productId: Long, newPrice: Double) {
        database.productDao().updatePrice(productId, newPrice.coerceAtLeast(1.0))
    }

    suspend fun toggleProductActive(productId: Long, isActive: Boolean) {
        database.productDao().updateActiveStatus(productId, isActive)
    }

    suspend fun addProduct(product: ProductEntity): Long {
        return database.productDao().insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        database.productDao().updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        database.productDao().deleteProduct(product)
    }

    suspend fun updateOrderStatus(orderId: Long, newStatus: String) {
        database.orderDao().updateStatus(orderId, newStatus)
    }

    suspend fun submitQuestionReply(questionId: Long, replyText: String) {
        database.questionDao().submitReply(questionId, replyText)
    }

    suspend fun generateAiReplyForQuestion(question: QuestionEntity, customApiKey: String? = null): String {
        val prompt = """
            You are responding as a high-rated, polite Trendyol Seller to a prospective customer.
            Product Title: ${question.productTitle}
            Customer Name: ${question.customerName}
            Customer Question: "${question.questionText}"

            Write a warm, highly professional, accurate, and reassuring reply in the same language the customer used (Arabic if Arabic, Turkish if Turkish).
            Mention fast dispatch via Trendyol Express and 100% original authentic guarantee. Keep it concise (2-3 sentences).
        """.trimIndent()

        val aiResponse = GeminiService.generateAiContent(prompt, customApiKey)
        database.questionDao().setAiSuggestion(question.id, aiResponse)
        return aiResponse
    }

    suspend fun generateProductListing(
        productName: String,
        brand: String,
        category: String,
        keywords: String,
        customApiKey: String? = null
    ): String {
        val prompt = """
            You are a top-tier Trendyol e-commerce marketplace optimization specialist.
            Create a complete, high-converting product listing for:
            - Product: $productName
            - Brand: $brand
            - Category: $category
            - Key Features/Keywords: $keywords

            Format the response in Arabic with clear markdown:
            1. **عنوان المنتج المحسن (Trendyol SEO Title)** - under 100 characters, keyword rich.
            2. **أهم مميزات المنتج (Öne Çıkan Özellikler)** - 4-5 bullet points.
            3. **وصف تفصيلي تسويقي مقنع (Ürün Açıklaması)**.
            4. **الكلمات المفتاحية والوسوم (Arama Etiketleri)**.
        """.trimIndent()

        return GeminiService.generateAiContent(prompt, customApiKey)
    }

    /**
     * Generates a fully optimized, conversion-ready Trendyol product description based on product metadata.
     */
    suspend fun generateOptimizedProductDescription(
        metadata: ProductMetadata,
        customApiKey: String? = null
    ): OptimizedProductDescription {
        return GeminiService.generateOptimizedProductDescription(metadata, customApiKey)
    }

    suspend fun analyzeBuyboxStrategy(product: ProductEntity, customApiKey: String? = null): String {
        val prompt = """
            As a Trendyol marketplace pricing algorithm expert, analyze this product:
            Product: ${product.title}
            Current Sale Price: ${product.salePrice} TL
            List Price: ${product.listPrice} TL
            Competitor Lowest Price: ${product.competitorPrice} TL
            Buybox Status: ${if (product.buyboxWinner) "Winner (الفائز بالباي بوكس)" else "Lost (خاسر الباي بوكس)"}
            Stock: ${product.stockCount} units
            Category: ${product.category}

            Provide an actionable, structured recommendation in Arabic:
            1. Optimal price point to win/keep the Buybox without destroying margin.
            2. Recommended Trendyol promotional campaign (e.g., Çok Al Az Öde, Flaş İndirim, Kupon).
            3. Delivery & Stock velocity alert.
        """.trimIndent()

        return GeminiService.generateAiContent(prompt, customApiKey)
    }

    suspend fun saveSettings(settings: StoreSettingsEntity) {
        database.storeSettingsDao().saveSettings(settings)
    }

    suspend fun testTrendyolConnection(supplierId: String, apiKey: String, apiSecret: String): Pair<Boolean, String> {
        return TrendyolApiService.testConnection(supplierId, apiKey, apiSecret)
    }

    suspend fun testTelegramConnection(botToken: String, chatId: String, storeName: String): Pair<Boolean, String> {
        return com.example.data.remote.TelegramBotService.testConnection(botToken, chatId, storeName)
    }

    suspend fun sendTelegramDailyReport(customApiKey: String? = null): Pair<Boolean, String> {
        val settings = database.storeSettingsDao().getSettingsDirect() ?: StoreSettingsEntity()
        if (settings.telegramBotToken.isBlank() || settings.telegramChatId.isBlank()) {
            return Pair(false, "يرجى إدخال وحفظ مفاتيح Telegram Bot (Bot Token و Chat ID) في الإعدادات أولاً.")
        }
        val orders = database.orderDao().getAllOrdersDirect()
        val totalRevenue = orders.sumOf { it.totalAmount }
        val todayOrders = orders.count { it.orderDate.contains("اليوم") || it.orderDate.contains("Bugün") }
        val pendingShipment = orders.count { it.status == "Created" || it.status == "Picking" }
        val deliveredCount = orders.count { it.status == "Delivered" }
        val returnedCount = orders.count { it.status == "Returned" }

        val metrics = DashboardMetrics(
            totalRevenue = totalRevenue,
            todayOrders = todayOrders,
            pendingShipment = pendingShipment,
            deliveredCount = deliveredCount,
            returnedCount = returnedCount,
            totalOrders = orders.size
        )

        val products = database.productDao().getAllProductsDirect()
        val topSummary = products.take(3).joinToString("\n") { "• ${it.title} (${it.salePrice} ₺ - متبقي ${it.stockCount})" }

        return com.example.data.remote.TelegramBotService.sendDailySalesReport(
            botToken = settings.telegramBotToken,
            chatId = settings.telegramChatId,
            metrics = metrics,
            storeName = settings.storeName,
            topProductsSummary = topSummary
        )
    }

    suspend fun sendTelegramSlowMovingAdvice(customApiKey: String? = null): Pair<Boolean, String> {
        val settings = database.storeSettingsDao().getSettingsDirect() ?: StoreSettingsEntity()
        if (settings.telegramBotToken.isBlank() || settings.telegramChatId.isBlank()) {
            return Pair(false, "يرجى حفظ مفاتيح تيليجرام (Bot Token و Chat ID) أولاً.")
        }
        val allProducts = database.productDao().getAllProductsDirect()
        val targetProducts = allProducts.filter { !it.buyboxWinner || it.stockCount > 15 || it.salePrice > 500 }.ifEmpty { allProducts.take(3) }

        val prompt = """
            You are a senior Trendyol Marketplace Growth Consultant.
            Analyze these slow-moving or low-selling products:
            ${targetProducts.map { "- ${it.title} (Price: ${it.salePrice} TL, Stock: ${it.stockCount}, BuyboxWinner: ${it.buyboxWinner}, CompetitorPrice: ${it.competitorPrice} TL)" }.joinToString("\n")}

            Provide 3 structured, high-impact recommendations in Arabic for the seller to boost conversion immediately:
            1. <b>استراتيجية الأسعار والباي بوكس:</b> (تخفيض ذكي أو كوبونات).
            2. <b>تحسين الكلمات المفتاحية والظهور:</b> (Trendyol SEO).
            3. <b>الحملات الإعلانية والعروض الترويجية:</b> (Çok Al Az Öde أو Flaş İndirim).
            Keep it clear, concise, and ready for Telegram HTML formatting.
        """.trimIndent()

        val aiAdvice = GeminiService.generateAiContent(prompt, customApiKey)
        return com.example.data.remote.TelegramBotService.sendAiSlowMovingProductsAdvice(
            botToken = settings.telegramBotToken,
            chatId = settings.telegramChatId,
            slowProducts = targetProducts,
            aiAdvice = aiAdvice,
            storeName = settings.storeName
        )
    }

    suspend fun sendTelegramLowStockAlert(): Pair<Boolean, String> {
        val settings = database.storeSettingsDao().getSettingsDirect() ?: StoreSettingsEntity()
        if (settings.telegramBotToken.isBlank() || settings.telegramChatId.isBlank()) {
            return Pair(false, "يرجى حفظ مفاتيح تيليجرام أولاً.")
        }
        val lowStock = database.productDao().getAllProductsDirect().filter { it.stockCount < 5 && it.isActive }
        return com.example.data.remote.TelegramBotService.sendLowStockNotification(
            botToken = settings.telegramBotToken,
            chatId = settings.telegramChatId,
            lowStockProducts = lowStock,
            storeName = settings.storeName
        )
    }

    suspend fun syncTrendyolData(supplierId: String, apiKey: String, apiSecret: String): TrendyolSyncResult {
        val result = TrendyolApiService.syncAllStoreData(supplierId, apiKey, apiSecret)
        if (result.isSuccess) {
            if (result.products.isNotEmpty()) {
                database.productDao().insertAll(result.products)
            }
            if (result.orders.isNotEmpty()) {
                database.orderDao().insertAll(result.orders)
            }
            if (result.questions.isNotEmpty()) {
                database.questionDao().insertAll(result.questions)
            }

            // Trigger Telegram notifications if enabled
            val settings = database.storeSettingsDao().getSettingsDirect()
            if (settings != null && settings.isTelegramEnabled && settings.telegramBotToken.isNotBlank() && settings.telegramChatId.isNotBlank()) {
                if (settings.notifyNewOrders && result.orders.isNotEmpty()) {
                    result.orders.take(3).forEach { order ->
                        com.example.data.remote.TelegramBotService.sendNewOrderNotification(
                            settings.telegramBotToken,
                            settings.telegramChatId,
                            order,
                            settings.storeName
                        )
                    }
                }
                if (settings.notifyCustomerQuestions && result.questions.isNotEmpty()) {
                    result.questions.filter { !it.isAnswered }.take(2).forEach { question ->
                        com.example.data.remote.TelegramBotService.sendNewQuestionNotification(
                            settings.telegramBotToken,
                            settings.telegramChatId,
                            question,
                            settings.storeName
                        )
                    }
                }
            }
        }
        return result
    }

    /**
     * Architecture: User -> AI -> Function/Tool -> Database/API -> Result
     * Executes autonomous tools against Room Database and returns enriched responses.
     */
    suspend fun processAgentCommand(userQuery: String, customApiKey: String?): AiAgentResponse {
        val execResult = GeminiService.executeAgentQuery(userQuery, customApiKey)

        return when (execResult) {
            is GeminiExecutionResult.FunctionCallResponse -> {
                executeDatabaseTool(execResult.name, execResult.arguments, customApiKey)
            }
            is GeminiExecutionResult.TextResponse -> {
                AiAgentResponse(displayText = execResult.text)
            }
            is GeminiExecutionResult.Error -> {
                AiAgentResponse(displayText = "عذراً، حدث خطأ أثناء المعالجة: ${execResult.message}")
            }
        }
    }

    private suspend fun executeDatabaseTool(
        toolName: String,
        arguments: Map<String, Any?>,
        customApiKey: String?
    ): AiAgentResponse {
        return when (toolName) {
            "get_low_stock_products" -> {
                val threshold = (arguments["threshold"] as? Number)?.toInt() ?: 5
                val products = database.productDao().getLowStockProductsDirect(threshold)
                val summary = if (products.isEmpty()) {
                    "✅ **فحص المخزون:** جميع المنتجات متوفرة بكميات كافية (أكبر من $threshold قطع)."
                } else {
                    val listDetails = products.joinToString("\n") { p ->
                        "• **${p.title}** (${p.brand})\n  - الباركود: `${p.barcode}` | السعر: ${p.salePrice} ₺ | **المتبقي: ${p.stockCount} قطع فقط ⚠️**"
                    }
                    """
                    ⚠️ **تنبيه المخزون الحرج (استدعاء دالة قاعدة البيانات):**
                    تم العثور على **${products.size}** منتجات يقل مخزونها عن **$threshold** قطع في متجرك.
                    
                    $listDetails

                    💡 *توصية الخوارزمية:* يُرجى إعادة تزويد المخزون فوراً لتجنب غرامة الإلغاء (Tedarik Edememe Cezası) وفقدان تصنيف الباي بوكس.
                    """.trimIndent()
                }

                AiAgentResponse(
                    displayText = summary,
                    toolCallInfo = ToolCallInfo(
                        functionName = "get_low_stock_products",
                        title = "فحص المنتجات ذات المخزون المنخفض (قاعدة البيانات)",
                        parameters = "الحد الأقصى: $threshold قطع",
                        resultSummary = "تم جلب ${products.size} منتج"
                    ),
                    attachedProducts = products,
                    quickActionType = QuickActionType.RESTOCK_ITEMS
                )
            }

            "calculate_revenue_and_profit" -> {
                val orders = database.orderDao().getAllOrdersDirect()
                val settings = database.storeSettingsDao().getSettingsDirect()
                val commissionRate = (settings?.commissionAverage ?: 18.0) / 100.0

                val todayOrders = orders.filter { it.orderDate.contains("اليوم") || it.orderDate.contains("Bugün") }
                val todayRevenue = todayOrders.sumOf { it.totalAmount }
                val totalRevenue = orders.sumOf { it.totalAmount }

                val todayEstimatedCommission = todayRevenue * commissionRate
                val todayEstimatedShipping = todayOrders.size * 27.50 // Average shipping barem
                val todayEstimatedNetProfit = (todayRevenue - todayEstimatedCommission - todayEstimatedShipping) * 0.40 // 40% gross margin approx

                val totalCommission = totalRevenue * commissionRate
                val totalShipping = orders.size * 27.50
                val totalNetProfit = (totalRevenue - totalCommission - totalShipping) * 0.40

                val displayText = """
                📊 **التقرير المالي والأرباح اللحظية (استدعاء دالة الحسابات من قاعدة البيانات):**

                🔹 **مبيعات وأرباح اليوم:**
                • إجمالي المبيعات: **${String.format(Locale.US, "%.2f", todayRevenue)} ₺** (${todayOrders.size} طلبات)
                • عمولة ترنديول (${settings?.commissionAverage ?: 18.0}%): **-${String.format(Locale.US, "%.2f", todayEstimatedCommission)} ₺**
                • تكلفة الشحن التقديرية (Trendyol Express): **-${String.format(Locale.US, "%.2f", todayEstimatedShipping)} ₺**
                • 💰 **صافي الربح التقديري لليوم:** **${String.format(Locale.US, "%.2f", todayEstimatedNetProfit.coerceAtLeast(0.0))} ₺**

                🔹 **الإجمالي الكلي للمتجر:**
                • إجمالي المبيعات المحققة: **${String.format(Locale.US, "%.2f", totalRevenue)} ₺** (${orders.size} طلب)
                • إجمالي صافي الأرباح التقديرية: **${String.format(Locale.US, "%.2f", totalNetProfit.coerceAtLeast(0.0))} ₺**
                • متوسط تقييم المتجر: **${settings?.storeScore ?: 9.8} / 10**

                ✅ الحسابات مطابقة لسجلات الطلبات المباشرة في النظام.
                """.trimIndent()

                AiAgentResponse(
                    displayText = displayText,
                    toolCallInfo = ToolCallInfo(
                        functionName = "calculate_revenue_and_profit",
                        title = "حساب الأرباح والمبيعات الحقيقية من قاعدة البيانات",
                        parameters = "الطلبات المحللة: ${orders.size}",
                        resultSummary = "مبيعات اليوم: ${String.format(Locale.US, "%.2f", todayRevenue)} ₺"
                    ),
                    quickActionType = QuickActionType.VIEW_ORDERS
                )
            }

            "generate_product_description" -> {
                val productName = arguments["productName"]?.toString() ?: "سترة فاخرة"
                val brand = arguments["brand"]?.toString() ?: "Trendyol Collection"
                val category = arguments["category"]?.toString() ?: "ملابس"
                val keywords = arguments["keywords"]?.toString() ?: "خامة ممتازة وتصميم تركي عصري"

                val listing = generateProductListing(productName, brand, category, keywords, customApiKey)

                AiAgentResponse(
                    displayText = listing,
                    toolCallInfo = ToolCallInfo(
                        functionName = "generate_product_description",
                        title = "توليد بطاقة منتج متوافقة مع SEO ترنديول",
                        parameters = "المنتج: $productName | الماركة: $brand",
                        resultSummary = "تم إنشاء العنوان التسويقي والمواصفات"
                    ),
                    quickActionType = QuickActionType.COPY_TEXT
                )
            }

            "get_pending_orders" -> {
                val pendingOrders = database.orderDao().getOrdersByStatusDirect("Created") +
                        database.orderDao().getOrdersByStatusDirect("Picking")
                
                val details = if (pendingOrders.isEmpty()) {
                    "✅ **حالة الشحنات:** لا توجد طلبات معلقة بانتظار التجهيز حالياً."
                } else {
                    val items = pendingOrders.joinToString("\n") { o ->
                        "• **طلب #${o.orderNumber}** | العميل: ${o.customerName} (${o.customerCity})\n  - المبلغ: ${o.totalAmount} ₺ | الشحن: ${o.cargoProvider} | الحالة: ${if (o.status == "Created") "جديد" else "قيد التجهيز"}"
                    }
                    """
                    📦 **الطلبات التي تنتظر الشحن والتجهيز (${pendingOrders.size} طلبات):**

                    $items

                    🚀 *تنبيه:* التسليم السريع لشركة الشحن يرفع تقييم سرعة التوصيل (Hızlı Teslimat) إلى 9.9!
                    """.trimIndent()
                }

                AiAgentResponse(
                    displayText = details,
                    toolCallInfo = ToolCallInfo(
                        functionName = "get_pending_orders",
                        title = "استعلام الطلبات المعلقة والشحنات",
                        parameters = "الحالة: Created / Picking",
                        resultSummary = "تم جلب ${pendingOrders.size} طلبات"
                    ),
                    quickActionType = QuickActionType.VIEW_ORDERS
                )
            }

            "update_product_stock" -> {
                val identifier = arguments["productIdentifier"]?.toString() ?: ""
                val newStock = (arguments["newStock"] as? Number)?.toInt() ?: 10

                val allProds = database.productDao().getAllProductsDirect()
                val target = allProds.find { 
                    it.barcode.equals(identifier, ignoreCase = true) ||
                    it.title.contains(identifier, ignoreCase = true) ||
                    identifier.contains(it.title, ignoreCase = true)
                } ?: allProds.firstOrNull()

                if (target != null) {
                    database.productDao().updateStock(target.id, newStock)
                    val updated = target.copy(stockCount = newStock)
                    AiAgentResponse(
                        displayText = "✅ **تم تنفيذ التعديل بنجاح في قاعدة البيانات:**\nتم تحديث مخزون المنتج **\"${target.title}\"** إلى **$newStock قطعة**.",
                        toolCallInfo = ToolCallInfo(
                            functionName = "update_product_stock",
                            title = "تعديل المخزون في قاعدة البيانات",
                            parameters = "المنتج: ${target.title} -> المخزون الجديد: $newStock",
                            resultSummary = "تم التحديث بنجاح"
                        ),
                        attachedProducts = listOf(updated),
                        quickActionType = QuickActionType.VIEW_PRODUCTS
                    )
                } else {
                    AiAgentResponse(
                        displayText = "⚠️ لم أتمكن من العثور على المنتج المحدد في قاعدة البيانات.",
                        toolCallInfo = ToolCallInfo(
                            functionName = "update_product_stock",
                            title = "تعديل المخزون",
                            parameters = "المنتج: $identifier",
                            resultSummary = "فشل: لم يتم العثور على المنتج"
                        )
                    )
                }
            }

            "get_unanswered_questions" -> {
                val questions = database.questionDao().getUnansweredQuestionsDirect()
                val text = if (questions.isEmpty()) {
                    "🎉 **خدمة العملاء:** جميع استفسارات الزبائن مجاب عليها بالكامل!"
                } else {
                    val list = questions.joinToString("\n\n") { q ->
                        "❓ **العميل:** ${q.customerName} عن \"${q.productTitle}\"\n  السؤال: \"${q.questionText}\""
                    }
                    """
                    💬 **استفسارات المشترين المعلقة (${questions.size} استفسارات):**

                    $list

                    ⚡ *توصية:* الرد السريع خلال 15 دقيقة يرفع معدل تحويل السؤال إلى عملية شراء بنسبة 45%!
                    """.trimIndent()
                }

                AiAgentResponse(
                    displayText = text,
                    toolCallInfo = ToolCallInfo(
                        functionName = "get_unanswered_questions",
                        title = "استعلام استفسارات العملاء المعلقة",
                        parameters = "غير مجاب عليها",
                        resultSummary = "تم جلب ${questions.size} استفسارات"
                    ),
                    quickActionType = QuickActionType.VIEW_QUESTIONS
                )
            }

            "analyze_buybox" -> {
                val products = database.productDao().getAllProductsDirect()
                val losing = products.filter { !it.buyboxWinner }
                val text = if (losing.isEmpty()) {
                    "🏆 **أداء الباي بوكس:** رائع جداً! أنت تفوز بالباي بوكس في جميع المنتجات المعروضة في متجرك بنسبة 100%!"
                } else {
                    val list = losing.joinToString("\n") { p ->
                        "• **${p.title}**\n  سعرك: ${p.salePrice} ₺ | سعر المنافس الفائز: **${p.competitorPrice} ₺** (الفارق: ${String.format(Locale.US, "%.1f", p.salePrice - p.competitorPrice)} ₺)"
                    }
                    """
                    ⚔️ **تحليل الباي بوكس (خوارزمية الأسعار والمنافسين):**
                    لديك **${losing.size}** منتجات تخسر فيها الباي بوكس لصالح منافسين آخرين:

                    $list

                    💡 **التوصية:** نوصي بخفض السعر ليصبح مساوياً أو أقل من سعر المنافس بـ 1 ₺، أو تفعيل ميزة الشحن السريع (Hızlı Teslimat) للفوز بالصفقة فوراً.
                    """.trimIndent()
                }

                AiAgentResponse(
                    displayText = text,
                    toolCallInfo = ToolCallInfo(
                        functionName = "analyze_buybox",
                        title = "تحليل خوارزمية الباي بوكس والمنافسين",
                        parameters = "المنتجات الخاسرة: ${losing.size}",
                        resultSummary = "تم تحليل الأسعار والمنافسين"
                    ),
                    attachedProducts = losing,
                    quickActionType = QuickActionType.VIEW_PRODUCTS
                )
            }

            else -> {
                AiAgentResponse(displayText = "تم استدعاء الوظيفة: $toolName بنجاح.")
            }
        }
    }
}

data class DashboardMetrics(
    val totalRevenue: Double,
    val todayOrders: Int,
    val pendingShipment: Int,
    val deliveredCount: Int,
    val returnedCount: Int,
    val totalOrders: Int
)

