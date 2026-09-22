package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val title: String,
    val brand: String,
    val category: String,
    val salePrice: Double,
    val listPrice: Double,
    val stockCount: Int,
    val imageUrl: String,
    val buyboxWinner: Boolean = true,
    val competitorPrice: Double = 0.0,
    val isActive: Boolean = true,
    val description: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val customerName: String,
    val customerCity: String,
    val orderDate: String,
    val totalAmount: Double,
    val status: String, // Created (جديد), Picking (قيد التجهيز), Shipped (بالشحن), Delivered (تم التسليم), Returned (مرتجع)
    val cargoProvider: String, // Trendyol Express, Yurtiçi Kargo, Aras Kargo, Sürat Kargo
    val trackingNumber: String,
    val itemCount: Int,
    val itemsSummary: String
)

@Entity(tableName = "customer_questions")
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productBarcode: String,
    val productTitle: String,
    val customerName: String,
    val questionText: String,
    val questionDate: String,
    val replyText: String? = null,
    val isAnswered: Boolean = false,
    val aiSuggestedReply: String? = null
)

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "Trendyol Partner Store",
    val supplierId: String = "842910",
    val apiKey: String = "",
    val apiSecret: String = "",
    val geminiApiKey: String = "",
    val isLiveMode: Boolean = false,
    val storeScore: Double = 9.8,
    val deliveryScore: Double = 9.9,
    val commissionAverage: Double = 18.0,
    // Telegram Bot Integration
    val telegramBotToken: String = "",
    val telegramChatId: String = "",
    val isTelegramEnabled: Boolean = false,
    val notifyNewOrders: Boolean = true,
    val notifyCustomerQuestions: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyDailyReport: Boolean = true,
    val notifyAiSlowMoving: Boolean = true
)
