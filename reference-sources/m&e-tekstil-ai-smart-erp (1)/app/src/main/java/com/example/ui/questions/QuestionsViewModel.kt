package com.example.ui.questions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionsUiState(
    val isLoading: Boolean = false,
    val isGeneratingDraft: Boolean = false,
    val isApproving: Boolean = false,
    val isSending: Boolean = false,
    val isSyncing: Boolean = false,
    val questions: List<CustomerQuestionDto> = emptyList(),
    val summary: CustomerQuestionsSummaryDto = CustomerQuestionsSummaryDto(),
    val selectedStatusFilter: String = "ALL", // ALL, NEW, DRAFT_GENERATED, APPROVED, SENT
    val searchQuery: String = "",
    val selectedQuestion: CustomerQuestionDto? = null,
    val editingDraftId: Int? = null,
    val editedAnswerText: String = "",
    val selectedTone: String = "KURUMSAL", // KURUMSAL, SAMIMI, KISA_VE_OZ
    val selectedSlot: Int? = null, // null for auto, 1 (Gemini), 2 (OpenAI), 3 (Custom)
    val additionalInstructions: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class QuestionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionsUiState())
    val uiState: StateFlow<QuestionsUiState> = _uiState.asStateFlow()

    private val api get() = NetworkModule.getApiService()

    init {
        loadSummary()
        loadQuestions()
    }

    fun loadSummary() {
        viewModelScope.launch {
            try {
                val res = api.getQuestionsSummary()
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update { it.copy(summary = res.body()!!) }
                }
            } catch (e: Exception) {
                // Keep default summary
            }
        }
    }

    fun loadQuestions(status: String? = _uiState.value.selectedStatusFilter, search: String? = _uiState.value.searchQuery) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val filter = if (status == "ALL" || status.isNullOrBlank()) null else status
                val res = api.getQuestions(
                    status = filter,
                    search = if (search.isNullOrBlank()) null else search.trim(),
                    limit = 50,
                    offset = 0
                )
                if (res.isSuccessful && res.body() != null) {
                    val questionsList: List<CustomerQuestionDto> = res.body()!!
                    _uiState.update { state ->
                        val currentSelectedId = state.selectedQuestion?.id
                        val updatedSelected = questionsList.find { q -> q.id == currentSelectedId } ?: state.selectedQuestion
                        state.copy(
                            isLoading = false,
                            questions = questionsList,
                            selectedQuestion = updatedSelected,
                            selectedStatusFilter = status ?: "ALL",
                            searchQuery = search ?: ""
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Sorular alınamadı: ${res.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Bağlantı hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun syncQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, successMessage = null) }
            try {
                val res = api.syncCustomerQuestions()
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            successMessage = body.message
                        )
                    }
                    loadSummary()
                    loadQuestions()
                } else {
                    _uiState.update { it.copy(isSyncing = false, errorMessage = "Senkronizasyon hatası: ${res.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSyncing = false, errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    fun selectQuestion(question: CustomerQuestionDto?) {
        _uiState.update { state ->
            val latestDraft = question?.drafts?.lastOrNull()
            state.copy(
                selectedQuestion = question,
                editingDraftId = null,
                editedAnswerText = latestDraft?.finalAnswer ?: "",
                additionalInstructions = ""
            )
        }
    }

    fun setStatusFilter(status: String) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
        loadQuestions(status = status)
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadQuestions(search = query)
    }

    fun setSelectedTone(tone: String) {
        _uiState.update { it.copy(selectedTone = tone) }
    }

    fun setSelectedSlot(slot: Int?) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun setAdditionalInstructions(instructions: String) {
        _uiState.update { it.copy(additionalInstructions = instructions) }
    }

    fun generateAiDraft(questionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingDraft = true, errorMessage = null, successMessage = null) }
            try {
                val req = GenerateDraftRequest(
                    providerSlot = _uiState.value.selectedSlot,
                    tone = _uiState.value.selectedTone,
                    additionalInstructions = _uiState.value.additionalInstructions.takeIf { it.isNotBlank() }
                )
                val res = api.generateAiDraft(questionId, req)
                if (res.isSuccessful && res.body() != null) {
                    val draft = res.body()!!
                    _uiState.update { state ->
                        state.copy(
                            isGeneratingDraft = false,
                            successMessage = "Yapay zekâ taslağı başarıyla oluşturuldu.",
                            editingDraftId = null,
                            editedAnswerText = draft.finalAnswer
                        )
                    }
                    refreshQuestionDetail(questionId)
                    loadSummary()
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    _uiState.update {
                        it.copy(
                            isGeneratingDraft = false,
                            errorMessage = "Taslak oluşturulamadı: $err"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingDraft = false, errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    fun startEditingDraft(draftId: Int, currentAnswer: String) {
        _uiState.update { it.copy(editingDraftId = draftId, editedAnswerText = currentAnswer) }
    }

    fun cancelEditingDraft() {
        _uiState.update { it.copy(editingDraftId = null) }
    }

    fun updateEditedAnswerText(text: String) {
        _uiState.update { it.copy(editedAnswerText = text) }
    }

    fun saveEditedDraft(draftId: Int, questionId: Int) {
        val newText = _uiState.value.editedAnswerText.trim()
        if (newText.length < 5) {
            _uiState.update { it.copy(errorMessage = "Cevap metni en az 5 karakter olmalıdır.") }
            return
        }

        viewModelScope.launch {
            try {
                val res = api.editDraft(draftId, EditDraftRequest(editedAnswer = newText))
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            editingDraftId = null,
                            successMessage = "Taslak düzenlemesi kaydedildi."
                        )
                    }
                    refreshQuestionDetail(questionId)
                } else {
                    _uiState.update { it.copy(errorMessage = "Düzenleme kaydedilemedi: ${res.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    fun approveDraft(draftId: Int, questionId: Int, operatorName: String = "OPERATOR") {
        viewModelScope.launch {
            _uiState.update { it.copy(isApproving = true, errorMessage = null, successMessage = null) }
            try {
                val res = api.approveDraft(draftId, ApproveDraftRequest(operatorName = operatorName))
                if (res.isSuccessful && res.body() != null) {
                    _uiState.update {
                        it.copy(
                            isApproving = false,
                            successMessage = "Taslak insan onayı aldı (APPROVED). Artık Trendyol'a gönderilebilir."
                        )
                    }
                    refreshQuestionDetail(questionId)
                    loadSummary()
                    loadQuestions()
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    _uiState.update {
                        it.copy(
                            isApproving = false,
                            errorMessage = "Onay başarısız: $err"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isApproving = false, errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    fun sendApprovedReply(questionId: Int, draftId: Int?, operatorName: String = "OPERATOR") {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null, successMessage = null) }
            try {
                val res = api.sendApprovedReply(
                    questionId = questionId,
                    request = SendReplyRequest(draftId = draftId, operatorName = operatorName)
                )
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    if (body.success) {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                successMessage = "Cevap Trendyol'a başarıyla iletildi!"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSending = false,
                                errorMessage = body.message
                            )
                        }
                    }
                    refreshQuestionDetail(questionId)
                    loadSummary()
                    loadQuestions()
                } else {
                    val err = res.errorBody()?.string() ?: res.message()
                    _uiState.update { it.copy(isSending = false, errorMessage = "Gönderim reddedildi: $err") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, errorMessage = "Gönderim hatası: ${e.localizedMessage}") }
            }
        }
    }

    fun rejectDraft(draftId: Int, questionId: Int, reason: String = "Uygun bulunmadı") {
        viewModelScope.launch {
            try {
                val res = api.rejectDraft(draftId, RejectDraftRequest(reason = reason))
                if (res.isSuccessful) {
                    _uiState.update { it.copy(successMessage = "Taslak reddedildi.") }
                    refreshQuestionDetail(questionId)
                    loadSummary()
                    loadQuestions()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Hata: ${e.localizedMessage}") }
            }
        }
    }

    private fun refreshQuestionDetail(questionId: Int) {
        viewModelScope.launch {
            try {
                val res = api.getQuestionDetail(questionId)
                if (res.isSuccessful && res.body() != null) {
                    val q = res.body()!!
                    _uiState.update { state ->
                        state.copy(
                            selectedQuestion = q,
                            questions = state.questions.map { item -> if (item.id == q.id) q else item }
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore detail refresh error
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
