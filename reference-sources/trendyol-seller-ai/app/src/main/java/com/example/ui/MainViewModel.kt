package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuestionEntity
import com.example.data.local.StoreSettingsEntity
import com.example.data.remote.OptimizedProductDescription
import com.example.data.remote.ProductMetadata
import com.example.data.repository.DashboardMetrics
import com.example.data.repository.QuickActionType
import com.example.data.repository.StoreRepository
import com.example.data.repository.ToolCallInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    ORDERS,
    PRODUCTS,
    QUESTIONS,
    AI_STUDIO,
    SETTINGS
}

sealed class SyncUiState {
    object Idle : SyncUiState()
    data class Syncing(val message: String = "جاري المزامنة مع Trendyol API...") : SyncUiState()
    data class Success(
        val productsCount: Int,
        val ordersCount: Int,
        val questionsCount: Int,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SyncUiState()
    data class Error(val errorMessage: String, val timestamp: Long = System.currentTimeMillis()) : SyncUiState()
}

data class AiChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "ai"
    val text: String,
    val timestamp: String = "الآن",
    val toolCallInfo: ToolCallInfo? = null,
    val attachedProducts: List<ProductEntity> = emptyList(),
    val quickActionType: QuickActionType = QuickActionType.NONE
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StoreRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = StoreRepository(db)
    }

    // Active screen navigation
    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    fun selectTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    // Products
    val allProducts = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedProductCategory = MutableStateFlow("الكل")
    val selectedProductCategory: StateFlow<String> = _selectedProductCategory.asStateFlow()

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        allProducts,
        _productSearchQuery,
        _selectedProductCategory
    ) { products, query, category ->
        products.filter { p ->
            val matchQuery = query.isBlank() ||
                    p.title.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.brand.contains(query, ignoreCase = true)
            val matchCategory = category == "الكل" || p.category.contains(category, ignoreCase = true)
            matchQuery && matchCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setProductSearchQuery(q: String) {
        _productSearchQuery.value = q
    }

    fun setProductCategory(cat: String) {
        _selectedProductCategory.value = cat
    }

    // Orders
    val allOrders = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _orderStatusFilter = MutableStateFlow("الكل")
    val orderStatusFilter: StateFlow<String> = _orderStatusFilter.asStateFlow()

    val filteredOrders: StateFlow<List<OrderEntity>> = combine(
        allOrders,
        _orderStatusFilter
    ) { orders, filter ->
        when (filter) {
            "الكل" -> orders
            "جديد" -> orders.filter { it.status == "Created" }
            "قيد التجهيز" -> orders.filter { it.status == "Picking" }
            "بالشحن" -> orders.filter { it.status == "Shipped" }
            "مكتمل" -> orders.filter { it.status == "Delivered" }
            "مرتجع" -> orders.filter { it.status == "Returned" }
            else -> orders
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setOrderStatusFilter(filter: String) {
        _orderStatusFilter.value = filter
    }

    // Questions
    val allQuestions = repository.allQuestions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard metrics
    val dashboardStats: StateFlow<DashboardMetrics> = repository.dashboardStats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DashboardMetrics(0.0, 0, 0, 0, 0, 0)
        )

    // Store settings
    val storeSettings: StateFlow<StoreSettingsEntity?> = repository.storeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Trendyol Live Sync State
    private val _syncState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    fun clearSyncState() {
        _syncState.value = SyncUiState.Idle
    }

    fun testTrendyolConnection(
        supplierId: String,
        apiKey: String,
        apiSecret: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _syncState.value = SyncUiState.Syncing("جاري التحقق من بيانات الاتصال بسيرفرات ترنديول...")
            val (isSuccess, msg) = repository.testTrendyolConnection(supplierId, apiKey, apiSecret)
            if (isSuccess) {
                _syncState.value = SyncUiState.Idle
            } else {
                _syncState.value = SyncUiState.Error(msg)
            }
            onResult(isSuccess, msg)
        }
    }

    fun syncWithTrendyol(
        supplierId: String? = null,
        apiKey: String? = null,
        apiSecret: String? = null
    ) {
        viewModelScope.launch {
            val sId = supplierId?.trim() ?: storeSettings.value?.supplierId?.trim() ?: ""
            val aKey = apiKey?.trim() ?: storeSettings.value?.apiKey?.trim() ?: ""
            val aSec = apiSecret?.trim() ?: storeSettings.value?.apiSecret?.trim() ?: ""

            if (sId.isBlank() || aKey.isBlank() || aSec.isBlank()) {
                _syncState.value = SyncUiState.Error("يرجى إدخال بيانات الربط: Supplier ID و API Key و API Secret.")
                return@launch
            }

            _syncState.value = SyncUiState.Syncing("جاري الاتصال بـ Trendyol ومزامنة المنتجات والطلبات والأسئلة...")
            try {
                val result = repository.syncTrendyolData(sId, aKey, aSec)
                if (result.isSuccess) {
                    _syncState.value = SyncUiState.Success(
                        productsCount = result.productsCount,
                        ordersCount = result.ordersCount,
                        questionsCount = result.questionsCount,
                        message = result.message
                    )
                } else {
                    _syncState.value = SyncUiState.Error(result.message)
                }
            } catch (e: Exception) {
                _syncState.value = SyncUiState.Error("حدث خطأ أثناء المزامنة: ${e.localizedMessage ?: "يرجى المحاولة مجدداً"}")
            }
        }
    }

    // AI state
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiOperationResult = MutableStateFlow<String?>(null)
    val aiOperationResult: StateFlow<String?> = _aiOperationResult.asStateFlow()

    private val _optimizedDescriptionResult = MutableStateFlow<OptimizedProductDescription?>(null)
    val optimizedDescriptionResult: StateFlow<OptimizedProductDescription?> = _optimizedDescriptionResult.asStateFlow()

    // AI Chat history
    private val _chatMessages = MutableStateFlow(
        listOf(
            AiChatMessage(
                sender = "ai",
                text = "مرحباً بك في مساعد ترنديول الذكي الوظيفي (AI Agent Copilot)!\n\nأنا لست مجرد شات عادي، بل مرتبط مباشرة بوظائف وقاعدة بيانات متجرك:\n• يمكنك سؤالي: \"أظهر المنتجات التي مخزونها أقل من 5\"\n• \"كم ربحي اليوم؟\"\n• \"اعمل وصف للمنتج\"\n• \"ما هي الطلبات بانتظار الشحن؟\"\n\nكيف يمكنني مساعدتك الآن؟"
            )
        )
    )
    val chatMessages: StateFlow<List<AiChatMessage>> = _chatMessages.asStateFlow()

    // Product stock and price controls
    fun updateStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            val product = allProducts.value.find { it.id == productId } ?: return@launch
            val newStock = (product.stockCount + delta).coerceAtLeast(0)
            repository.updateProductStock(productId, newStock)
        }
    }

    fun updatePrice(productId: Long, newPrice: Double) {
        viewModelScope.launch {
            repository.updateProductPrice(productId, newPrice)
        }
    }

    fun toggleProductActive(productId: Long) {
        viewModelScope.launch {
            val product = allProducts.value.find { it.id == productId } ?: return@launch
            repository.toggleProductActive(productId, !product.isActive)
        }
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.addProduct(product)
        }
    }

    fun updateProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun advanceOrderStatus(order: OrderEntity) {
        viewModelScope.launch {
            val nextStatus = when (order.status) {
                "Created" -> "Picking"
                "Picking" -> "Shipped"
                "Shipped" -> "Delivered"
                else -> order.status
            }
            repository.updateOrderStatus(order.id, nextStatus)
        }
    }

    fun markOrderReturned(orderId: Long) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "Returned")
        }
    }

    // AI question auto-reply
    fun generateAiReply(question: QuestionEntity) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val apiKey = storeSettings.value?.geminiApiKey
                repository.generateAiReplyForQuestion(question, apiKey)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun submitQuestionReply(questionId: Long, replyText: String) {
        viewModelScope.launch {
            repository.submitQuestionReply(questionId, replyText)
        }
    }

    // AI Listing generator
    fun generateListingWithAi(name: String, brand: String, category: String, keywords: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val apiKey = storeSettings.value?.geminiApiKey
                val result = repository.generateProductListing(name, brand, category, keywords, apiKey)
                _aiOperationResult.value = result
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    // AI Buybox analysis
    fun analyzeBuyboxWithAi(product: ProductEntity) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val apiKey = storeSettings.value?.geminiApiKey
                val result = repository.analyzeBuyboxStrategy(product, apiKey)
                _aiOperationResult.value = result
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearAiResult() {
        _aiOperationResult.value = null
    }

    /**
     * Generates a complete SEO and conversion-optimized product description using Gemini API based on ProductMetadata.
     */
    fun generateOptimizedProductDescription(
        metadata: ProductMetadata,
        onComplete: ((OptimizedProductDescription) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val apiKey = storeSettings.value?.geminiApiKey
                val result = repository.generateOptimizedProductDescription(metadata, apiKey)
                _optimizedDescriptionResult.value = result
                _aiOperationResult.value = result.rawFormattedOutput
                onComplete?.invoke(result)
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearOptimizedDescriptionResult() {
        _optimizedDescriptionResult.value = null
    }

    // AI Agent Chat with Real Function / Tool Calling on Database
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        val userMsg = AiChatMessage(sender = "user", text = text)
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                val customKey = storeSettings.value?.geminiApiKey
                val agentResponse = repository.processAgentCommand(text, customKey)
                
                val aiMsg = AiChatMessage(
                    sender = "ai",
                    text = agentResponse.displayText,
                    toolCallInfo = agentResponse.toolCallInfo,
                    attachedProducts = agentResponse.attachedProducts,
                    quickActionType = agentResponse.quickActionType
                )
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                val errorMsg = AiChatMessage(
                    sender = "ai",
                    text = "حدث خطأ أثناء معالجة الطلب: ${e.localizedMessage ?: "يرجى التحقق من المدخلات"}"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun saveSettings(
        storeName: String,
        supplierId: String,
        apiKey: String,
        apiSecret: String,
        isLiveMode: Boolean,
        geminiApiKey: String = "",
        telegramBotToken: String = "",
        telegramChatId: String = "",
        isTelegramEnabled: Boolean = false,
        notifyNewOrders: Boolean = true,
        notifyCustomerQuestions: Boolean = true,
        notifyLowStock: Boolean = true,
        notifyDailyReport: Boolean = true,
        notifyAiSlowMoving: Boolean = true
    ) {
        viewModelScope.launch {
            val current = storeSettings.value
            val updated = (current ?: StoreSettingsEntity()).copy(
                storeName = storeName,
                supplierId = supplierId,
                apiKey = apiKey,
                apiSecret = apiSecret,
                isLiveMode = isLiveMode,
                geminiApiKey = geminiApiKey,
                telegramBotToken = telegramBotToken,
                telegramChatId = telegramChatId,
                isTelegramEnabled = isTelegramEnabled,
                notifyNewOrders = notifyNewOrders,
                notifyCustomerQuestions = notifyCustomerQuestions,
                notifyLowStock = notifyLowStock,
                notifyDailyReport = notifyDailyReport,
                notifyAiSlowMoving = notifyAiSlowMoving
            )
            repository.saveSettings(updated)

            // Auto-trigger live synchronization if keys are supplied
            if (supplierId.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank()) {
                syncWithTrendyol(supplierId, apiKey, apiSecret)
            }
        }
    }

    fun testTelegramConnection(
        botToken: String,
        chatId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val name = storeSettings.value?.storeName ?: "Trendyol Partner Store"
            val res = repository.testTelegramConnection(botToken, chatId, name)
            onResult(res.first, res.second)
        }
    }

    fun sendTelegramDailyReport(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val apiKey = storeSettings.value?.geminiApiKey
            val res = repository.sendTelegramDailyReport(apiKey)
            onResult(res.first, res.second)
        }
    }

    fun sendTelegramSlowMovingAdvice(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val apiKey = storeSettings.value?.geminiApiKey
            val res = repository.sendTelegramSlowMovingAdvice(apiKey)
            onResult(res.first, res.second)
        }
    }

    fun sendTelegramLowStockAlert(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.sendTelegramLowStockAlert()
            onResult(res.first, res.second)
        }
    }
}

