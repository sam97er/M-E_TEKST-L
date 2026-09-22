package com.example.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersUiState(
    val isLoading: Boolean = false,
    val isSimulating: Boolean = false,
    val isUpdatingStatus: Boolean = false,
    val orders: List<OrderSummaryItemDto> = emptyList(),
    val salesSummary: SalesSummaryDto = SalesSummaryDto(),
    val dailyAnalytics: List<DailySalesPointDto> = emptyList(),
    val productRanking: List<ProductProfitRankingDto> = emptyList(),
    val selectedOrder: OrderDetailDto? = null,
    val statusFilter: String = "ALL", // ALL, Created, Shipped, Delivered, Cancelled, Returned
    val searchQuery: String = "",
    val sortBy: String = "date_desc",
    val selectedTab: Int = 0, // 0: Siparişler, 1: Kâr & Analiz, 2: Simülatör
    val simSalePrice: String = "599.90",
    val simPurchaseCost: String = "180.00",
    val simCommissionRate: String = "20.0",
    val simShippingCost: String = "38.50",
    val simPackagingCost: String = "12.00",
    val simVatRate: String = "10.0",
    val simResult: ProfitSimulationResponse? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class OrdersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    private val api get() = NetworkModule.getApiService()

    init {
        loadSummary()
        loadOrders()
        loadAnalytics()
        runDefaultSimulation()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        if (tabIndex == 1) {
            loadSummary()
            loadAnalytics()
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            try {
                val res = api.getSalesSummary()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(salesSummary = res.body()!!) }
                }
            } catch (e: Exception) {
                // Keep existing summary
            }
        }
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                val resDaily = api.getDailySalesAnalytics(days = 7)
                if (resDaily.isSuccessful && resDaily.body() != null) {
                    _uiState.update { it.copy(dailyAnalytics = resDaily.body()!!) }
                }

                val resRank = api.getProductProfitRanking(limit = 15)
                if (resRank.isSuccessful && resRank.body() != null) {
                    _uiState.update { it.copy(productRanking = resRank.body()!!) }
                }
            } catch (e: Exception) {
                // Ignore background analytics error
            }
        }
    }

    fun loadOrders(
        status: String = _uiState.value.statusFilter,
        search: String = _uiState.value.searchQuery,
        sortBy: String = _uiState.value.sortBy
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val filter = if (status == "ALL" || status.isBlank()) null else status
                val query = if (search.isBlank()) null else search.trim()
                val res = api.getOrders(
                    status = filter,
                    search = query,
                    limit = 50,
                    offset = 0,
                    sortBy = sortBy
                )
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            orders = res.body()!!,
                            statusFilter = status,
                            searchQuery = search,
                            sortBy = sortBy
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Siparişler yüklenemedi: ${res.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Bağlantı hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun setStatusFilter(filter: String) {
        _uiState.update { it.copy(statusFilter = filter) }
        loadOrders(status = filter)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadOrders(search = query)
    }

    fun setSortBy(sortBy: String) {
        _uiState.update { it.copy(sortBy = sortBy) }
        loadOrders(sortBy = sortBy)
    }

    fun selectOrder(orderSummary: OrderSummaryItemDto?) {
        if (orderSummary == null) {
            _uiState.update { it.copy(selectedOrder = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val res = api.getOrderDetail(orderSummary.id)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(isLoading = false, selectedOrder = res.body()!!) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Sipariş detayı alınamadı.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    fun updateOrderStatus(orderId: Int, newStatus: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingStatus = true) }
            try {
                val res = api.updateOrderStatus(orderId, OrderStatusUpdateRequest(status = newStatus))
                if (res.isSuccessful && res.body() != null) {
                    val updated = res.body()!!
                    _uiState.update {
                        it.copy(
                            isUpdatingStatus = false,
                            selectedOrder = updated,
                            successMessage = "Sipariş durumu '$newStatus' olarak güncellendi ve kâr yeniden hesaplandı."
                        )
                    }
                    loadOrders()
                    loadSummary()
                } else {
                    _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = "Durum güncellenemedi: ${res.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingStatus = false, errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    // Profit Simulator handlers
    fun updateSimInput(
        salePrice: String? = null,
        purchaseCost: String? = null,
        commissionRate: String? = null,
        shippingCost: String? = null,
        packagingCost: String? = null,
        vatRate: String? = null
    ) {
        _uiState.update { state ->
            state.copy(
                simSalePrice = salePrice ?: state.simSalePrice,
                simPurchaseCost = purchaseCost ?: state.simPurchaseCost,
                simCommissionRate = commissionRate ?: state.simCommissionRate,
                simShippingCost = shippingCost ?: state.simShippingCost,
                simPackagingCost = packagingCost ?: state.simPackagingCost,
                simVatRate = vatRate ?: state.simVatRate
            )
        }
        calculateSimulation()
    }

    fun calculateSimulation() {
        val s = _uiState.value
        val sale = s.simSalePrice.toDoubleOrNull() ?: 0.0
        val cost = s.simPurchaseCost.toDoubleOrNull() ?: 0.0
        val comm = s.simCommissionRate.toDoubleOrNull() ?: 20.0
        val ship = s.simShippingCost.toDoubleOrNull() ?: 38.50
        val pack = s.simPackagingCost.toDoubleOrNull() ?: 12.00
        val vat = s.simVatRate.toDoubleOrNull() ?: 10.0

        if (sale <= 0 || cost < 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSimulating = true) }
            try {
                val req = ProfitSimulationRequest(
                    salePrice = sale,
                    purchaseCost = cost,
                    commissionRatePercent = comm,
                    shippingCost = ship,
                    packagingCost = pack,
                    vatRatePercent = vat,
                    serviceFee = 8.49
                )
                val res = api.simulatePricing(req)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(isSimulating = false, simResult = res.body()!!) }
                } else {
                    _uiState.update { it.copy(isSimulating = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSimulating = false) }
            }
        }
    }

    private fun runDefaultSimulation() {
        calculateSimulation()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
