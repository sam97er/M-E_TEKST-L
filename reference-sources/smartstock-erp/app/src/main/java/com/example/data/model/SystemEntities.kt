package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trendyol_sync_logs")
data class TrendyolSyncLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String, // "INVENTORY_SYNC", "ORDER_IMPORT", "CATALOG_UPDATE", "PRICE_SYNC"
    val status: String,     // "SUCCESS", "PENDING", "FAILED"
    val message: String,
    val itemsProcessed: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class TrendyolConfig(
    val supplierId: String = "982341",
    val apiKey: String = "ty_prod_live_8f3a9e4d1b7c",
    val apiSecret: String = "••••••••••••••••",
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMinutes: Int = 15,
    val storeUrl: String = "https://trendyol.com/magaza/smarterp-official",
    val lastSyncTime: Long = System.currentTimeMillis() - 1000 * 60 * 12
)

enum class EmployeeRole(val titleAr: String, val titleEn: String) {
    MANAGER("مدير النظام", "System Manager"),
    WAREHOUSE_SUPERVISOR("مشرف مستودع", "Warehouse Supervisor"),
    CASHIER("أمين الصندوق (كاشير)", "Cashier"),
    INVENTORY_SPECIALIST("أخصائي جرد وتدقيق", "Inventory Specialist")
}

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val employeeCode: String,
    val fullName: String,
    val role: EmployeeRole,
    val pinCode: String = "1234",
    val isClockedIn: Boolean = true,
    val currentShift: String = "الوردية الصباحية (Morning)",
    val dailyTargetSales: Double = 5000.0,
    val currentSales: Double = 3450.0,
    val scannedItemsCount: Int = 142,
    val efficiencyScore: Int = 96
)

enum class TicketPriority(val titleAr: String, val titleEn: String) {
    URGENT("عاجل جداً", "Urgent"),
    HIGH("مرتفع", "High"),
    MEDIUM("متوسط", "Medium"),
    LOW("عادي", "Low")
}

enum class TicketStatus(val titleAr: String, val titleEn: String) {
    OPEN("مفتوحة", "Open"),
    IN_PROGRESS("قيد المعالجة", "In Progress"),
    RESOLVED("تم الحل", "Resolved")
}

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ticketCode: String,
    val customerName: String,
    val customerContact: String,
    val subject: String,
    val message: String,
    val priority: TicketPriority,
    val status: TicketStatus = TicketStatus.OPEN,
    val assignedStaff: String = "فريق الدعم الفني 24/7",
    val createdAt: Long = System.currentTimeMillis(),
    val responseNotes: String = ""
)

@Entity(tableName = "backup_logs")
data class BackupLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val backupName: String,
    val type: String = "FULL_SYSTEM_JSON",
    val recordCount: Int,
    val sizeKb: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val encryptionType: String = "AES-256 GCM"
)
