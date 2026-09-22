package com.example.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUiState(
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val isGeneratingAdvice: Boolean = false,
    val isOptimizingContent: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val minDaysFilter: Int = 20,
    val stagnantReport: StagnantStockReportResponseDto = StagnantStockReportResponseDto(),
    val listingAudits: ListingAuditsResponseDto = ListingAuditsResponseDto(),
    val executiveSummary: ExecutiveReportResponseDto = ExecutiveReportResponseDto(
        reportDate = "",
        generatedByModel = "",
        overallHealthScore = 0,
        headline = "",
        executiveSummaryText = ""
    ),
    val selectedProductId: Int? = null,
    val pricingAdvice: PricingAdviceResponseDto? = null,
    val optimizedContent: ContentOptimizationResponseDto? = null,
    val targetMarginInput: Double = 25.0,
    val targetAudienceInput: String = "Genç & Dinamik Günlük Giyim",
    val toneOfVoiceInput: String = "Trend, Şık ve Bilgilendirici"
)

class ReportsViewModel : ViewModel() {

    private val api get() = NetworkModule.getApiService()

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadAllReports()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
        when (index) {
            0 -> loadStagnantStock()
            1 -> loadListingAudits()
            2 -> {
                val currentProdId = _uiState.value.selectedProductId
                if (currentProdId == null && _uiState.value.listingAudits.products.isNotEmpty()) {
                    val firstProdId = _uiState.value.listingAudits.products.first().productId
                    selectProductForAnalysis(firstProdId)
                }
            }
            3 -> loadExecutiveSummary()
        }
    }

    fun loadAllReports() {
        loadStagnantStock()
        loadListingAudits()
        loadExecutiveSummary()
    }

    fun loadStagnantStock(minDays: Int = _uiState.value.minDaysFilter) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, minDaysFilter = minDays) }
            try {
                val response = api.getStagnantStockReport(minDays = minDays)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            stagnantReport = response.body()!!,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Hareketsiz stok raporu alınamadı: HTTP ${response.code()}",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Bağlantı hatası: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadListingAudits() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.getListingQualityAudits()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val firstId = data.products.firstOrNull()?.productId
                    _uiState.update {
                        it.copy(
                            listingAudits = data,
                            selectedProductId = it.selectedProductId ?: firstId,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "İlan kalite denetimi alınamadı: HTTP ${response.code()}",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Bağlantı hatası: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun loadExecutiveSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.getExecutiveSummary()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            executiveSummary = response.body()!!,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Yönetici raporu alınamadı: HTTP ${response.code()}",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Bağlantı hatası: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectProductForAnalysis(productId: Int) {
        _uiState.update { it.copy(selectedProductId = productId, pricingAdvice = null, optimizedContent = null) }
        generatePricingAdvice(productId)
    }

    fun onTargetMarginChanged(margin: Double) {
        _uiState.update { it.copy(targetMarginInput = margin) }
        val prodId = _uiState.value.selectedProductId
        if (prodId != null) {
            generatePricingAdvice(prodId)
        }
    }

    fun onTargetAudienceChanged(audience: String) {
        _uiState.update { it.copy(targetAudienceInput = audience) }
    }

    fun onToneOfVoiceChanged(tone: String) {
        _uiState.update { it.copy(toneOfVoiceInput = tone) }
    }

    fun generatePricingAdvice(productId: Int? = _uiState.value.selectedProductId) {
        val targetId = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingAdvice = true, errorMessage = null) }
            try {
                val req = PricingAdviceRequestDto(
                    targetMarginPercent = _uiState.value.targetMarginInput
                )
                val response = api.getPricingAdvice(productId = targetId, request = req)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            pricingAdvice = response.body()!!,
                            isGeneratingAdvice = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Fiyatlandırma tavsiyesi hesaplanamadı: HTTP ${response.code()}",
                            isGeneratingAdvice = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Hata: ${e.localizedMessage}",
                        isGeneratingAdvice = false
                    )
                }
            }
        }
    }

    fun generateOptimizedContent(productId: Int? = _uiState.value.selectedProductId) {
        val targetId = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isOptimizingContent = true, errorMessage = null) }
            try {
                val req = ContentOptimizationRequestDto(
                    targetAudience = _uiState.value.targetAudienceInput,
                    toneOfVoice = _uiState.value.toneOfVoiceInput
                )
                val response = api.optimizeProductContent(productId = targetId, request = req)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            optimizedContent = response.body()!!,
                            isOptimizingContent = false,
                            successMessage = "AI SEO Başlık ve Açıklama taslağı başarıyla üretildi!"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "İçerik optimizasyonu başarısız: HTTP ${response.code()}",
                            isOptimizingContent = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Hata: ${e.localizedMessage}",
                        isOptimizingContent = false
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
