package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DashboardSummaryResponseDto
import com.example.data.model.TrendyolSyncRequest
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val summary: DashboardSummaryResponseDto = DashboardSummaryResponseDto()
)

class DashboardViewModel : ViewModel() {

    private val api get() = NetworkModule.getApiService()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardSummary()
    }

    fun loadDashboardSummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = api.getDashboardSummary()
                if (response.isSuccessful && response.body() != null) {
                    _uiState.update {
                        it.copy(
                            summary = response.body()!!,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Gösterge paneli verisi alınamadı: HTTP ${response.code()}",
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

    fun triggerQuickTrendyolSync() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                val response = api.triggerTrendyolSync(TrendyolSyncRequest(syncType = "ALL"))
                if (response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            successMessage = "Trendyol ürün ve siparişleri başarıyla eşitlendi!"
                        )
                    }
                    loadDashboardSummary()
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Eşitleme başarısız: HTTP ${response.code()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = "Hata: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
