package com.example.ui.trendyol

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TrendyolOrderSummaryDto
import com.example.data.model.TrendyolQuestionSummaryDto
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendyolScreen(
    viewModel: TrendyolViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successSnackbar) {
        uiState.successSnackbar?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Trendyol Entegrasyonu",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Trendyol Orange badge
                            Surface(
                                color = PrimaryOrange,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PARTNER API",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Sipariş, Soru ve Stok Senkronizasyonu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.testConnection() },
                        enabled = !uiState.isTestingConnection,
                        modifier = Modifier.testTag("btn_test_trendyol_connection")
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.NetworkPing,
                                contentDescription = "Ping Testi",
                                tint = PrimaryOrange
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.triggerSync("ALL") },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.testTag("btn_sync_trendyol_all")
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryOrange
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Tümünü Senkronize Et",
                                tint = PrimaryOrange
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Tab Selector Row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrendyolTab.values().forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_trendyol_${tab.name.lowercase()}")
                    )
                }
            }

            // Connection Test Banner (if available)
            AnimatedVisibility(visible = uiState.connectionTestResult != null) {
                uiState.connectionTestResult?.let { test ->
                    Surface(
                        color = if (test.success) SuccessContainer else ErrorContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (test.success) SuccessGreen.copy(alpha = 0.4f) else ErrorRed.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (test.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (test.success) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = test.message,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (test.success) SuccessGreen else ErrorRed
                                )
                                Text(
                                    text = "Gecikme: ${test.latencyMs} ms • Durum: ${test.statusCode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Content per Tab
            when (uiState.selectedTab) {
                TrendyolTab.DASHBOARD -> TrendyolDashboardContent(viewModel = viewModel, uiState = uiState)
                TrendyolTab.ORDERS -> TrendyolOrdersContent(orders = uiState.orders, onRefresh = { viewModel.loadOrders() })
                TrendyolTab.QUESTIONS -> TrendyolQuestionsContent(questions = uiState.questions, onRefresh = { viewModel.loadQuestions() })
                TrendyolTab.CONFIG -> TrendyolConfigContent(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@Composable
fun TrendyolDashboardContent(
    viewModel: TrendyolViewModel,
    uiState: TrendyolUiState
) {
    val dash = uiState.dashboard

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card: Integration & Rate Limit Status
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (dash?.isConnected == true) SuccessGreen else ErrorRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (dash?.isConnected == true) "Bağlantı Aktif" else "Bağlantı Kurulmadı",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Surface(
                            color = PrimaryOrange.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Satıcı ID: ${dash?.supplierId ?: "Yok"}",
                                color = PrimaryOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Son Başarılı Senkronizasyon", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                text = dash?.lastSyncTime ?: "Bugün Yapılmadı",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "API Hız Limiti (Quota)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(
                                text = dash?.rateLimitInfo ?: "50/50 İstek",
                                color = PrimaryOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons: Quick Sync
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.triggerSync("ALL") },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_sync_all_action"),
                    enabled = !uiState.isSyncing
                ) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tam Senkron", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.testConnection() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_test_ping_action"),
                    enabled = !uiState.isTestingConnection
                ) {
                    Icon(imageVector = Icons.Outlined.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Canlı Ping", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // KPI Counters Grid
        item {
            Text(
                text = "Senkronizasyon Metrikleri",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "Trendyol Siparişleri",
                    value = "${dash?.totalOrdersSynced ?: 0}",
                    subtitle = "Toplam Kayıt",
                    icon = Icons.Outlined.LocalShipping,
                    color = PrimaryOrange,
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "Müşteri Soruları",
                    value = "${dash?.totalQuestionsSynced ?: 0}",
                    subtitle = "Bekleyen / Yanıt",
                    icon = Icons.Outlined.ContactSupport,
                    color = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Sync Details / Architecture Summary
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Entegrasyon Yetenekleri:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Basic Auth ve Satıcı Numarası (Supplier-ID) doğrulama\n" +
                               "• 50 istek/dakika dinamik Rate Limiting ve otomatik backoff\n" +
                               "• Toplu Fiyat ve Stok Güncelleme Motoru (/price-and-inventory)\n" +
                               "• Sipariş Çekme & Net Kâr Hesaplaması (Komisyon %20, Kargo, KDV)\n" +
                               "• Müşteri Soruları Çekme (AI Yanıt Motoruna Hazır)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrendyolOrdersContent(
    orders: List<TrendyolOrderSummaryDto>,
    onRefresh: () -> Unit
) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Henüz Senkronize Edilmiş Sipariş Yok",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Üst kısımdaki senkronizasyon ikonuna basarak Trendyol siparişlerini çekebilirsiniz.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trendyol Sipariş Listesi (${orders.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yenile", tint = PrimaryOrange)
                    }
                }
            }

            items(orders) { order ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.orderNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = PrimaryOrange
                            )
                            StatusBadge(status = order.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = order.customerName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = "${order.city} • ${order.itemCount} Ürün", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₺${order.netAmount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tahmini Kâr: ₺${order.estimatedProfit}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status.uppercase()) {
        "CREATED" -> Triple(InfoContainer, InfoBlue, "Yeni")
        "SHIPPED" -> Triple(WarningContainer, WarningAmber, "Kargoda")
        "DELIVERED" -> Triple(SuccessContainer, SuccessGreen, "Teslim Edildi")
        "CANCELLED" -> Triple(ErrorContainer, ErrorRed, "İptal")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TrendyolQuestionsContent(
    questions: List<TrendyolQuestionSummaryDto>,
    onRefresh: () -> Unit
) {
    if (questions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.QuestionAnswer,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Henüz Senkronize Edilmiş Müşteri Sorusu Yok",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trendyol Müşteri Soruları (${questions.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yenile", tint = PrimaryOrange)
                    }
                }
            }

            items(questions) { q ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q.customerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = WarningContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "CEVAP BEKLİYOR",
                                    color = WarningAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = q.productTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryOrange
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = q.questionText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendyolConfigContent(
    viewModel: TrendyolViewModel,
    uiState: TrendyolUiState
) {
    val config = uiState.config
    var supplierId by remember(config) { mutableStateOf(config?.supplierId ?: "") }
    var apiKey by remember(config) { mutableStateOf("") }
    var apiSecret by remember(config) { mutableStateOf("") }
    var mockMode by remember(config) { mutableStateOf(config?.mockMode ?: true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Trendyol Satıcı Entegrasyon Bilgileri",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Bu bilgiler yerel veritabanında güvenle saklanır ve API isteklerinde Basic Auth ile Trendyol'a iletilir.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = supplierId,
                onValueChange = { supplierId = it },
                label = { Text("Satıcı ID (Supplier ID)") },
                placeholder = { Text("Örn: 123456") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trendyol_supplier_id"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = BorderColor)
            )
        }

        item {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key") },
                placeholder = { Text(config?.apiKeyMasked ?: "Yeni API Key girin") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trendyol_api_key"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = BorderColor)
            )
        }

        item {
            OutlinedTextField(
                value = apiSecret,
                onValueChange = { apiSecret = it },
                label = { Text("API Secret") },
                placeholder = { Text(config?.apiSecretMasked ?: "Yeni API Secret girin") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_trendyol_api_secret"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = BorderColor)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Geliştirici & Simülasyon Modu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Gerçek Trendyol mağazası bağlanmadan veya test aşamasındayken simüle edilmiş sipariş ve soruları kullanır.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = mockMode,
                        onCheckedChange = { mockMode = it },
                        modifier = Modifier.testTag("switch_trendyol_mock_mode"),
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryOrange, checkedTrackColor = PrimaryOrangeLight)
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.saveConfig(
                        supplierId = supplierId,
                        apiKey = apiKey,
                        apiSecret = apiSecret,
                        mockMode = mockMode
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_trendyol_config"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                shape = RoundedCornerShape(10.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ayarları Kaydet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
