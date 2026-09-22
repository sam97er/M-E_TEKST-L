package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod(val titleAr: String, val titleEn: String) {
    CASH("نقداً", "Cash"),
    CARD_MADA("بطاقة مدى / POS", "Mada / POS Card"),
    CREDIT_CARD("فيزا / ماستركارد", "Credit Card"),
    ELECTRONIC_WALLET("محفظة إلكترونية (Apple/STC)", "E-Wallet (Apple/STC)"),
    SPLIT("دفع مجزأ", "Split Payment"),
    TRENDYOL_GATEWAY("بوابة ترنديول", "Trendyol Gateway")
}

enum class OrderStatus(val titleAr: String, val titleEn: String) {
    COMPLETED("مكتمل", "Completed"),
    PENDING("قيد المعالجة", "Pending"),
    SHIPPED("تم الشحن", "Shipped"),
    CANCELLED("ملغي", "Cancelled")
}

@Entity(tableName = "sale_orders")
data class SaleOrder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String = "",
    val totalAmount: Double, // in SAR
    val paymentMethod: PaymentMethod,
    val paymentStatus: String = "PAID",
    val orderStatus: OrderStatus = OrderStatus.COMPLETED,
    val cashierName: String,
    val source: String = "DIRECT_POS", // "DIRECT_POS", "TRENDYOL", "MOBILE_APP"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "sale_order_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleOrder::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class SaleOrderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val productBarcode: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)
