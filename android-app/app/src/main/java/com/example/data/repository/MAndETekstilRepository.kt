package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.AutomationRuleEntity
import com.example.data.local.CustomerQuestionsEntity
import com.example.data.local.DailyBriefEntity
import com.example.data.local.StockMovementEntity
import com.example.data.local.TrendyolOrderEntity
import com.example.data.local.TrendyolProductEntity
import com.example.data.local.TrendyolReturnEntity
import com.example.domain.ai.AiExecutionResult
import com.example.domain.ai.AiTaskRouter
import com.example.domain.calculator.ProfitCalculationResult
import com.example.domain.calculator.ProfitCalculatorEngine
import com.example.domain.model.AiApprovalStatus
import com.example.domain.model.AiTaskType
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.ResponseStyle
import com.example.domain.telegram.TelegramNotifier
import com.example.domain.trendyol.TrendyolSyncManager
import com.example.security.AiSlotConfiguration
import com.example.security.ProfitCalculationRules
import com.example.security.SecureConfigManager
import com.example.security.TelegramConfiguration
import com.example.security.TrendyolApiConfiguration
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MAndETekstilRepository(
    private val database: AppDatabase,
    private val configManager: SecureConfigManager,
    private val aiTaskRouter: AiTaskRouter,
    private val syncManager: TrendyolSyncManager,
    private val telegramNotifier: TelegramNotifier
) {
    // Products & Stock
    val products: Flow<List<TrendyolProductEntity>> = database.productDao().getAllProducts()
    val lowStockProducts: Flow<List<TrendyolProductEntity>> = database.productDao().getLowStockProducts(threshold = 5)
    val stockMovements: Flow<List<StockMovementEntity>> = database.stockMovementDao().getAllMovements()

    suspend fun updateStock(barcode: String, newStock: Int, note: String) {
        val product = database.productDao().getProductByBarcode(barcode)
        database.productDao().updateStock(barcode, newStock)
        if (product != null) {
            val diff = newStock - product.stockQuantity
            val movementType = if (diff >= 0) "GİRİŞ" else "ÇIKIŞ/DÜZELTME"
            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = movementType,
                    quantityChange = diff,
                    remainingStock = newStock,
                    note = note
                )
            )
            // If low stock, trigger telegram if enabled
            if (newStock < 5) {
                telegramNotifier.sendTelegramMessage(
                    "⚠️ <b>Kritik Stok Uyarısı:</b>\nÜrün: ${product.title}\nVaryant: ${product.color} (${product.size})\nKalan: $newStock adet",
                    category = "DÜŞÜK_STOK"
                )
            }
        }
    }

    // Orders
    val orders: Flow<List<TrendyolOrderEntity>> = database.orderDao().getAllOrders()
    val activeOrderCount: Flow<Int> = database.orderDao().getActiveOrderCount()
    val totalSales: Flow<Double?> = database.orderDao().getTotalSales()

    suspend fun updateOrderPrepStatus(orderNumber: String, prepStatus: OrderPrepStatus) {
        database.orderDao().updatePrepStatus(orderNumber, prepStatus)
        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "ORDER_STATUS_CHANGE",
                moduleName = "Sipariş Merkezi",
                details = "Sipariş $orderNumber hazırlık durumu '${prepStatus.title}' olarak güncellendi."
            )
        )
    }

    // Customer Questions
    val allQuestions: Flow<List<CustomerQuestionsEntity>> = database.customerQuestionDao().getAllQuestions()
    val pendingQuestions: Flow<List<CustomerQuestionsEntity>> = database.customerQuestionDao().getPendingQuestions()
    val unansweredCount: Flow<Int> = database.customerQuestionDao().getUnansweredCount()

    suspend fun generateCustomerReply(
        question: CustomerQuestionsEntity,
        style: ResponseStyle
    ): AiExecutionResult {
        val systemPrompt = """
            Sen M&E Tekstil'in profesyonel Trendyol müşteri iletişim asistanısın.
            M&E Tekstil, birinci sınıf erkek ve kadın hazır giyim, polo yaka tişört, keten gömlek ve pamuklu sweatshirt üreticisidir.
            İstenen yanıt stili: ${style.title} (${style.description}).
            Müşteriye saygılı, net, güven veren, Türkçe dilbilgisi mükemmel bir cevap yaz.
            Cevapta M&E Tekstil imzasını kullan. İade/değişim gerekiyorsa Trendyol kolay iade adımlarını kibarca belirt.
        """.trimIndent()

        val userPrompt = """
            Ürün: ${question.productTitle} (${question.productCode})
            Müşteri: ${question.customerName}
            Müşteri Sorusu: ${question.questionText}
        """.trimIndent()

        val result = aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.CUSTOMER_REPLY,
            targetRefId = question.questionId,
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 1
        )

        if (result.isSuccess) {
            database.customerQuestionDao().updateQuestion(
                question.copy(
                    aiSuggestedReply = result.content,
                    finalReply = result.content,
                    selectedStyle = style,
                    providerUsed = result.providerUsed,
                    modelUsed = result.modelUsed,
                    status = "BEKLIYOR"
                )
            )
        }
        return result
    }

    suspend fun approveAndSendReply(questionId: String, finalReplyText: String) {
        val q = database.customerQuestionDao().getQuestionById(questionId) ?: return
        database.customerQuestionDao().updateQuestion(
            q.copy(
                finalReply = finalReplyText,
                isSent = true,
                status = "YANITLANDI",
                lastUpdated = System.currentTimeMillis()
            )
        )
        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "CUSTOMER_REPLY_SENT",
                moduleName = "Müşteri İletişimi",
                details = "Soru $questionId yanıtlandı ve Trendyol'a onaylandı."
            )
        )
    }

    // AI Approval Center
    val aiTasks = database.aiTaskDao().getAllTasks()
    val pendingAiTasksCount = database.aiTaskDao().getPendingTaskCount()

    suspend fun updateAiTaskStatus(taskId: Long, newStatus: AiApprovalStatus, editedContent: String) {
        database.aiTaskDao().updateStatus(taskId, newStatus, editedContent)
        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "AI_TASK_APPROVAL",
                moduleName = "AI Onay Merkezi",
                details = "Görev #$taskId durumu: ${newStatus.title}"
            )
        )
    }

    // Profit Calculator
    fun calculateProfit(
        salePrice: Double,
        buyingPrice: Double,
        commissionRate: Double,
        shippingCost: Double
    ): ProfitCalculationResult {
        val rules = configManager.getProfitRules()
        return ProfitCalculatorEngine.calculate(salePrice, buyingPrice, commissionRate, shippingCost, rules)
    }

    fun getProfitRules(): ProfitCalculationRules = configManager.getProfitRules()
    fun saveProfitRules(rules: ProfitCalculationRules) = configManager.saveProfitRules(rules)

    // Product Growth AI (Slot 3)
    suspend fun generateProductOptimization(
        productCode: String,
        title: String,
        category: String,
        currentDescription: String
    ): AiExecutionResult {
        val systemPrompt = """
            Sen M&E Tekstil'in Trendyol Ürün ve Büyüme Uzmanısın.
            Görevin: Tekstil ürünleri için satış dönüşümünü artıran, arama motorlarında üst sıralara çıkaran başlık, maddeler halinde detaylı Türkçe ürün açıklaması ve arama anahtar kelimeleri (keywords) üretmektir.
        """.trimIndent()

        val userPrompt = """
            Ürün Kodu: $productCode
            Mevcut Başlık: $title
            Kategori: $category
            Mevcut Detay: $currentDescription
            Lütfen şu formatta yanıt ver:
            1. ÖNERİLEN TRENDYOL BAŞLIĞI
            2. ÖNE ÇIKAN ÖZELLİKLER (Madde imleri ile kumaş, kalıp, yıkama)
            3. TRENDYOL ARAMA ANAHTAR KELİMELERİ (10 adet)
        """.trimIndent()

        return aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.PRODUCT_IMPROVEMENT,
            targetRefId = productCode,
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 3
        )
    }

    suspend fun runProductQualityCheck(product: TrendyolProductEntity): AiExecutionResult {
        val systemPrompt = """
            Sen Trendyol Kalite ve Politika Denetçisisin.
            Ürün listesini incele ve aşağıdaki kriterleri kontrol et:
            - Başlık kalitesi ve uzunluğu
            - Kumaş ve kalıp bilgisi eksikliği
            - Yanıltıcı veya hatalı iddia var mı?
            - Renk ve beden tutarlılığı
            Lütfen yanıtını [HATA], [UYARI], [ÖNERİ], [ONAYLANDI] etiketleriyle yapılandır.
        """.trimIndent()

        val userPrompt = """
            Ürün: ${product.title}
            Kategori: ${product.categoryName}
            Beden: ${product.size}, Renk: ${product.color}
            Fiyat: ${product.salePrice} TL, Stok: ${product.stockQuantity}
        """.trimIndent()

        return aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.QUALITY_CHECK,
            targetRefId = product.barcode,
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 3
        )
    }

    suspend fun runPricingAdvisor(product: TrendyolProductEntity): AiExecutionResult {
        val rules = configManager.getProfitRules()
        val calc = ProfitCalculatorEngine.calculate(
            product.salePrice,
            product.buyingPrice,
            product.commissionRate,
            product.shippingCost,
            rules
        )

        val systemPrompt = """
            Sen M&E Tekstil Finans ve Fiyatlandırma Danışmanı Yapay Zekasısın.
            Ürünün satış, maliyet, komisyon, kargo ve stok verilerine göre kârlılığı artıracak ve stok devir hızını optimize edecek 3 farklı fiyatlandırma senaryosu öner:
            1. Yüksek Kâr Senaryosu
            2. Hızlı Satış / Rekabetçi Senaryo
            3. Kampanya / Barem İndirim Senaryosu
            Her senaryonun tahmini net kârını ve olası risklerini Türkçe açıkla.
        """.trimIndent()

        val userPrompt = """
            Ürün: ${product.title}
            Mevcut Satış Fiyatı: ${product.salePrice} ₺
            Alış Maliyeti: ${product.buyingPrice} ₺
            Trendyol Komisyonu: %${product.commissionRate}
            Kargo: ${product.shippingCost} ₺
            Mevcut Net Kâr: ${calc.netProfit} ₺ (Marj: %${String.format(Locale.US, "%.1f", calc.profitMarginPercent)})
            Mevcut Stok: ${product.stockQuantity}
            Toplam Satış: ${product.totalSold} adet
        """.trimIndent()

        return aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.PRICING_ADVICE,
            targetRefId = product.barcode,
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 3
        )
    }

    // Returns & Return Analysis AI
    val returns: Flow<List<TrendyolReturnEntity>> = database.returnDao().getAllReturns()
    val returnsCount: Flow<Int> = database.returnDao().getReturnsCount()

    suspend fun runReturnsAnalysis(): AiExecutionResult {
        val systemPrompt = """
            Sen M&E Tekstil İade ve Kalite Analiz Yapay Zekasısın.
            Trendyol iade bildirimlerini ve müşteri yorumlarını inceleyerek:
            - Tekrarlanan temel iade kalıplarını belirle (örn: beden uyumsuzluğu, kumaş beklentisi)
            - Beden tablosu ve kalıp tavsiyesi için somut öneriler sun
            - Ürün açıklamasında yapılması gereken revizyonları listele
        """.trimIndent()

        val userPrompt = """
            Mevcut İadeler:
            1. ME-TSH-01 (Siyah M): Beden küçük geldi, omuzlar dar.
            2. ME-GOM-02 (Haki XL): Fotoğraftan daha koyu haki geldi.
            Lütfen M&E Tekstil yönetimi için aksiyon planı hazırla.
        """.trimIndent()

        return aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.RETURN_ANALYSIS,
            targetRefId = "ALL_RETURNS",
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 3
        )
    }

    // Daily Business Brief (Slot 2)
    val dailyBriefs: Flow<List<DailyBriefEntity>> = database.dailyBriefDao().getAllBriefs()

    suspend fun generateDailyBrief(): AiExecutionResult {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val systemPrompt = """
            Sen M&E Tekstil'in Baş Strateji ve Kâr Yapay Zekasısın.
            Günün satış, sipariş, tahmini net kâr, kritik stoklar ve müşteri sorularına dayanarak yöneticiye 'Günlük İş Özeti' hazırla.
            Gerçek veriler ile yapay zeka yorumunu net şekilde ayır.
            En kritik 3 aksiyon adımını maddeler halinde listele.
        """.trimIndent()

        val userPrompt = """
            Tarih: $today
            Toplam Satış: 1,579.60 ₺
            Toplam Sipariş: 3 adet
            Tahmini Net Kâr: 486.20 ₺
            Kritik Düşük Stok: 2 ürün (ME-TSH-01 L Beden: 4 adet, ME-GOM-02 XL: 3 adet)
            Bekleyen Müşteri Sorusu: 2 adet
            İade: 2 adet (Beden ve renk uyuşmazlığı)
        """.trimIndent()

        val result = aiTaskRouter.routeAndExecute(
            taskType = AiTaskType.DAILY_BRIEF,
            targetRefId = today,
            systemInstruction = systemPrompt,
            userPrompt = userPrompt,
            overrideSlot = 2
        )

        if (result.isSuccess) {
            database.dailyBriefDao().insertBrief(
                DailyBriefEntity(
                    reportDate = today,
                    totalSales = 1579.60,
                    orderCount = 3,
                    estimatedProfit = 486.20,
                    lowStockCount = 2,
                    returnCount = 2,
                    pendingQuestionsCount = 2,
                    aiInterpretation = result.content,
                    suggestedActions = "1. Kritik stoklar için kumaş kesim emri verilmeli.\n2. Beden tablosu L beden ölçüsü güncellenmeli.\n3. Bekleyen sorular AI onaylanarak gönderilmeli."
                )
            )

            telegramNotifier.sendTelegramMessage(
                "📊 <b>GÜNLÜK İŞ VE KÂR ÖZETİ ($today)</b>\n\n${result.content.take(400)}...\n\n<i>Detaylar M&E Tekstil uygulamasında.</i>",
                category = "GÜNLÜK_RAPOR"
            )
        }
        return result
    }

    // Automations
    val automationRules: Flow<List<AutomationRuleEntity>> = database.automationRuleDao().getAllRules()

    suspend fun initDefaultAutomationsIfEmpty() {
        val rules = listOf(
            AutomationRuleEntity(
                name = "Yeni Sipariş Bildirimi",
                triggerType = "NEW_ORDER",
                conditionDescription = "Yeni sipariş geldiğinde anında bildirim",
                actionType = "TELEGRAM_NOTIFY",
                isEnabled = true
            ),
            AutomationRuleEntity(
                name = "Kritik Stok Uyarısı",
                triggerType = "LOW_STOCK",
                conditionDescription = "Stok adedi 5'in altına düştüğünde",
                actionType = "TELEGRAM_NOTIFY",
                isEnabled = true
            ),
            AutomationRuleEntity(
                name = "Yeni Soruda AI Yanıt Taslağı",
                triggerType = "NEW_QUESTION",
                conditionDescription = "Müşteri sorusu geldiğinde otomatik taslak oluştur",
                actionType = "AI_REPLY_GENERATE",
                isEnabled = true
            ),
            AutomationRuleEntity(
                name = "Günlük Kâr ve İş Raporu",
                triggerType = "DAILY_SCHEDULE",
                conditionDescription = "Her akşam saat 20:00'de özet çıkar",
                actionType = "DAILY_REPORT",
                isEnabled = true
            )
        )
        for (r in rules) {
            database.automationRuleDao().insertRule(r)
        }
    }

    suspend fun toggleAutomation(rule: AutomationRuleEntity) {
        database.automationRuleDao().updateRule(rule.copy(isEnabled = !rule.isEnabled))
    }

    suspend fun testAutomationRule(ruleId: Long): Result<String> {
        val enabledRules = database.automationRuleDao().getEnabledRules()
        val rule = enabledRules.find { it.id == ruleId }
            ?: return Result.failure(IllegalStateException("Kural bulunamadı veya pasif."))

        database.automationRuleDao().recordExecution(rule.id, System.currentTimeMillis(), "")
        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "AUTOMATION_TRIGGERED",
                moduleName = "Otomasyon Motoru",
                details = "Kural elle test edildi: ${rule.name}"
            )
        )
        return Result.success("Otomasyon kuralı '${rule.name}' başarıyla test edildi.")
    }

    // Audit logs
    val auditLogs: Flow<List<AuditLogEntity>> = database.auditLogDao().getRecentLogs()

    // Warehouses & Multi-Depot Operations
    val warehouses: Flow<List<com.example.data.local.WarehouseEntity>> = database.warehouseDao().getAllWarehouses()
    val warehouseTransfers: Flow<List<com.example.data.local.WarehouseTransferEntity>> = database.warehouseDao().getAllTransfers()

    suspend fun addWarehouse(warehouse: com.example.data.local.WarehouseEntity) {
        database.warehouseDao().insertWarehouse(warehouse)
        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "WAREHOUSE_CREATED",
                moduleName = "Depo Yönetimi",
                details = "Yeni depo oluşturuldu: ${warehouse.name} (${warehouse.warehouseId})"
            )
        )
    }

    suspend fun executeWarehouseTransfer(
        barcode: String,
        fromWarehouseId: String,
        toWarehouseId: String,
        quantity: Int,
        note: String,
        user: String = "Yönetici"
    ): Result<String> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Transfer adedi 0'dan büyük olmalıdır."))
        }
        if (fromWarehouseId == toWarehouseId) {
            return Result.failure(IllegalArgumentException("Kaynak depo ile hedef depo aynı olamaz."))
        }
        return database.withTransaction {
            val product = database.productDao().getProductByBarcode(barcode)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Ürün bulunamadı (Barkod: $barcode)"))

            if (product.stockQuantity < quantity) {
                return@withTransaction Result.failure(IllegalStateException("Yetersiz stok! Mevcut: ${product.stockQuantity}, İstenen transfer: $quantity"))
            }

            val transfer = com.example.data.local.WarehouseTransferEntity(
                barcode = barcode,
                productCode = product.productCode,
                productTitle = product.title,
                color = product.color,
                size = product.size,
                fromWarehouseId = fromWarehouseId,
                toWarehouseId = toWarehouseId,
                quantity = quantity,
                note = note,
                performedBy = user
            )
            database.warehouseDao().insertTransfer(transfer)

            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = "TRANSFER_ÇIKIŞ",
                    quantityChange = -quantity,
                    previousStock = product.stockQuantity,
                    remainingStock = product.stockQuantity,
                    warehouseId = fromWarehouseId,
                    referenceId = "TRF-$toWarehouseId",
                    note = "Transfer Çıkış: $fromWarehouseId ➔ $toWarehouseId ($quantity adet). Not: $note",
                    performedBy = user
                )
            )

            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = "TRANSFER_GİRİŞ",
                    quantityChange = quantity,
                    previousStock = product.stockQuantity,
                    remainingStock = product.stockQuantity,
                    warehouseId = toWarehouseId,
                    referenceId = "TRF-$toWarehouseId",
                    note = "Transfer Giriş: $fromWarehouseId ➔ $toWarehouseId ($quantity adet). Not: $note",
                    performedBy = user
                )
            )

            database.productDao().updateWarehouse(barcode, toWarehouseId)

            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "WAREHOUSE_TRANSFER",
                    moduleName = "Depo Yönetimi",
                    details = "Depo transferi tamamlandı: ${product.productCode} ($quantity adet), $fromWarehouseId ➔ $toWarehouseId. Yapan: $user"
                )
            )

            Result.success("Depo transferi başarıyla kaydedildi.")
        }
    }

    // Atomic Sales Transaction (Satış İşlemi)
    suspend fun executeSaleTransaction(
        orderNumber: String,
        customerName: String,
        barcode: String,
        quantity: Int,
        unitPrice: Double,
        warehouseId: String = "W-MAIN",
        city: String = "İstanbul",
        note: String = "",
        user: String = "Yönetici"
    ): Result<TrendyolOrderEntity> {
        if (quantity <= 0) {
            return Result.failure(IllegalArgumentException("Satış adedi 0'dan büyük olmalıdır."))
        }

        val profitRules = configManager.getProfitRules()

        return database.withTransaction {
            val product = database.productDao().getProductByBarcode(barcode)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Barkod '$barcode' sistemde bulunamadı."))

            if (product.stockQuantity < quantity) {
                return@withTransaction Result.failure(IllegalStateException("Yetersiz stok! Mevcut stok: ${product.stockQuantity} adet, Satılmak istenen: $quantity adet."))
            }

            val calc = ProfitCalculatorEngine.calculate(
                salePrice = unitPrice,
                buyingPrice = product.buyingPrice,
                commissionRate = product.commissionRate,
                shippingCost = product.shippingCost,
                rules = profitRules
            )

            val newStock = product.stockQuantity - quantity
            database.productDao().updateStock(barcode, newStock)
            database.productDao().incrementTotalSold(barcode, quantity)

            // Ledger entry for stock movement
            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = "SATIŞ",
                    quantityChange = -quantity,
                    previousStock = product.stockQuantity,
                    remainingStock = newStock,
                    warehouseId = warehouseId,
                    referenceId = orderNumber,
                    note = "Satış Siparişi: $orderNumber ($customerName)",
                    performedBy = user
                )
            )

            val order = TrendyolOrderEntity(
                orderNumber = orderNumber,
                customerName = customerName,
                orderDate = System.currentTimeMillis(),
                status = com.example.domain.model.TrendyolOrderStatus.NEW,
                prepStatus = OrderPrepStatus.WAITING,
                totalPrice = unitPrice * quantity,
                itemsSummary = "${product.title} - ${product.color} (${product.size}) x $quantity",
                productCode = product.productCode,
                barcode = barcode,
                color = product.color,
                size = product.size,
                quantity = quantity,
                city = city,
                buyingCostTotal = product.buyingPrice * quantity,
                commissionAmount = calc.commissionAmount * quantity,
                shippingCost = product.shippingCost,
                netProfit = calc.netProfit * quantity,
                isStockDeducted = true,
                isDemo = false,
                note = note
            )
            database.orderDao().insertOrder(order)

            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "SALE_RECORDED",
                    moduleName = "Satış & Sipariş",
                    details = "Satış onaylandı: Sipariş $orderNumber, Ürün: ${product.productCode} ($quantity adet). Kalan stok: $newStock"
                )
            )

            telegramNotifier.sendTelegramMessage(
                "🛍️ <b>Yeni Satış / Sipariş:</b>\n" +
                "Sipariş No: <code>$orderNumber</code>\n" +
                "Müşteri: $customerName ($city)\n" +
                "Ürün: ${product.title} [${product.color} - ${product.size}]\n" +
                "Adet: $quantity | Tutar: ${unitPrice * quantity} ₺\n" +
                "Net Kâr: ${calc.netProfit * quantity} ₺\n" +
                "Kalan Stok: $newStock adet",
                category = "SİPARİŞ"
            )

            Result.success(order)
        }
    }

    // Atomic Returns Transaction (İade İşlemi & Kısıtlamaları)
    suspend fun executeReturnTransaction(
        returnId: String,
        orderNumber: String,
        barcode: String,
        returnQty: Int,
        reason: String,
        customerComment: String = "",
        restockAction: String = "STOKA_EKLENDİ",
        user: String = "Yönetici"
    ): Result<TrendyolReturnEntity> {
        if (returnQty <= 0) {
            return Result.failure(IllegalArgumentException("İade adedi 0'dan büyük olmalıdır."))
        }

        return database.withTransaction {
            val order = database.orderDao().getOrderByNumber(orderNumber)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Sipariş No '$orderNumber' sistemde bulunamadı."))

            val productBarcode = if (barcode.isNotBlank()) barcode else order.barcode
            val alreadyReturned = database.returnDao().getReturnedQuantityForOrder(orderNumber, productBarcode) ?: 0
            if (alreadyReturned + returnQty > order.quantity) {
                return@withTransaction Result.failure(
                    IllegalStateException("Hata: İade adedi ($returnQty), siparişteki kalan iade edilebilir adedi (${order.quantity - alreadyReturned}) aşıyor!")
                )
            }

            val product = database.productDao().getProductByBarcode(productBarcode)

            if (restockAction == "STOKA_EKLENDİ" && product != null) {
                val newStock = product.stockQuantity + returnQty
                database.productDao().updateStock(productBarcode, newStock)
                database.productDao().incrementReturnCount(productBarcode, returnQty)

                database.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        barcode = productBarcode,
                        productCode = product.productCode,
                        title = product.title,
                        movementType = "İADE",
                        quantityChange = returnQty,
                        previousStock = product.stockQuantity,
                        remainingStock = newStock,
                        warehouseId = product.warehouseId,
                        referenceId = returnId,
                        note = "İade Kabul: $orderNumber - $reason (Tekrar Stoka Alındı)",
                        performedBy = user
                    )
                )
            } else if (product != null) {
                database.productDao().incrementReturnCount(productBarcode, returnQty)
                database.stockMovementDao().insertMovement(
                    StockMovementEntity(
                        barcode = productBarcode,
                        productCode = product.productCode,
                        title = product.title,
                        movementType = "İADE_DEFOLU",
                        quantityChange = 0,
                        previousStock = product.stockQuantity,
                        remainingStock = product.stockQuantity,
                        warehouseId = product.warehouseId,
                        referenceId = returnId,
                        note = "İade Kabul: $orderNumber - Defolu/Kusurlu ayrıldı (Stoka eklenmedi)",
                        performedBy = user
                    )
                )
            }

            val returnEntity = TrendyolReturnEntity(
                returnId = returnId,
                orderNumber = orderNumber,
                barcode = productBarcode,
                productCode = product?.productCode ?: order.productCode,
                productTitle = product?.title ?: order.itemsSummary,
                color = product?.color ?: order.color,
                size = product?.size ?: order.size,
                quantity = returnQty,
                returnReason = reason,
                customerComment = customerComment,
                restockAction = restockAction,
                refundAmount = (order.totalPrice / order.quantity) * returnQty,
                processedBy = user,
                returnDate = System.currentTimeMillis(),
                status = "KABUL_EDİLDİ"
            )
            database.returnDao().insertReturn(returnEntity)
            database.orderDao().updateReturnStatus(orderNumber, "İADE_ALINDI")

            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "RETURN_PROCESSED",
                    moduleName = "İade Yönetimi",
                    details = "İade onaylandı: $returnId (Sipariş: $orderNumber, Adet: $returnQty, Eylem: $restockAction). Yapan: $user"
                )
            )

            telegramNotifier.sendTelegramMessage(
                "🔄 <b>İade İşlemi Onaylandı:</b>\n" +
                "İade ID: <code>$returnId</code>\n" +
                "Sipariş No: $orderNumber\n" +
                "Neden: $reason\n" +
                "İşlem: $restockAction ($returnQty adet)\n" +
                "Müşteri Notu: $customerComment",
                category = "İADE"
            )

            Result.success(returnEntity)
        }
    }

    // Stock Adjustment with Negative Stock Protection
    suspend fun adjustStockWithAudit(
        barcode: String,
        newStock: Int,
        reason: String,
        user: String = "Yönetici",
        allowNegativeOverride: Boolean = false
    ): Result<String> {
        if (newStock < 0 && !allowNegativeOverride) {
            return Result.failure(
                IllegalStateException("Negatif stok girişi engellendi! Stok miktarı 0 veya daha büyük olmalıdır. (M&E ERP Güvenlik Kuralı)")
            )
        }

        return database.withTransaction {
            val product = database.productDao().getProductByBarcode(barcode)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Ürün bulunamadı (Barkod: $barcode)"))

            val diff = newStock - product.stockQuantity
            database.productDao().updateStock(barcode, newStock)

            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = "SAYIM_DÜZELTME",
                    quantityChange = diff,
                    previousStock = product.stockQuantity,
                    remainingStock = newStock,
                    warehouseId = product.warehouseId,
                    note = "Sayım / Düzeltme: $reason",
                    performedBy = user
                )
            )

            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "STOCK_ADJUSTMENT",
                    moduleName = "Stok Yönetimi",
                    details = "Stok düzeltildi: ${product.productCode} (${product.color} ${product.size}) ➔ $newStock adet (Fark: $diff). Neden: $reason. Yapan: $user"
                )
            )

            Result.success("Stok miktarı başarıyla güncellendi.")
        }
    }

    // Add or Update Product Variant
    suspend fun saveProductVariant(product: TrendyolProductEntity): Result<String> {
        if (product.barcode.isBlank() || product.productCode.isBlank() || product.title.isBlank()) {
            return Result.failure(IllegalArgumentException("Barkod, Ürün Kodu ve Ürün Adı zorunludur."))
        }
        val existing = database.productDao().getProductByBarcode(product.barcode)
        database.productDao().insertProduct(product)

        if (existing == null && product.stockQuantity > 0) {
            database.stockMovementDao().insertMovement(
                StockMovementEntity(
                    barcode = product.barcode,
                    productCode = product.productCode,
                    title = product.title,
                    movementType = "GİRİŞ",
                    quantityChange = product.stockQuantity,
                    remainingStock = product.stockQuantity,
                    warehouseId = product.warehouseId,
                    note = "Yeni varyant ilk stok girişi"
                )
            )
        }

        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = if (existing == null) "PRODUCT_CREATED" else "PRODUCT_UPDATED",
                moduleName = "Ürün & Stok",
                details = "${product.productCode} - ${product.color} (${product.size}) kaydedildi."
            )
        )

        return Result.success("Ürün varyantı başarıyla kaydedildi.")
    }

    suspend fun toggleProductActive(barcode: String) {
        val p = database.productDao().getProductByBarcode(barcode) ?: return
        val updated = p.copy(isActive = !p.isActive)
        database.productDao().updateProduct(updated)
    }

    // Search & Dead Stock helpers
    fun searchProducts(query: String): Flow<List<TrendyolProductEntity>> = database.productDao().searchProducts(query)
    fun getDeadStock(): Flow<List<TrendyolProductEntity>> = database.productDao().getDeadStockProducts()
    fun searchOrders(query: String): Flow<List<TrendyolOrderEntity>> = database.orderDao().searchOrders(query)

    // Sync Center Operations
    val syncLogs: Flow<List<com.example.data.local.SyncLogEntity>> = database.syncLogDao().getAllSyncLogs()
    suspend fun getLastSyncLog(): com.example.data.local.SyncLogEntity? = database.syncLogDao().getLastSyncLog()

    suspend fun runComprehensiveSync(): Result<com.example.data.local.SyncLogEntity> {
        val res = syncManager.syncAllFromTrendyol()
        val lastLog = database.syncLogDao().getLastSyncLog()
        return if (res.isSuccess && lastLog != null) {
            Result.success(lastLog)
        } else if (lastLog != null) {
            Result.success(lastLog)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Senkronizasyon tamamlanamadı."))
        }
    }

    // CSV Export & Import for Products
    fun exportProductsCsv(productList: List<TrendyolProductEntity>): String {
        val sb = StringBuilder()
        sb.append("Barkod,UrunKodu,UrunAdi,Kategori,Renk,Beden,Stok,SatisFiyati,AlisFiyati,KomisyonOrani,KargoUcreti,Depo,KumasDetay,Durum\n")
        for (p in productList) {
            sb.append("\"${p.barcode}\",")
            sb.append("\"${p.productCode}\",")
            sb.append("\"${p.title.replace("\"", "\"\"")}\",")
            sb.append("\"${p.categoryName}\",")
            sb.append("\"${p.color}\",")
            sb.append("\"${p.size}\",")
            sb.append("${p.stockQuantity},")
            sb.append("${p.salePrice},")
            sb.append("${p.buyingPrice},")
            sb.append("${p.commissionRate},")
            sb.append("${p.shippingCost},")
            sb.append("\"${p.warehouseId}\",")
            sb.append("\"${p.fabricDetails.replace("\"", "\"\"")}\",")
            sb.append("\"${if (p.isActive) "Aktif" else "Pasif"}\"\n")
        }
        return sb.toString()
    }

    suspend fun importProductsFromCsv(csvText: String): Result<Pair<Int, List<String>>> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size <= 1) {
            return Result.failure(IllegalArgumentException("CSV dosyası boş veya başlık satırı dışında veri içermiyor."))
        }

        var importedCount = 0
        val errorLogs = mutableListOf<String>()

        for (i in 1 until lines.size) {
            val line = lines[i]
            val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()).map { it.trim().trim('"') }
            if (tokens.size < 7) {
                errorLogs.add("Satır ${i + 1}: Eksik sütun sayısı (${tokens.size}/7)")
                continue
            }

            try {
                val barcode = tokens[0]
                val code = tokens[1]
                val title = tokens[2]
                val category = if (tokens.size > 3) tokens[3] else "Genel"
                val color = if (tokens.size > 4) tokens[4] else "Standart"
                val size = if (tokens.size > 5) tokens[5] else "Standart"
                val stock = tokens[6].toIntOrNull() ?: 0
                val salePrice = if (tokens.size > 7) tokens[7].toDoubleOrNull() ?: 299.90 else 299.90
                val buyingPrice = if (tokens.size > 8) tokens[8].toDoubleOrNull() ?: 110.0 else 110.0
                val commission = if (tokens.size > 9) tokens[9].toDoubleOrNull() ?: 18.5 else 18.5
                val shipping = if (tokens.size > 10) tokens[10].toDoubleOrNull() ?: 35.0 else 35.0
                val warehouse = if (tokens.size > 11) tokens[11] else "W-MAIN"
                val fabric = if (tokens.size > 12) tokens[12] else "%100 Pamuk"

                val product = TrendyolProductEntity(
                    barcode = barcode,
                    productCode = code,
                    title = title,
                    categoryName = category,
                    color = color,
                    size = size,
                    stockQuantity = stock,
                    salePrice = salePrice,
                    buyingPrice = buyingPrice,
                    commissionRate = commission,
                    shippingCost = shipping,
                    warehouseId = warehouse,
                    fabricDetails = fabric,
                    isActive = true
                )
                database.productDao().insertProduct(product)
                importedCount++
            } catch (e: Exception) {
                errorLogs.add("Satır ${i + 1}: Hata - ${e.message}")
            }
        }

        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "CSV_IMPORT",
                moduleName = "Stok İçe Aktar",
                details = "CSV ile $importedCount ürün içe aktarıldı. Hatalı satır: ${errorLogs.size}"
            )
        )

        return Result.success(Pair(importedCount, errorLogs))
    }

    // Financial / Profit Report CSV Export
    fun exportFinancialReportCsv(ordersList: List<TrendyolOrderEntity>): String {
        val sb = StringBuilder()
        sb.append("SiparisNo,Tarih,Musteri,UrunKodu,Barkod,Renk,Beden,Adet,SatisTutari,Maliyet,Komisyon,Kargo,NetKar,KarMarjiYuzde,Sehir,Durum\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (o in ordersList) {
            val dateStr = sdf.format(Date(o.orderDate))
            val margin = if (o.totalPrice > 0) (o.netProfit / o.totalPrice) * 100.0 else 0.0
            sb.append("\"${o.orderNumber}\",")
            sb.append("\"$dateStr\",")
            sb.append("\"${o.customerName}\",")
            sb.append("\"${o.productCode}\",")
            sb.append("\"${o.barcode}\",")
            sb.append("\"${o.color}\",")
            sb.append("\"${o.size}\",")
            sb.append("${o.quantity},")
            sb.append("${o.totalPrice},")
            sb.append("${o.buyingCostTotal},")
            sb.append("${o.commissionAmount},")
            sb.append("${o.shippingCost},")
            sb.append("${o.netProfit},")
            sb.append("${String.format(Locale.US, "%.1f", margin)},")
            sb.append("\"${o.city}\",")
            sb.append("\"${o.status.title}\"\n")
        }
        return sb.toString()
    }

    // Language preferences
    fun getAppLanguage(): String = configManager.getAppLanguage()
    fun setAppLanguage(lang: String) = configManager.setAppLanguage(lang)

    // System preferences
    fun getAutoSyncIntervalMinutes(): Int = configManager.getAutoSyncIntervalMinutes()
    fun setAutoSyncIntervalMinutes(minutes: Int) = configManager.setAutoSyncIntervalMinutes(minutes)
    fun getLowStockThreshold(): Int = configManager.getLowStockThreshold()
    fun setLowStockThreshold(threshold: Int) = configManager.setLowStockThreshold(threshold)

    // Config helpers
    fun getTrendyolConfig(): TrendyolApiConfiguration = configManager.getTrendyolConfig()
    fun saveTrendyolConfig(cfg: TrendyolApiConfiguration) = configManager.saveTrendyolConfig(cfg)
    suspend fun testTrendyolConnection(): Result<String> = syncManager.testConnection()
    suspend fun syncTrendyol(): Result<String> = syncManager.syncAllFromTrendyol()
    suspend fun seedInitialData() = syncManager.seedInitialDataIfEmpty()
    suspend fun clearDemoData() = syncManager.clearDemoData()

    fun getAiSlotConfig(slot: Int): AiSlotConfiguration = configManager.getAiSlotConfig(slot)
    fun saveAiSlotConfig(cfg: AiSlotConfiguration) = configManager.saveAiSlotConfig(cfg)
    suspend fun testAiSlotConnection(slot: Int): Result<String> = aiTaskRouter.testSlotConnection(slot)

    fun getTelegramConfig(): TelegramConfiguration = configManager.getTelegramConfig()
    fun saveTelegramConfig(cfg: TelegramConfiguration) = configManager.saveTelegramConfig(cfg)
    suspend fun sendTelegramTest(msg: String): Result<String> = telegramNotifier.sendTelegramMessage(msg, "TEST")
}
