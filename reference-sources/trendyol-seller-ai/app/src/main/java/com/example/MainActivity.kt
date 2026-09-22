package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ProductEntity
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.TrendyolBottomNav
import com.example.ui.components.TrendyolTopBar
import com.example.ui.screens.AddEditProductDialog
import com.example.ui.screens.AiStudioScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.QuestionsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TrendyolSellerApp()
            }
        }
    }
}

@Composable
fun TrendyolSellerApp(viewModel: MainViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val products by viewModel.filteredProducts.collectAsStateWithLifecycle()
    val allProductsList by viewModel.allProducts.collectAsStateWithLifecycle()
    val orders by viewModel.filteredOrders.collectAsStateWithLifecycle()
    val allOrdersList by viewModel.allOrders.collectAsStateWithLifecycle()
    val questions by viewModel.allQuestions.collectAsStateWithLifecycle()
    val dashboardMetrics by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()

    val searchQuery by viewModel.productSearchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedProductCategory.collectAsStateWithLifecycle()
    val orderFilter by viewModel.orderStatusFilter.collectAsStateWithLifecycle()

    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val aiOperationResult by viewModel.aiOperationResult.collectAsStateWithLifecycle()
    val optimizedDescriptionResult by viewModel.optimizedDescriptionResult.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showBuyboxResultDialog by remember { mutableStateOf(false) }

    val pendingQuestionsCount = questions.count { !it.isAnswered }
    val pendingOrdersCount = allOrdersList.count { it.status == "Created" || it.status == "Picking" }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TrendyolTopBar(
                settings = storeSettings,
                syncState = syncState,
                onSyncClick = { viewModel.syncWithTrendyol() },
                onSettingsClick = { viewModel.selectTab(ScreenTab.SETTINGS) }
            )
        },
        bottomBar = {
            TrendyolBottomNav(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                pendingQuestionsCount = pendingQuestionsCount,
                pendingOrdersCount = pendingOrdersCount
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                ScreenTab.DASHBOARD -> DashboardScreen(
                    metrics = dashboardMetrics,
                    products = allProductsList,
                    orders = allOrdersList,
                    questions = questions,
                    onNavigate = { viewModel.selectTab(it) },
                    onRestockClick = { product ->
                        viewModel.updateStock(product.id, 10)
                    },
                    onAnswerQuestionClick = { question ->
                        viewModel.selectTab(ScreenTab.QUESTIONS)
                        viewModel.generateAiReply(question)
                    }
                )

                ScreenTab.ORDERS -> OrdersScreen(
                    orders = orders,
                    selectedFilter = orderFilter,
                    onFilterChange = { viewModel.setOrderStatusFilter(it) },
                    onAdvanceOrder = { viewModel.advanceOrderStatus(it) },
                    onMarkReturned = { viewModel.markOrderReturned(it) }
                )

                ScreenTab.PRODUCTS -> ProductsScreen(
                    products = products,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchChange = { viewModel.setProductSearchQuery(it) },
                    onCategoryChange = { viewModel.setProductCategory(it) },
                    onStockChange = { id, delta -> viewModel.updateStock(id, delta) },
                    onToggleActive = { viewModel.toggleProductActive(it) },
                    onEditProduct = {
                        editingProduct = it
                        showAddEditDialog = true
                    },
                    onDeleteProduct = { viewModel.deleteProduct(it) },
                    onAnalyzeBuybox = { product ->
                        viewModel.analyzeBuyboxWithAi(product)
                        showBuyboxResultDialog = true
                    }
                )

                ScreenTab.QUESTIONS -> QuestionsScreen(
                    questions = questions,
                    isAiLoading = isAiLoading,
                    onGenerateAiReply = { viewModel.generateAiReply(it) },
                    onSubmitReply = { id, reply -> viewModel.submitQuestionReply(id, reply) }
                )

                ScreenTab.AI_STUDIO -> AiStudioScreen(
                    chatMessages = chatMessages,
                    isAiLoading = isAiLoading,
                    aiOperationResult = aiOperationResult,
                    optimizedDescriptionResult = optimizedDescriptionResult,
                    products = allProductsList,
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onGenerateListing = { name, brand, cat, kw ->
                        viewModel.generateListingWithAi(name, brand, cat, kw)
                    },
                    onGenerateOptimizedDescription = { metadata ->
                        viewModel.generateOptimizedProductDescription(metadata)
                    },
                    onAnalyzeBuybox = { product ->
                        viewModel.analyzeBuyboxWithAi(product)
                    },
                    onClearResult = {
                        viewModel.clearAiResult()
                        viewModel.clearOptimizedDescriptionResult()
                    }
                )

                ScreenTab.SETTINGS -> SettingsScreen(
                    settings = storeSettings,
                    syncState = syncState,
                    onSaveSettings = { name, suppId, key, sec, live, gemKey, botToken, chatId, enabled, newOrd, custQ, lowStk, dailyRep, slowMov ->
                        viewModel.saveSettings(
                            storeName = name,
                            supplierId = suppId,
                            apiKey = key,
                            apiSecret = sec,
                            isLiveMode = live,
                            geminiApiKey = gemKey,
                            telegramBotToken = botToken,
                            telegramChatId = chatId,
                            isTelegramEnabled = enabled,
                            notifyNewOrders = newOrd,
                            notifyCustomerQuestions = custQ,
                            notifyLowStock = lowStk,
                            notifyDailyReport = dailyRep,
                            notifyAiSlowMoving = slowMov
                        )
                    },
                    onSyncNow = { suppId, key, sec ->
                        viewModel.syncWithTrendyol(suppId, key, sec)
                    },
                    onTestConnection = { suppId, key, sec, onRes ->
                        viewModel.testTrendyolConnection(suppId, key, sec, onRes)
                    },
                    onTestTelegram = { botToken, chatId, onRes ->
                        viewModel.testTelegramConnection(botToken, chatId, onRes)
                    },
                    onSendTelegramDailyReport = { onRes ->
                        viewModel.sendTelegramDailyReport(onRes)
                    },
                    onSendTelegramSlowMovingAdvice = { onRes ->
                        viewModel.sendTelegramSlowMovingAdvice(onRes)
                    },
                    onSendTelegramLowStockAlert = { onRes ->
                        viewModel.sendTelegramLowStockAlert(onRes)
                    }
                )
            }
        }

        // Product Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditProductDialog(
                initialProduct = editingProduct,
                onDismiss = {
                    showAddEditDialog = false
                    editingProduct = null
                },
                onSave = { product ->
                    if (editingProduct != null) {
                        viewModel.updateProduct(product)
                    } else {
                        viewModel.addProduct(product)
                    }
                    showAddEditDialog = false
                    editingProduct = null
                },
                onGenerateAiDescription = { metadata, onResult ->
                    viewModel.generateOptimizedProductDescription(metadata) { optDesc ->
                        onResult(optDesc)
                    }
                }
            )
        }

        // Buybox AI Analysis Modal Dialog
        if (showBuyboxResultDialog && aiOperationResult != null) {
            AlertDialog(
                onDismissRequest = {
                    showBuyboxResultDialog = false
                    viewModel.clearAiResult()
                },
                title = {
                    Text(
                        text = "تحليل خوارزمية الباي بوكس (Buybox)",
                        fontWeight = FontWeight.Bold,
                        color = TrendyolOrangeDark
                    )
                },
                text = {
                    Text(
                        text = aiOperationResult ?: "",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBuyboxResultDialog = false
                            viewModel.clearAiResult()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                    ) {
                        Text(text = "تم وفهمت التوصية", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
