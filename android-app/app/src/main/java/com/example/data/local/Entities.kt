package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AiApprovalStatus
import com.example.domain.model.AiTaskType
import com.example.domain.model.OrderPrepStatus
import com.example.domain.model.ResponseStyle
import com.example.domain.model.TrendyolOrderStatus

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey val warehouseId: String, // örn: "W-MAIN", "W-WORKSHOP", "W-STORE"
    val name: String,
    val location: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "warehouse_transfers")
data class WarehouseTransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val productCode: String,
    val productTitle: String,
    val color: String = "",
    val size: String = "",
    val fromWarehouseId: String,
    val toWarehouseId: String,
    val quantity: Int,
    val note: String = "",
    val performedBy: String = "Yönetici",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [
        Index("productCode"),
        Index("categoryName"),
        Index("warehouseId")
    ]
)
data class TrendyolProductEntity(
    @PrimaryKey val barcode: String,
    val productCode: String,
    val title: String,
    val categoryName: String,
    val color: String,
    val size: String,
    val stockQuantity: Int,
    val salePrice: Double,
    val buyingPrice: Double,
    val commissionRate: Double, // örn: 18.5 (%)
    val shippingCost: Double = 35.0, // TL
    val warehouseId: String = "W-MAIN",
    val isActive: Boolean = true,
    val fabricDetails: String = "%100 Pamuklu Süprem Kumaş",
    val trendyolStatus: String = "Satışta", // "Satışta", "Tükendi", "Kilitli"
    val lastSyncTime: Long = System.currentTimeMillis(),
    val lastSaleDate: Long = 0L,
    val totalSold: Int = 0,
    val returnCount: Int = 0,
    val isDemo: Boolean = false
)

@Entity(
    tableName = "orders",
    indices = [
        Index("barcode"),
        Index("status"),
        Index("orderDate")
    ]
)
data class TrendyolOrderEntity(
    @PrimaryKey val orderNumber: String,
    val customerName: String,
    val orderDate: Long,
    val status: TrendyolOrderStatus,
    val prepStatus: OrderPrepStatus,
    val totalPrice: Double,
    val itemsSummary: String, // "M&E Slim Fit Polo Yaka Tişört - Siyah (L) x 1"
    val productCode: String,
    val barcode: String,
    val color: String,
    val size: String,
    val quantity: Int,
    val city: String,
    val buyingCostTotal: Double = 0.0,
    val commissionAmount: Double = 0.0,
    val shippingCost: Double = 35.0,
    val netProfit: Double = 0.0,
    val cargoProvider: String = "Trendyol Express",
    val trackingNumber: String = "",
    val returnStatus: String = "YOK", // "YOK", "TALEP_EDİLDİ", "İADE_ALINDI"
    val syncStatus: String = "SENKRONİZE", // "SENKRONİZE", "BEKLEMEDE", "HATA"
    val isDuplicate: Boolean = false,
    val isStockDeducted: Boolean = true,
    val isDemo: Boolean = false,
    val note: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_questions")
data class CustomerQuestionsEntity(
    @PrimaryKey val questionId: String,
    val customerName: String,
    val productTitle: String,
    val productCode: String,
    val questionText: String,
    val questionDate: Long,
    val status: String = "BEKLIYOR", // "BEKLIYOR", "YANITLANDI", "REDDEDILDI", "MANUEL_INCELEME"
    val aiSuggestedReply: String = "",
    val finalReply: String = "",
    val selectedStyle: ResponseStyle = ResponseStyle.FORMAL,
    val requiresHumanReview: Boolean = false,
    val uncertaintyReason: String = "",
    val isSent: Boolean = false,
    val providerUsed: String = "",
    val modelUsed: String = "",
    val isDemo: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index("barcode"),
        Index("timestamp")
    ]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val productCode: String,
    val title: String,
    val movementType: String, // "GİRİŞ", "SATIŞ", "İADE", "TRANSFER", "SAYIM_DÜZELTME"
    val quantityChange: Int,
    val previousStock: Int = 0,
    val remainingStock: Int,
    val warehouseId: String = "W-MAIN",
    val referenceId: String = "", // sipariş no veya transfer id
    val note: String,
    val performedBy: String = "Yönetici",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val syncType: String, // "ALL", "ORDERS", "PRODUCTS", "RETURNS"
    val status: String,   // "SUCCESS", "FAILED", "PARTIAL"
    val itemsProcessed: Int,
    val itemsFailed: Int,
    val details: String,
    val durationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_tasks")
data class AiTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskType: AiTaskType,
    val targetReferenceId: String, // sipariş no, soru id veya barkod
    val inputData: String,
    val outputData: String,
    val providerName: String,
    val modelName: String,
    val slotNumber: Int,
    val status: AiApprovalStatus = AiApprovalStatus.BEKLIYOR,
    val errorMessage: String = "",
    val userEditedContent: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val approvedAt: Long? = null
)

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val triggerType: String, // "NEW_ORDER", "NEW_QUESTION", "LOW_STOCK", "SYNC_FAILURE", "DAILY_SCHEDULE", "HIGH_RETURN_RATE"
    val conditionDescription: String,
    val actionType: String, // "TELEGRAM_NOTIFY", "AI_REPLY_GENERATE", "DAILY_REPORT", "STOCK_ALERT"
    val isEnabled: Boolean = true,
    val lastTriggeredAt: Long? = null,
    val executionCount: Int = 0,
    val lastError: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val moduleName: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_briefs")
data class DailyBriefEntity(
    @PrimaryKey val reportDate: String, // "YYYY-MM-DD"
    val totalSales: Double,
    val orderCount: Int,
    val estimatedProfit: Double,
    val lowStockCount: Int,
    val returnCount: Int,
    val pendingQuestionsCount: Int,
    val aiInterpretation: String,
    val suggestedActions: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "returns",
    indices = [
        Index("orderNumber"),
        Index("barcode")
    ]
)
data class TrendyolReturnEntity(
    @PrimaryKey val returnId: String,
    val orderNumber: String,
    val barcode: String = "",
    val productCode: String,
    val productTitle: String,
    val color: String,
    val size: String,
    val quantity: Int = 1,
    val returnReason: String, // "Beden Küçük Geldi", "Kumaş Beklediğim Gibi Değil", "Defolu/Hatalı", "Vazgeçtim"
    val customerComment: String = "",
    val restockAction: String = "STOKA_EKLENDİ", // "STOKA_EKLENDİ", "DEFOLU_AYRILDI", "BEKLEMEDE"
    val refundAmount: Double = 0.0,
    val isRestocked: Boolean = false,
    val isDemo: Boolean = false,
    val processedBy: String = "Yönetici",
    val returnDate: Long = System.currentTimeMillis(),
    val status: String = "İNCELENİYOR" // "İNCELENİYOR", "KABUL_EDİLDİ", "REDDEDİLDİ"
)
