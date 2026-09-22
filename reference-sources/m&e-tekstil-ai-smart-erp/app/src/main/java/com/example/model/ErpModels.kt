package com.example.model

data class InventoryItem(
    val id: String,
    val name: String,
    val color: String,
    val size: String,
    val stockCount: Int,
    val price: Double,
    val isBestSeller: Boolean,
    val shelfLocation: String
)

data class ChatMessage(
    val id: String,
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hasApproval: Boolean = false,
    val approvalAction: String? = null
)

data class PricingProduct(
    val id: String,
    val name: String,
    val color: String,
    val costPrice: Double,
    val currentPrice: Double,
    val trendyolCommission: Double = 0.18,
    val cargoFee: Double = 39.90
)
