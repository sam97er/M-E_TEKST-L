package com.example.domain.trendyol

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.CustomerQuestionsEntity
import com.example.data.local.StockMovementEntity
import com.example.data.local.SyncLogEntity
import com.example.data.local.TrendyolOrderEntity
import com.example.data.local.TrendyolProductEntity
import com.example.data.local.TrendyolReturnEntity
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.ResponseStyle
import com.example.domain.model.TrendyolOrderStatus
import com.example.security.SecureConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TrendyolSyncManager(
    private val configManager: SecureConfigManager,
    private val database: AppDatabase
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val syncMutex = Mutex()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val config = configManager.getTrendyolConfig()
        if (config.sellerId.isBlank() || config.apiKey.isBlank() || config.apiSecret.isBlank()) {
            return@withContext Result.failure(
                IllegalArgumentException("Lütfen Trendyol Satıcı ID, API Key ve API Secret bilgilerini eksiksiz girin.")
            )
        }

        try {
            // Trendyol official test endpoint: GET /suppliers/{supplierId}/products?size=1
            val url = "https://api.trendyol.com/sapigw/suppliers/${config.sellerId}/products?size=1"
            val credential = Credentials.basic(config.apiKey, config.apiSecret)
            val request = Request.Builder()
                .url(url)
                .header("Authorization", credential)
                .header("User-Agent", "${config.sellerId} - SelfIntegration")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful || response.code == 200) {
                    configManager.updateTrendyolSyncStatus(true, "")
                    database.auditLogDao().insertLog(
                        AuditLogEntity(
                            actionType = "TRENDYOL_CONNECTION_SUCCESS",
                            moduleName = "Trendyol Entegrasyon",
                            details = "Trendyol API bağlantı testi başarılı. Satıcı ID: ${config.sellerId}"
                        )
                    )
                    Result.success("Trendyol API bağlantısı başarıyla kuruldu.")
                } else {
                    val errorMsg = when (response.code) {
                        401 -> "Yetkilendirme Hatası (401): API Key veya API Secret geçersiz."
                        403 -> "Yetkisiz Erişim (403): Satıcı ID'nizin bu API kaynağına yetkisi yok."
                        404 -> "Kaynak Bulunamadı (404): Satıcı ID veya API uç noktası mevcut değil."
                        429 -> "Hız Sınırı Aşıldı (429): Lütfen 1 dakika bekleyin."
                        in 500..599 -> "Trendyol Sunucu Hatası (${response.code})."
                        else -> "HTTP ${response.code}: ${response.message}"
                    }
                    configManager.updateTrendyolSyncStatus(false, errorMsg)
                    Result.failure(Exception("Trendyol Bağlantı Hatası: $errorMsg"))
                }
            }
        } catch (e: Exception) {
            val err = e.message ?: "Bağlantı zaman aşımına uğradı."
            configManager.updateTrendyolSyncStatus(false, err)
            Result.failure(Exception("Bağlantı hatası: $err"))
        }
    }

    /**
     * Ensures default warehouse structure exists without seeding fake products.
     */
    suspend fun ensureDefaultWarehouses() = withContext(Dispatchers.IO) {
        val sampleWarehouses = listOf(
            com.example.data.local.WarehouseEntity(
                warehouseId = "W-MAIN",
                name = "Merkez Depo",
                location = "Güngören / İstanbul",
                isDefault = true
            ),
            com.example.data.local.WarehouseEntity(
                warehouseId = "W-WORKSHOP",
                name = "Atölye & Üretim",
                location = "Merter / İstanbul",
                isDefault = false
            ),
            com.example.data.local.WarehouseEntity(
                warehouseId = "W-STORE",
                name = "Showroom & Mağaza",
                location = "Zeytinburnu / İstanbul",
                isDefault = false
            )
        )
        database.warehouseDao().insertWarehouses(sampleWarehouses)
    }

    /**
     * Initializes initial sample demo data for M&E Tekstil if explicitly requested by user.
     * All demo entities are clearly flagged with isDemo = true.
     */
    suspend fun seedInitialDataIfEmpty(forceDemo: Boolean = false) = withContext(Dispatchers.IO) {
        ensureDefaultWarehouses()
        val count = database.productDao().getProductCount()
        if (count > 0 && !forceDemo) return@withContext

        val sampleProducts = listOf(
            TrendyolProductEntity(
                barcode = "868200100101",
                productCode = "ME-TSH-01",
                title = "M&E Tekstil Erkek %100 Pamuklu Polo Yaka Tişört",
                categoryName = "Erkek Tişört",
                color = "Siyah",
                size = "M",
                stockQuantity = 14,
                salePrice = 299.90,
                buyingPrice = 110.00,
                commissionRate = 18.5,
                shippingCost = 35.0,
                trendyolStatus = "Satışta",
                totalSold = 42,
                returnCount = 2,
                isDemo = true
            ),
            TrendyolProductEntity(
                barcode = "868200100102",
                productCode = "ME-TSH-01",
                title = "M&E Tekstil Erkek %100 Pamuklu Polo Yaka Tişört",
                categoryName = "Erkek Tişört",
                color = "Siyah",
                size = "L",
                stockQuantity = 4, // Low stock!
                salePrice = 299.90,
                buyingPrice = 110.00,
                commissionRate = 18.5,
                shippingCost = 35.0,
                trendyolStatus = "Satışta",
                totalSold = 68,
                returnCount = 3,
                isDemo = true
            ),
            TrendyolProductEntity(
                barcode = "868200100103",
                productCode = "ME-TSH-01",
                title = "M&E Tekstil Erkek %100 Pamuklu Polo Yaka Tişört",
                categoryName = "Erkek Tişört",
                color = "Beyaz",
                size = "L",
                stockQuantity = 22,
                salePrice = 299.90,
                buyingPrice = 110.00,
                commissionRate = 18.5,
                shippingCost = 35.0,
                trendyolStatus = "Satışta",
                totalSold = 31,
                returnCount = 1,
                isDemo = true
            ),
            TrendyolProductEntity(
                barcode = "868200200201",
                productCode = "ME-GOM-02",
                title = "M&E Tekstil Regular Fit Keten Karışımlı Gömlek",
                categoryName = "Erkek Gömlek",
                color = "Haki",
                size = "XL",
                stockQuantity = 3, // Low stock!
                salePrice = 449.90,
                buyingPrice = 165.00,
                commissionRate = 19.0,
                shippingCost = 35.0,
                trendyolStatus = "Satışta",
                totalSold = 55,
                returnCount = 4,
                isDemo = true
            ),
            TrendyolProductEntity(
                barcode = "868200300301",
                productCode = "ME-SWT-03",
                title = "M&E Tekstil 3 İplik Şardonlu Kapüşonlu Sweatshirt",
                categoryName = "Sweatshirt",
                color = "Antrasit",
                size = "M",
                stockQuantity = 19,
                salePrice = 529.90,
                buyingPrice = 210.00,
                commissionRate = 19.5,
                shippingCost = 42.0,
                trendyolStatus = "Satışta",
                totalSold = 89,
                returnCount = 5,
                isDemo = true
            )
        )
        database.productDao().insertProducts(sampleProducts)

        // Initial orders
        val now = System.currentTimeMillis()
        val sampleOrders = listOf(
            TrendyolOrderEntity(
                orderNumber = "TY-8492019",
                customerName = "Ahmet Yılmaz",
                orderDate = now - 3600000 * 2,
                status = TrendyolOrderStatus.NEW,
                prepStatus = OrderPrepStatus.WAITING,
                totalPrice = 599.80,
                itemsSummary = "M&E Erkek Polo Yaka Tişört - Siyah (M) x 2",
                productCode = "ME-TSH-01",
                barcode = "868200100101",
                color = "Siyah",
                size = "M",
                quantity = 2,
                city = "İstanbul",
                isStockDeducted = true,
                isDemo = true,
                syncStatus = "SENKRONİZE"
            ),
            TrendyolOrderEntity(
                orderNumber = "TY-8491823",
                customerName = "Mehmet Demir",
                orderDate = now - 3600000 * 5,
                status = TrendyolOrderStatus.PREPARING,
                prepStatus = OrderPrepStatus.BEING_PREPARED,
                totalPrice = 449.90,
                itemsSummary = "M&E Regular Fit Keten Gömlek - Haki (XL) x 1",
                productCode = "ME-GOM-02",
                barcode = "868200200201",
                color = "Haki",
                size = "XL",
                quantity = 1,
                city = "Ankara",
                isStockDeducted = true,
                isDemo = true,
                syncStatus = "SENKRONİZE"
            ),
            TrendyolOrderEntity(
                orderNumber = "TY-8490451",
                customerName = "Canan Öztürk",
                orderDate = now - 3600000 * 18,
                status = TrendyolOrderStatus.SHIPPED,
                prepStatus = OrderPrepStatus.COMPLETED,
                totalPrice = 529.90,
                itemsSummary = "M&E 3 İplik Şardonlu Sweatshirt - Antrasit (M) x 1",
                productCode = "ME-SWT-03",
                barcode = "868200300301",
                color = "Antrasit",
                size = "M",
                quantity = 1,
                city = "İzmir",
                isStockDeducted = true,
                isDemo = true,
                syncStatus = "SENKRONİZE"
            )
        )
        database.orderDao().insertOrders(sampleOrders)

        // Sample customer questions
        val sampleQuestions = listOf(
            CustomerQuestionsEntity(
                questionId = "QST-9021",
                customerName = "Burak K.",
                productTitle = "M&E Tekstil Erkek %100 Pamuklu Polo Yaka Tişört",
                productCode = "ME-TSH-01",
                questionText = "Boy 1.82, kilo 84 hangi beden tercih etmeliyim? Yıkamada çekme yapar mı?",
                questionDate = now - 3600000 * 1,
                status = "BEKLIYOR",
                aiSuggestedReply = "Merhaba, 1.82 boy ve 84 kg için L (Large) beden rahat ve tam kalıp olacaktır. Ürünümüz sanforlu %100 penye pamuk olduğu için 30 derecede yıkandığında çekme yapmaz. Keyifli alışverişler dileriz.",
                selectedStyle = ResponseStyle.FORMAL,
                isDemo = true
            ),
            CustomerQuestionsEntity(
                questionId = "QST-9018",
                customerName = "Selin T.",
                productTitle = "M&E Tekstil Regular Fit Keten Karışımlı Gömlek",
                productCode = "ME-GOM-02",
                questionText = "Kumaşı iç gösteriyor mu? Yaz aylarında terletir mi?",
                questionDate = now - 3600000 * 4,
                status = "BEKLIYOR",
                aiSuggestedReply = "Değerli Müşterimiz, keten ve pamuk karışımı gramajlı özel dokumamız kesinlikle iç göstermez. Nefes alabilen doğal lifleri sayesinde yaz sıcağında serin tutar ve terletme yapmaz.",
                selectedStyle = ResponseStyle.FRIENDLY,
                isDemo = true
            )
        )
        database.customerQuestionDao().insertQuestions(sampleQuestions)

        // Sample returns
        val sampleReturns = listOf(
            TrendyolReturnEntity(
                returnId = "RET-101",
                orderNumber = "TY-8488001",
                productCode = "ME-TSH-01",
                productTitle = "M&E Tekstil Erkek Polo Yaka Tişört",
                color = "Siyah",
                size = "M",
                returnReason = "Beden Küçük Geldi",
                customerComment = "Omuzları biraz dar oldu, L beden ile değiştirmek istiyorum.",
                returnDate = now - 86400000,
                isDemo = true
            ),
            TrendyolReturnEntity(
                returnId = "RET-102",
                orderNumber = "TY-8487220",
                productCode = "ME-GOM-02",
                productTitle = "M&E Tekstil Regular Fit Keten Gömlek",
                color = "Haki",
                size = "XL",
                returnReason = "Kumaş Beklediğim Gibi Değil",
                customerComment = "Rengi fotoğraftakinden biraz daha koyu haki geldi.",
                returnDate = now - 86400000 * 2,
                isDemo = true
            )
        )
        database.returnDao().insertReturns(sampleReturns)

        // Sample stock movements
        database.stockMovementDao().insertMovement(
            StockMovementEntity(
                barcode = "868200100101",
                productCode = "ME-TSH-01",
                title = "M&E Tekstil Erkek %100 Pamuklu Polo Yaka Tişört",
                movementType = "GİRİŞ",
                quantityChange = 50,
                previousStock = 0,
                remainingStock = 50,
                note = "Demo başlangıç envanter girişi"
            )
        )

        database.auditLogDao().insertLog(
            AuditLogEntity(
                actionType = "INITIAL_SEED_DEMO",
                moduleName = "Sistem",
                details = "M&E Tekstil demo ürün, sipariş ve soru verileri yüklendi."
            )
        )
    }

    /**
     * Clears demo data completely and cleanly from local storage without touching user records.
     */
    suspend fun clearDemoData() = withContext(Dispatchers.IO) {
        database.withTransaction {
            val pCount = database.productDao().deleteDemoProducts()
            val oCount = database.orderDao().deleteDemoOrders()
            val qCount = database.customerQuestionDao().deleteDemoQuestions()
            val rCount = database.returnDao().deleteDemoReturns()

            database.auditLogDao().insertLog(
                AuditLogEntity(
                    actionType = "DEMO_DATA_CLEARED",
                    moduleName = "Sistem",
                    details = "Demo verileri temizlendi: $pCount ürün, $oCount sipariş, $qCount soru, $rCount iade."
                )
            )
        }
    }

    suspend fun syncAllFromTrendyol(): Result<String> = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            return@withContext Result.failure(IllegalStateException("Zaten devam eden bir senkronizasyon işlemi var."))
        }

        val startTime = System.currentTimeMillis()
        val config = configManager.getTrendyolConfig()

        try {
            if (config.sellerId.isBlank() || config.apiKey.isBlank() || config.apiSecret.isBlank()) {
                val duration = System.currentTimeMillis() - startTime
                val log = SyncLogEntity(
                    syncType = "ALL",
                    status = "NOT_CONFIGURED",
                    itemsProcessed = 0,
                    itemsFailed = 0,
                    details = "Trendyol API anahtarları henüz girilmedi. Ayarlar ekranından Satıcı ID, API Key ve Secret bilgilerini kaydediniz.",
                    durationMs = duration
                )
                database.syncLogDao().insertSyncLog(log)
                return@withContext Result.failure(IllegalStateException("Trendyol API anahtarları yapılandırılmamış (NOT_CONFIGURED). Lütfen Ayarlar sekmesini doldurun."))
            }

            // Real Trendyol Sync with pagination
            val credential = Credentials.basic(config.apiKey, config.apiSecret)
            var totalOrdersProcessed = 0
            var totalOrdersFailed = 0
            var page = 0
            var hasMorePages = true
            val maxPages = 3 // Safety ceiling

            while (hasMorePages && page < maxPages) {
                val ordersUrl = "https://api.trendyol.com/sapigw/suppliers/${config.sellerId}/orders?size=50&page=$page"
                val request = Request.Builder()
                    .url(ordersUrl)
                    .header("Authorization", credential)
                    .header("User-Agent", "${config.sellerId} - SelfIntegration")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val duration = System.currentTimeMillis() - startTime

                    when (response.code) {
                        401 -> {
                            val msg = "Yetkilendirme Hatası (401): API Key veya API Secret geçersiz."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                        403 -> {
                            val msg = "Yetkisiz Erişim (403): Satıcı ID'nizin bu API kaynağına yetkisi yok."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                        404 -> {
                            val msg = "Kaynak Bulunamadı (404): Satıcı ID veya API uç noktası mevcut değil."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                        408 -> {
                            val msg = "Trendyol Sunucu Zaman Aşımı (408)."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                        429 -> {
                            val msg = "Trendyol İstek Sınırı (Rate Limit 429). Lütfen kısa süre sonra tekrar deneyin."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                        in 500..599 -> {
                            val msg = "Trendyol Sunucu Hatası (HTTP ${response.code}). Trendyol servisleri geçici olarak yanıt vermiyor."
                            database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                            return@withContext Result.failure(Exception(msg))
                        }
                    }

                    if (!response.isSuccessful) {
                        val msg = "HTTP ${response.code}: ${response.message}"
                        database.syncLogDao().insertSyncLog(SyncLogEntity(syncType = "ORDERS", status = "FAILED", itemsProcessed = totalOrdersProcessed, itemsFailed = 1, details = msg, durationMs = duration))
                        return@withContext Result.failure(Exception(msg))
                    }

                    val bodyString = response.body?.string().orEmpty()
                    val json = JSONObject(bodyString)
                    val contentArray = json.optJSONArray("content") ?: org.json.JSONArray()
                    val totalPages = json.optInt("totalPages", 1)

                    for (i in 0 until contentArray.length()) {
                        try {
                            val orderObj = contentArray.getJSONObject(i)
                            val orderNumber = orderObj.optString("orderNumber", "")
                            if (orderNumber.isBlank()) continue

                            val existingOrder = database.orderDao().getOrderByNumber(orderNumber)
                            val lines = orderObj.optJSONArray("lines")
                            val firstLine = if (lines != null && lines.length() > 0) lines.getJSONObject(0) else null
                            val barcode = firstLine?.optString("barcode", "") ?: ""
                            val qty = firstLine?.optInt("quantity", 1) ?: 1

                            if (existingOrder == null) {
                                // New incoming order -> Insert and deduct stock idempotently
                                database.withTransaction {
                                    val newOrder = TrendyolOrderEntity(
                                        orderNumber = orderNumber,
                                        customerName = "${orderObj.optString("customerFirstName")} ${orderObj.optString("customerLastName")}".trim(),
                                        orderDate = orderObj.optLong("orderDate", System.currentTimeMillis()),
                                        status = TrendyolOrderStatus.NEW,
                                        prepStatus = OrderPrepStatus.WAITING,
                                        totalPrice = orderObj.optDouble("grossAmount", 0.0),
                                        itemsSummary = firstLine?.optString("productName", "Trendyol Siparişi") ?: "Trendyol Siparişi",
                                        productCode = firstLine?.optString("productCode", barcode) ?: barcode,
                                        barcode = barcode,
                                        color = firstLine?.optString("color", "") ?: "",
                                        size = firstLine?.optString("size", "") ?: "",
                                        quantity = qty,
                                        city = orderObj.optJSONObject("shipmentAddress")?.optString("city", "İstanbul") ?: "İstanbul",
                                        isStockDeducted = true,
                                        isDemo = false,
                                        syncStatus = "SENKRONİZE"
                                    )
                                    database.orderDao().insertOrder(newOrder)

                                    if (barcode.isNotBlank()) {
                                        val product = database.productDao().getProductByBarcode(barcode)
                                        if (product != null) {
                                            val newStock = (product.stockQuantity - qty).coerceAtLeast(0)
                                            database.productDao().updateStock(barcode, newStock)
                                            database.productDao().incrementTotalSold(barcode, qty)
                                            database.stockMovementDao().insertMovement(
                                                StockMovementEntity(
                                                    barcode = barcode,
                                                    productCode = product.productCode,
                                                    title = product.title,
                                                    movementType = "SATIŞ",
                                                    quantityChange = -qty,
                                                    previousStock = product.stockQuantity,
                                                    remainingStock = newStock,
                                                    referenceId = orderNumber,
                                                    note = "Trendyol Canlı Senkronizasyon Satış Düşümü"
                                                )
                                            )
                                        }
                                    }
                                }
                                totalOrdersProcessed++
                            } else {
                                // Existing order -> Update status only, never double-deduct stock
                                val tyStatusStr = orderObj.optString("status", "")
                                val resolvedStatus = when (tyStatusStr.uppercase()) {
                                    "SHIPPED" -> TrendyolOrderStatus.SHIPPED
                                    "DELIVERED" -> TrendyolOrderStatus.DELIVERED
                                    "CANCELLED" -> TrendyolOrderStatus.CANCELLED
                                    "RETURNED" -> TrendyolOrderStatus.RETURNED
                                    else -> existingOrder.status
                                }
                                database.orderDao().updateOrder(existingOrder.copy(status = resolvedStatus, lastUpdated = System.currentTimeMillis()))
                                totalOrdersProcessed++
                            }
                        } catch (e: Exception) {
                            totalOrdersFailed++
                        }
                    }

                    page++
                    hasMorePages = page < totalPages
                }
            }

            configManager.updateTrendyolSyncStatus(true, "")
            val duration = System.currentTimeMillis() - startTime
            val log = SyncLogEntity(
                syncType = "ALL",
                status = "SUCCESS",
                itemsProcessed = totalOrdersProcessed,
                itemsFailed = totalOrdersFailed,
                details = "Trendyol API canlı senkronizasyon tamamlandı ($totalOrdersProcessed sipariş işlendi, $totalOrdersFailed hata).",
                durationMs = duration
            )
            database.syncLogDao().insertSyncLog(log)
            return@withContext Result.success("Trendyol canlı senkronizasyonu başarıyla tamamlandı ($totalOrdersProcessed sipariş).")

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val err = e.message ?: "Bağlantı hatası"
            val log = SyncLogEntity(
                syncType = "ALL",
                status = "FAILED",
                itemsProcessed = 0,
                itemsFailed = 1,
                details = "İstisna: $err",
                durationMs = duration
            )
            database.syncLogDao().insertSyncLog(log)
            return@withContext Result.failure(e)
        } finally {
            syncMutex.unlock()
        }
    }
}
