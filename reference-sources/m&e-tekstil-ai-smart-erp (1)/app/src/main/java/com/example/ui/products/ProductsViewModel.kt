package com.example.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val products: List<ProductDto> = emptyList(),
    val filteredProducts: List<ProductDto> = emptyList(),
    val categories: List<String> = listOf("Tümü", "Elbise", "Bluz & Gömlek", "Pantolon", "Etek", "Takım", "Trikotaj", "Kumaş"),
    val selectedCategory: String = "Tümü",
    val searchQuery: String = "",
    val selectedProduct: ProductDto? = null,
    val isAddDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val totalStockSum: Int = 0,
    val criticalStockCount: Int = 0
)

class ProductsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProductsUiState())
    val uiState: StateFlow<ProductsUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadProducts()
        loadCategories()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val api = NetworkModule.getApiService()
                val resp = api.getProducts(
                    search = _uiState.value.searchQuery.ifBlank { null },
                    category = if (_uiState.value.selectedCategory != "Tümü") _uiState.value.selectedCategory else null
                )

                if (resp.isSuccessful && resp.body() != null) {
                    val list = resp.body()!!
                    applyProductsList(list)
                } else {
                    applyProductsList(emptyList())
                }
            } catch (e: Exception) {
                applyProductsList(emptyList())
                _uiState.update {
                    it.copy(
                        errorMessage = "Sunucuya bağlanılamadı (${e.localizedMessage ?: "Bağlantı bekleniyor"}). Lütfen internet bağlantınızı veya Backend URL adresini Ayarlar'dan kontrol edin."
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val api = NetworkModule.getApiService()
                val resp = api.getCategories()
                if (resp.isSuccessful && resp.body() != null) {
                    val cats = listOf("Tümü") + resp.body()!!.filter { it != "Tümü" }
                    _uiState.update { it.copy(categories = cats) }
                }
            } catch (_: Exception) {
                // Keep default categories on error
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            filterLocally()
        }
    }

    fun onSelectCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        filterLocally()
    }

    private fun filterLocally() {
        val query = _uiState.value.searchQuery.trim().lowercase()
        val cat = _uiState.value.selectedCategory

        val filtered = _uiState.value.products.filter { product ->
            val matchesCategory = (cat == "Tümü") || (product.categoryName?.equals(cat, ignoreCase = true) == true)
            val matchesSearch = query.isBlank() ||
                    product.title.lowercase().contains(query) ||
                    product.productCode.lowercase().contains(query) ||
                    (product.categoryName?.lowercase()?.contains(query) == true) ||
                    product.variants.any { it.barcode.lowercase().contains(query) }

            matchesCategory && matchesSearch
        }

        _uiState.update { it.copy(filteredProducts = filtered) }
    }

    private fun applyProductsList(list: List<ProductDto>) {
        val totalStock = list.sumOf { it.totalStock }
        val criticalCount = list.count { it.totalStock <= 5 }

        _uiState.update {
            it.copy(
                products = list,
                filteredProducts = list,
                totalStockSum = totalStock,
                criticalStockCount = criticalCount
            )
        }
        filterLocally()
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = true, errorMessage = null, successMessage = null) }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = false) }
    }

    fun selectProduct(product: ProductDto?) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun createProduct(request: ProductCreateRequest, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val api = NetworkModule.getApiService()
                val resp = api.createProduct(request)
                if (resp.isSuccessful && resp.body() != null) {
                    val created = resp.body()!!
                    val updatedList = listOf(created) + _uiState.value.products
                    applyProductsList(updatedList)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isAddDialogOpen = false,
                            successMessage = "'${created.title}' ürünü (${created.productCode}) başarıyla kaydedildi."
                        )
                    }
                    onComplete(true)
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Kayıt hatası: HTTP ${resp.code()} - ${resp.errorBody()?.string() ?: "Bilinmeyen hata"}"
                        )
                    }
                    onComplete(false)
                }
            } catch (e: Exception) {
                // If offline, add locally to preview list
                val localCode = request.productCode?.ifBlank { null } ?: "MET-${System.currentTimeMillis() % 10000}"
                val localVariants = request.variants.mapIndexed { idx, v ->
                    ProductVariantDto(
                        id = idx + 1,
                        productId = 999,
                        barcode = v.barcode ?: ("868" + (100000000L + idx)),
                        sku = "$localCode-${v.color.take(3).uppercase()}-${v.size}",
                        color = v.color,
                        size = v.size,
                        purchaseCost = v.purchaseCost,
                        sellingPrice = if (v.sellingPrice > 0) v.sellingPrice else request.sellingPrice,
                        isActive = true,
                        totalStock = v.initialStock,
                        availableStock = v.initialStock
                    )
                }
                val localProduct = ProductDto(
                    id = (System.currentTimeMillis() % 10000).toInt(),
                    productCode = localCode,
                    title = request.title,
                    description = request.description,
                    categoryName = request.categoryName,
                    brand = request.brand,
                    sellingPrice = request.sellingPrice,
                    vatRate = request.vatRate,
                    status = "ACTIVE",
                    trendyolContentId = null,
                    totalStock = localVariants.sumOf { it.totalStock },
                    variants = localVariants,
                    images = emptyList()
                )
                val updatedList = listOf(localProduct) + _uiState.value.products
                applyProductsList(updatedList)

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isAddDialogOpen = false,
                        successMessage = "Ürün yerel listeye eklendi: ${localProduct.title} (${localProduct.productCode})"
                    )
                }
                onComplete(true)
            }
        }
    }

    fun deleteProduct(id: Int) {
        viewModelScope.launch {
            try {
                val api = NetworkModule.getApiService()
                api.deleteProduct(id)
            } catch (_: Exception) {}

            val updated = _uiState.value.products.filter { it.id != id }
            applyProductsList(updated)
            if (_uiState.value.selectedProduct?.id == id) {
                _uiState.update { it.copy(selectedProduct = null) }
            }
        }
    }

    private fun getMockProducts(): List<ProductDto> {
        return listOf(
            ProductDto(
                id = 101,
                productCode = "MET-1001",
                title = "İpek Dokuma Şifon Kuşaklı Elbise",
                description = "Zarif dökümlü, terletmeyen ipeksi şifon kumaş, astarlı abiye ve günlük elbise.",
                categoryName = "Elbise",
                brand = "M&E Tekstil",
                sellingPrice = 949.90,
                vatRate = 10.0,
                status = "ACTIVE",
                trendyolContentId = "TY-849201",
                totalStock = 48,
                variants = listOf(
                    ProductVariantDto(1, 101, "8682024001011", "MET-1001-SIY-38", "Siyah", "38", 340.0, 949.90, true, 16, 16),
                    ProductVariantDto(2, 101, "8682024001028", "MET-1001-SIY-40", "Siyah", "40", 340.0, 949.90, true, 12, 12),
                    ProductVariantDto(3, 101, "8682024001035", "MET-1001-BEJ-38", "Bej", "38", 340.0, 949.90, true, 14, 14),
                    ProductVariantDto(4, 101, "8682024001042", "MET-1001-BEJ-40", "Bej", "40", 340.0, 949.90, true, 6, 6)
                )
            ),
            ProductDto(
                id = 102,
                productCode = "MET-1002",
                title = "Gofre Kumaş Oversize Gömlek & Bluz",
                description = "Yazlık hafif gofre dokuma, nefes alan pamuk karışımlı kumaş.",
                categoryName = "Bluz & Gömlek",
                brand = "M&E Tekstil",
                sellingPrice = 589.50,
                vatRate = 10.0,
                status = "ACTIVE",
                trendyolContentId = "TY-849202",
                totalStock = 22,
                variants = listOf(
                    ProductVariantDto(5, 102, "8682024002018", "MET-1002-BEY-S", "Beyaz", "S", 210.0, 589.50, true, 8, 8),
                    ProductVariantDto(6, 102, "8682024002025", "MET-1002-BEY-M", "Beyaz", "M", 210.0, 589.50, true, 10, 10),
                    ProductVariantDto(7, 102, "8682024002032", "MET-1002-MAV-S", "Mavi", "S", 210.0, 589.50, true, 4, 4)
                )
            ),
            ProductDto(
                id = 103,
                productCode = "MET-1003",
                title = "Yüksek Bel Palazzo Keten Pantolon",
                description = "Rahat kesim, doğal keten dokuma, cepli modern kadın pantolon.",
                categoryName = "Pantolon",
                brand = "M&E Tekstil",
                sellingPrice = 729.00,
                vatRate = 10.0,
                status = "ACTIVE",
                trendyolContentId = "TY-849203",
                totalStock = 5,
                variants = listOf(
                    ProductVariantDto(8, 103, "8682024003015", "MET-1003-EKR-36", "Ekru", "36", 260.0, 729.00, true, 2, 2),
                    ProductVariantDto(9, 103, "8682024003022", "MET-1003-EKR-38", "Ekru", "38", 260.0, 729.00, true, 3, 3)
                )
            ),
            ProductDto(
                id = 104,
                productCode = "MET-1004",
                title = "Fitilli Triko İkili Takım",
                description = "Etek ve kazak kombin, yumuşak dokulu esnek triko kumaş.",
                categoryName = "Takım",
                brand = "M&E Tekstil",
                sellingPrice = 1199.00,
                vatRate = 10.0,
                status = "ACTIVE",
                trendyolContentId = null,
                totalStock = 30,
                variants = listOf(
                    ProductVariantDto(10, 104, "8682024004012", "MET-1004-LAC-STD", "Lacivert", "Standart", 450.0, 1199.00, true, 18, 18),
                    ProductVariantDto(11, 104, "8682024004029", "MET-1004-VİZ-STD", "Vizon", "Standart", 450.0, 1199.00, true, 12, 12)
                )
            )
        )
    }
}
