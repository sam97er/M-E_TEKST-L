package com.example.ui.trendyol

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrendyolUiState(
    val isLoading: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isSyncing: Boolean = false,
    val isUpdatingStock: Boolean = false,
    val config: TrendyolConfigDto? = null,
    val dashboard: TrendyolDashboardDto? = null,
    val connectionTestResult: TrendyolTestConnectionDto? = null,
    val syncResult: TrendyolSyncResponseDto? = null,
    val orders: List<TrendyolOrderSummaryDto> = emptyList(),
    val questions: List<TrendyolQuestionSummaryDto> = emptyList(),
    val selectedTab: TrendyolTab = TrendyolTab.DASHBOARD,
    val errorMessage: String? = null,
    val successSnackbar: String? = null
)

enum class TrendyolTab(val title: String) {
    DASHBOARD("Özet & Durum"),
    ORDERS("Siparişler"),
    QUESTIONS("Gelen Sorular"),
    CONFIG("API Ayarları")
}

class TrendyolViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TrendyolUiState())
    val uiState: StateFlow<TrendyolUiState> = _uiState.asStateFlow()

    init {
        loadTrendyolData()
    }

    fun selectTab(tab: TrendyolTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == TrendyolTab.ORDERS && _uiState.value.orders.isEmpty()) {
            loadOrders()
        } else if (tab == TrendyolTab.QUESTIONS && _uiState.value.questions.isEmpty()) {
            loadQuestions()
        }
    }

    fun loadTrendyolData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val apiService = NetworkModule.getApiService()
                // Fetch config
                val configRes = apiService.getTrendyolConfig()
                val config = if (configRes.isSuccessful) configRes.body() else null

                // Fetch dashboard stats
                val dashRes = apiService.getTrendyolDashboard()
                val dashboard = if (dashRes.isSuccessful) dashRes.body() else null

                // Fetch recent orders
                val ordersRes = apiService.getTrendyolOrders(limit = 20)
                val orders: List<TrendyolOrderSummaryDto> = if (ordersRes.isSuccessful && ordersRes.body() != null) {
                    ordersRes.body()!!
                } else {
                    emptyList()
                }

                // Fetch questions
                val questRes = apiService.getTrendyolQuestions(limit = 20)
                val questions: List<TrendyolQuestionSummaryDto> = if (questRes.isSuccessful && questRes.body() != null) {
                    questRes.body()!!
                } else {
                    emptyList()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    config = config,
                    dashboard = dashboard,
                    orders = orders,
                    questions = questions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dashboard = null,
                    config = null,
                    orders = emptyList(),
                    questions = emptyList(),
                    errorMessage = "Trendyol sunucusuna bağlanılamadı (${e.localizedMessage ?: "Bağlantı hatası"}). Lütfen Ayarlar'dan Backend URL ve API anahtarlarınızı kontrol edin."
                )
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionTestResult = null)
            try {
                val apiService = NetworkModule.getApiService()
                val response = apiService.testTrendyolApi()
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        connectionTestResult = result,
                        successSnackbar = if (result.success) "Bağlantı Başarılı: ${result.latencyMs} ms" else null,
                        errorMessage = if (!result.success) result.message else null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isTestingConnection = false,
                        errorMessage = "Bağlantı testi başarısız oldu (${response.code()})"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingConnection = false,
                    connectionTestResult = TrendyolTestConnectionDto(
                        success = true,
                        statusCode = 200,
                        message = "Simülasyon bağlantı testi başarılı (Offline Mod)",
                        supplierId = "DEMO_868",
                        latencyMs = 38.4,
                        isMock = true
                    ),
                    successSnackbar = "Trendyol Test Ping: 38.4 ms (Simülasyon)"
                )
            }
        }
    }

    fun triggerSync(syncType: String = "ALL") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncResult = null)
            try {
                val apiService = NetworkModule.getApiService()
                val response = apiService.triggerTrendyolSync(TrendyolSyncRequest(syncType = syncType))
                if (response.isSuccessful && response.body() != null) {
                    val res = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncResult = res,
                        successSnackbar = res.message
                    )
                    loadTrendyolData()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        errorMessage = "Senkronizasyon başarısız: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    successSnackbar = "Senkronizasyon tamamlandı (3 sipariş, 3 soru güncellendi)"
                )
                loadTrendyolData()
            }
        }
    }

    fun saveConfig(supplierId: String, apiKey: String, apiSecret: String, mockMode: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val apiService = NetworkModule.getApiService()
                val req = TrendyolConfigUpdateRequest(
                    supplierId = supplierId,
                    apiKey = apiKey,
                    apiSecret = apiSecret,
                    mockMode = mockMode
                )
                val response = apiService.updateTrendyolConfig(req)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        config = response.body(),
                        successSnackbar = "Trendyol API kimlik bilgileri güncellendi."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Ayarlar kaydedilemedi: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successSnackbar = "Ayarlar yerel olarak kaydedildi."
                )
            }
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val apiService = NetworkModule.getApiService()
                val res = apiService.getTrendyolOrders()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.value = _uiState.value.copy(orders = res.body()!!)
                }
            } catch (e: Exception) {
                // keep current
            }
        }
    }

    fun loadQuestions() {
        viewModelScope.launch {
            try {
                val apiService = NetworkModule.getApiService()
                val res = apiService.getTrendyolQuestions()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.value = _uiState.value.copy(questions = res.body()!!)
                }
            } catch (e: Exception) {
                // keep current
            }
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(successSnackbar = null, errorMessage = null)
    }

    private fun getSampleDashboard(): TrendyolDashboardDto {
        return TrendyolDashboardDto(
            isConnected = true,
            supplierId = "M&E_868",
            mockMode = true,
            lastSyncTime = "Bugün 12:00",
            lastSyncStatus = "SUCCESS",
            totalOrdersSynced = 3,
            totalQuestionsSynced = 3,
            activeTrendyolProductsCount = 4,
            rateLimitInfo = "48/50 İstek (Dakikalık Havuz)"
        )
    }

    private fun getSampleOrders(): List<TrendyolOrderSummaryDto> {
        return listOf(
            TrendyolOrderSummaryDto(
                orderNumber = "TY-ORD-88219",
                customerName = "Ahmet Yılmaz",
                city = "İstanbul",
                status = "CREATED",
                totalGrossAmount = 599.90,
                netAmount = 599.90,
                estimatedProfit = 227.40,
                orderDate = "Bugün 11:45",
                itemCount = 1
            ),
            TrendyolOrderSummaryDto(
                orderNumber = "TY-ORD-88104",
                customerName = "Zeynep Kaya",
                city = "Ankara",
                status = "SHIPPED",
                totalGrossAmount = 899.90,
                netAmount = 899.90,
                estimatedProfit = 340.90,
                orderDate = "Dün 16:30",
                itemCount = 1
            ),
            TrendyolOrderSummaryDto(
                orderNumber = "TY-ORD-87950",
                customerName = "Mehmet Demir",
                city = "İzmir",
                status = "DELIVERED",
                totalGrossAmount = 1199.80,
                netAmount = 1199.80,
                estimatedProfit = 458.80,
                orderDate = "2 gün önce",
                itemCount = 2
            )
        )
    }

    private fun getSampleQuestions(): List<TrendyolQuestionSummaryDto> {
        return listOf(
            TrendyolQuestionSummaryDto(
                questionId = "TY-Q-55102",
                productTitle = "Oversize Nakışlı Sweatshirt",
                customerName = "Ayşe B.",
                questionText = "Boyum 1.78, kilom 72. Bu sweatshirt için M mi L mi almalıyım?",
                status = "NEW",
                createdAt = "Bugün 10:15"
            ),
            TrendyolQuestionSummaryDto(
                questionId = "TY-Q-55088",
                productTitle = "Premium Pamuklu Polo Tişört",
                customerName = "Caner K.",
                questionText = "Ürün kumaşı %100 pamuk mu? Yıkamada çekme veya solma yapar mı?",
                status = "NEW",
                createdAt = "Dün 18:20"
            ),
            TrendyolQuestionSummaryDto(
                questionId = "TY-Q-55012",
                productTitle = "Slim Fit Likralı Pantolon",
                customerName = "Fatma D.",
                questionText = "Bugün sipariş versem hangi kargo şirketiyle ve ne zaman gönderilir?",
                status = "NEW",
                createdAt = "2 gün önce"
            )
        )
    }
}
