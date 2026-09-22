package com.example.ui.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TelegramUiState(
    val isLoading: Boolean = false,
    val isSavingConfig: Boolean = false,
    val isSendingTest: Boolean = false,
    val isRetrying: Boolean = false,
    val isSendingDigest: Boolean = false,
    val config: TelegramConfigDto = TelegramConfigDto(),
    val eventsResponse: TelegramEventsListResponseDto = TelegramEventsListResponseDto(),
    val auditResponse: AuditLogListResponseDto = AuditLogListResponseDto(),
    val selectedTab: Int = 0, // 0: Bot & Ayarlar, 1: Bildirim Kuyruğu, 2: Denetim İzi (Audit)
    val statusFilter: String = "ALL", // ALL, PENDING, SENT, FAILED, SKIPPED
    val auditFilter: String = "ALL", // ALL, INVENTORY_CHANGE, TELEGRAM_SENT, AI_APPROVAL, TRENDYOL_SYNC
    val inputBotToken: String = "",
    val inputChatId: String = "",
    val isEnabledToggle: Boolean = false,
    val notifyNewOrder: Boolean = true,
    val notifyLowStock: Boolean = true,
    val notifyNewQuestion: Boolean = true,
    val notifyDailyDigest: Boolean = true,
    val notifySystemError: Boolean = true,
    val testMessageInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class TelegramViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TelegramUiState())
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    private val api get() = NetworkModule.getApiService()

    init {
        loadConfig()
        loadEvents()
        loadAuditLogs()
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
        when (tabIndex) {
            0 -> loadConfig()
            1 -> loadEvents()
            2 -> loadAuditLogs()
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(statusFilter = status) }
        loadEvents()
    }

    fun setAuditFilter(filter: String) {
        _uiState.update { it.copy(auditFilter = filter) }
        loadAuditLogs()
    }

    fun onBotTokenChanged(token: String) {
        _uiState.update { it.copy(inputBotToken = token) }
    }

    fun onChatIdChanged(chatId: String) {
        _uiState.update { it.copy(inputChatId = chatId) }
    }

    fun onTestMessageChanged(msg: String) {
        _uiState.update { it.copy(testMessageInput = msg) }
    }

    fun toggleEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isEnabledToggle = enabled) }
    }

    fun toggleCategory(category: String, enabled: Boolean) {
        _uiState.update {
            when (category) {
                "NEW_ORDER" -> it.copy(notifyNewOrder = enabled)
                "LOW_STOCK" -> it.copy(notifyLowStock = enabled)
                "NEW_QUESTION" -> it.copy(notifyNewQuestion = enabled)
                "DAILY_DIGEST" -> it.copy(notifyDailyDigest = enabled)
                "SYSTEM_ERROR" -> it.copy(notifySystemError = enabled)
                else -> it
            }
        }
    }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val res = api.getTelegramConfig()
                if (res.isSuccessful && res.body() != null) {
                    val cfg = res.body()!!
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            config = cfg,
                            isEnabledToggle = cfg.enabled,
                            inputChatId = cfg.chatId ?: "",
                            notifyNewOrder = cfg.notifyNewOrder,
                            notifyLowStock = cfg.notifyLowStock,
                            notifyNewQuestion = cfg.notifyNewQuestion,
                            notifyDailyDigest = cfg.notifyDailyDigest,
                            notifySystemError = cfg.notifySystemError
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Telegram ayarları yüklenemedi: ${res.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Bağlantı hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingConfig = true, errorMessage = null, successMessage = null) }
            try {
                val req = TelegramConfigUpdateRequest(
                    enabled = _uiState.value.isEnabledToggle,
                    botToken = if (_uiState.value.inputBotToken.isNotBlank()) _uiState.value.inputBotToken.trim() else null,
                    chatId = if (_uiState.value.inputChatId.isNotBlank()) _uiState.value.inputChatId.trim() else null,
                    notifyNewOrder = _uiState.value.notifyNewOrder,
                    notifyLowStock = _uiState.value.notifyLowStock,
                    notifyNewQuestion = _uiState.value.notifyNewQuestion,
                    notifyDailyDigest = _uiState.value.notifyDailyDigest,
                    notifySystemError = _uiState.value.notifySystemError
                )
                val res = api.updateTelegramConfig(req)
                if (res.isSuccessful && res.body() != null) {
                    val updated = res.body()!!
                    _uiState.update {
                        it.copy(
                            isSavingConfig = false,
                            config = updated,
                            inputBotToken = "",
                            successMessage = "Telegram yapılandırması başarıyla kaydedildi!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSavingConfig = false, errorMessage = "Kaydetme başarısız: ${res.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingConfig = false, errorMessage = "Kaydetme hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun sendTestMessage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingTest = true, errorMessage = null, successMessage = null) }
            try {
                val customMsg = if (_uiState.value.testMessageInput.isNotBlank()) _uiState.value.testMessageInput.trim() else null
                val res = api.sendTelegramTest(TelegramSendTestRequest(message = customMsg))
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    if (data.success) {
                        _uiState.update {
                            it.copy(
                                isSendingTest = false,
                                successMessage = "Test mesajı Telegram kanalına başarıyla ulaştı!",
                                testMessageInput = ""
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSendingTest = false,
                                errorMessage = "Bildirim kuyruğa alındı ancak gönderilemedi: ${data.error ?: data.status}"
                            )
                        }
                    }
                    loadEvents()
                } else {
                    _uiState.update { it.copy(isSendingTest = false, errorMessage = "Test isteği başarısız: ${res.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingTest = false, errorMessage = "Test hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun sendDailyDigest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingDigest = true, errorMessage = null, successMessage = null) }
            try {
                val res = api.sendDailyDigestNow()
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    _uiState.update {
                        it.copy(
                            isSendingDigest = false,
                            successMessage = "Günlük satış ve net kâr özeti Telegram kanalına gönderildi!"
                        )
                    }
                    loadEvents()
                } else {
                    _uiState.update { it.copy(isSendingDigest = false, errorMessage = "Özet gönderimi başarısız: ${res.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSendingDigest = false, errorMessage = "Özet gönderim hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun retryFailedEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRetrying = true, errorMessage = null, successMessage = null) }
            try {
                val res = api.retryTelegramEvents(maxRetries = 5)
                if (res.isSuccessful && res.body() != null) {
                    val data = res.body()!!
                    _uiState.update {
                        it.copy(
                            isRetrying = false,
                            successMessage = data.message
                        )
                    }
                    loadEvents()
                } else {
                    _uiState.update { it.copy(isRetrying = false, errorMessage = "Yeniden deneme başarısız: ${res.message()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isRetrying = false, errorMessage = "Yeniden deneme hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            try {
                val filter = if (_uiState.value.statusFilter == "ALL") null else _uiState.value.statusFilter
                val res = api.getTelegramEvents(status = filter, limit = 50)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(eventsResponse = res.body()!!) }
                }
            } catch (e: Exception) {
                // Keep current state
            }
        }
    }

    fun loadAuditLogs() {
        viewModelScope.launch {
            try {
                val filter = if (_uiState.value.auditFilter == "ALL") null else _uiState.value.auditFilter
                val res = api.getAuditLogs(eventType = filter, limit = 50)
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(auditResponse = res.body()!!) }
                }
            } catch (e: Exception) {
                // Keep current state
            }
        }
    }

    fun clearDeliveredEvents() {
        viewModelScope.launch {
            try {
                api.clearTelegramEvents(status = "SENT")
                loadEvents()
            } catch (e: Exception) {
                // Silently handle
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
