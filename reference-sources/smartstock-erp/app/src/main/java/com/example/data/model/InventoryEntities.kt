package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sku: String,
    val barcode: String,
    val name: String,
    val nameEn: String = "",
    val category: String,
    val warehouseId: Long,
    val stockQuantity: Int,
    val minAlertStock: Int = 10,
    val costPrice: Double,
    val sellingPrice: Double,
    val trendyolBarcode: String = "",
    val isTrendyolSynced: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stockQuantity <= minAlertStock

    val isOutOfStock: Boolean
        get() = stockQuantity <= 0
}

@Entity(tableName = "warehouses")
data class Warehouse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val location: String,
    val managerName: String,
    val phone: String,
    val maxCapacity: Int = 5000
)
