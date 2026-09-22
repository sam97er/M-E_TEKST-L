package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "id") val id: Int,
    @Json(name = "product_code") val productCode: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "category_name") val categoryName: String?,
    @Json(name = "brand") val brand: String,
    @Json(name = "selling_price") val sellingPrice: Double,
    @Json(name = "vat_rate") val vatRate: Double,
    @Json(name = "status") val status: String,
    @Json(name = "trendyol_content_id") val trendyolContentId: String?,
    @Json(name = "total_stock") val totalStock: Int,
    @Json(name = "variants") val variants: List<ProductVariantDto> = emptyList(),
    @Json(name = "images") val images: List<ProductImageDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProductVariantDto(
    @Json(name = "id") val id: Int,
    @Json(name = "product_id") val productId: Int,
    @Json(name = "barcode") val barcode: String,
    @Json(name = "sku") val sku: String?,
    @Json(name = "color") val color: String,
    @Json(name = "size") val size: String,
    @Json(name = "purchase_cost") val purchaseCost: Double,
    @Json(name = "selling_price") val sellingPrice: Double,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "total_stock") val totalStock: Int,
    @Json(name = "available_stock") val availableStock: Int
)

@JsonClass(generateAdapter = true)
data class ProductImageDto(
    @Json(name = "id") val id: Int,
    @Json(name = "product_id") val productId: Int,
    @Json(name = "image_url") val imageUrl: String,
    @Json(name = "is_primary") val isPrimary: Boolean,
    @Json(name = "review_status") val reviewStatus: String
)

@JsonClass(generateAdapter = true)
data class ProductCreateRequest(
    @Json(name = "product_code") val productCode: String? = null,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "category_name") val categoryName: String? = null,
    @Json(name = "brand") val brand: String = "M&E Tekstil",
    @Json(name = "selling_price") val sellingPrice: Double,
    @Json(name = "vat_rate") val vatRate: Double = 10.0,
    @Json(name = "variants") val variants: List<ProductVariantCreateRequest> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProductVariantCreateRequest(
    @Json(name = "color") val color: String,
    @Json(name = "size") val size: String,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "purchase_cost") val purchaseCost: Double = 0.0,
    @Json(name = "selling_price") val sellingPrice: Double = 0.0,
    @Json(name = "initial_stock") val initialStock: Int = 0,
    @Json(name = "min_stock_threshold") val minStockThreshold: Int = 5
)

@JsonClass(generateAdapter = true)
data class BarcodeGenerateResponseDto(
    @Json(name = "barcode") val barcode: String,
    @Json(name = "format") val format: String
)
