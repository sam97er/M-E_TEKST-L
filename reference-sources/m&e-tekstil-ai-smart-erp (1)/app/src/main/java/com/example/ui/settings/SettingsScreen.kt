package com.example.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AIProviderSlotDto
import com.example.data.model.TestConnectionResponseDto
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrl by remember { mutableStateOf(state.backendUrl) }
    var editingAiSlot by remember { mutableStateOf<AIProviderSlotDto?>(null) }
    var showTrendyolDialog by remember { mutableStateOf(false) }
    var showTelegramDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ayarlar & AI Sağlayıcılar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "3 Bağımsız AI Slotu ve Sistem Güvenliği",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadSettings() },
                        modifier = Modifier.testTag("refresh_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = PrimaryOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Backend Server & System Status
            item {
                BackendStatusCard(
                    isConnected = state.isBackendConnected,
                    backendUrl = state.backendUrl,
                    latencyMs = state.backendLatencyMs,
                    errorMessage = state.errorMessage,
                    onEditUrl = {
                        tempUrl = state.backendUrl
                        showUrlDialog = true
                    }
                )
            }

            // 2. AI Providers Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "3 Bağımsız AI Sağlayıcı (AI Provider Slots)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sistem tek bir AI modeline bağımlı değildir; 3 bağımsız slot desteklenir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. AI Providers Slot Cards
            items(state.aiProviders, key = { it.slot }) { slotDto ->
                val isTesting = state.testingSlots.contains(slotDto.slot)
                val testResult = state.testResults[slotDto.slot]

                AIProviderCard(
                    slot = slotDto,
                    isTesting = isTesting,
                    testResult = testResult,
                    onTestConnection = { viewModel.testProviderConnection(slotDto.slot) },
                    onEdit = { editingAiSlot = slotDto }
                )
            }

            // 4. Trendyol Integration Status Card
            item {
                IntegrationStatusCard(
                    title = "Trendyol Pazaryeri Entegrasyonu",
                    subtitle = "Supplier Partner API (Siparişler, Sorular, Stok Senkronizasyonu)",
                    icon = Icons.Default.Storefront,
                    isConfigured = state.generalSettings?.trendyolConfigured ?: false,
                    details = listOf(
                        "Tedarikçi ID (Supplier ID)" to (state.generalSettings?.trendyolSupplierId ?: "Yapılandırılmamış"),
                        "API Anahtarı" to if (state.generalSettings?.trendyolConfigured == true) "Aktif (Gizlenmiş)" else "Girilmemiş",
                        "Taban URL" to "https://api.trendyol.com/sapigw/suppliers/"
                    ),
                    onConfigure = { showTrendyolDialog = true }
                )
            }

            // 5. Telegram Notification Bot Card
            item {
                IntegrationStatusCard(
                    title = "Telegram Acil Bildirim Botu",
                    subtitle = "Kritik stok tükenmesi ve yeni müşteri soruları için anlık bildirim",
                    icon = Icons.Default.NotificationsActive,
                    isConfigured = state.generalSettings?.telegramConfigured ?: false,
                    details = listOf(
                        "Bot Durumu" to if (state.generalSettings?.telegramEnabled == true) "Etkinleştirildi" else "Pasif",
                        "Yetkilendirilmiş Chat ID" to if (state.generalSettings?.telegramConfigured == true) "Kayıtlı (Gizlenmiş)" else "Tanımlanmamış"
                    ),
                    onConfigure = { showTelegramDialog = true }
                )
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog to modify backend URL
    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = { Text("Backend URL Adresini Düzenle", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text(
                        "Termux üzerinde çalışıyorsa 'http://localhost:8000/' veya yerel IP'yi girin. Emülatör için 'http://10.0.2.2:8000/' kullanın.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("Backend API Base URL") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backend_url_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateBackendUrl(tempUrl)
                        showUrlDialog = false
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                ) {
                    Text("Kaydet & Yeniden Bağlan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // AI Slot Edit Dialog
    editingAiSlot?.let { slot ->
        var pName by remember { mutableStateOf(slot.providerName) }
        var mModel by remember { mutableStateOf(slot.model) }
        var apiKey by remember { mutableStateOf("") }
        var bUrl by remember { mutableStateOf(slot.baseUrl ?: "") }
        var enabled by remember { mutableStateOf(slot.isEnabled) }
        var timeout by remember { mutableStateOf(slot.timeoutSeconds.toString()) }

        AlertDialog(
            onDismissRequest = { editingAiSlot = null },
            title = { Text("AI Slot ${slot.slot} Yapılandırması", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("API anahtarınızı, modelinizi ve sağlayıcınızı buradan güncelleyebilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    OutlinedTextField(
                        value = pName,
                        onValueChange = { pName = it },
                        label = { Text("Sağlayıcı (gemini / openai / vb.)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = mModel,
                        onValueChange = { mModel = it },
                        label = { Text("Model (örn. gemini-2.5-flash)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Yeni API Anahtarı (Boş bırakılırsa değişmez)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bUrl,
                        onValueChange = { bUrl = it },
                        label = { Text("Base URL (Opsiyonel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = timeout,
                        onValueChange = { timeout = it },
                        label = { Text("Zaman Aşımı (Saniye)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Slot Aktif", fontWeight = FontWeight.Medium)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAiProvider(
                            slot = slot.slot,
                            providerName = pName,
                            model = mModel,
                            apiKey = apiKey.ifBlank { null },
                            baseUrl = bUrl.ifBlank { null },
                            isEnabled = enabled,
                            timeoutSeconds = timeout.toIntOrNull() ?: 30,
                            onComplete = { editingAiSlot = null }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAiSlot = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Trendyol Config Dialog
    if (showTrendyolDialog) {
        var supplierId by remember { mutableStateOf(state.generalSettings?.trendyolSupplierId ?: "") }
        var apiKey by remember { mutableStateOf("") }
        var apiSecret by remember { mutableStateOf("") }
        var mockMode by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTrendyolDialog = false },
            title = { Text("Trendyol API Bilgileri", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Trendyol Supplier ID ve API anahtarlarınızı girerek entegrasyonu aktif edin.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = supplierId,
                        onValueChange = { supplierId = it },
                        label = { Text("Tedarikçi ID (Supplier ID)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Trendyol API Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apiSecret,
                        onValueChange = { apiSecret = it },
                        label = { Text("Trendyol API Secret") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTrendyolSettings(supplierId, apiKey, apiSecret, mockMode) {
                            showTrendyolDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                ) {
                    Text("Kaydet ve Test Et")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTrendyolDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Telegram Config Dialog
    if (showTelegramDialog) {
        var botToken by remember { mutableStateOf("") }
        var chatId by remember { mutableStateOf("") }
        var enabled by remember { mutableStateOf(state.generalSettings?.telegramEnabled ?: true) }

        AlertDialog(
            onDismissRequest = { showTelegramDialog = false },
            title = { Text("Telegram Bot Ayarları", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Telegram Bot Token ve Chat ID girerek anlık sipariş ve soru bildirimleri alın.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Bot Token (örn. 123456:ABC-DEF...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        label = { Text("Chat ID (örn. -100123456)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bildirimleri Etkinleştir", fontWeight = FontWeight.Medium)
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTelegramSettings(botToken, chatId, enabled) {
                            showTelegramDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTelegramDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun BackendStatusCard(
    isConnected: Boolean,
    backendUrl: String,
    latencyMs: Double,
    errorMessage: String?,
    onEditUrl: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("backend_status_card")
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
                            .background(if (isConnected) SuccessGreen else ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Yerel Backend Bağlı (FastAPI + SQLite WAL)" else "Backend Bağlantısı Bekleniyor",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isConnected) SuccessGreen else ErrorRed
                    )
                }

                if (isConnected && latencyMs > 0) {
                    Surface(
                        color = SuccessContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${latencyMs} ms",
                            color = SuccessGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sunucu Adresi:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = backendUrl,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onEditUrl, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "URL Düzenle",
                        tint = PrimaryOrange
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = WarningContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = WarningAmber,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AIProviderCard(
    slot: AIProviderSlotDto,
    isTesting: Boolean,
    testResult: TestConnectionResponseDto?,
    onTestConnection: () -> Unit,
    onEdit: () -> Unit
) {
    val slotLabel = when (slot.slot) {
        1 -> "AI Slot 1 (Birincil / Ana Model)"
        2 -> "AI Slot 2 (Görsel Denetim & Yedek)"
        3 -> "AI Slot 3 (Yerel LLM / Bağımsız)"
        else -> "AI Slot ${slot.slot}"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_provider_card_${slot.slot}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Slot Name & Active Badge & Edit Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (slot.providerName.lowercase()) {
                            "gemini" -> Icons.Default.AutoAwesome
                            "openai" -> Icons.Default.Psychology
                            else -> Icons.Default.Memory
                        },
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slotLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (slot.isEnabled) SuccessContainer else WarningContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (slot.isEnabled) "AKTİF" else "DEVRE DIŞI",
                            color = if (slot.isEnabled) SuccessGreen else WarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Düzenle",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            // Details: Provider & Model
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Sağlayıcı / Altyapı", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = slot.providerName.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Aktif Model", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = slot.model,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Masked API Key (Never cleartext!)
            Column {
                Text(text = "API Anahtarı Durumu (Gizli)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = slot.maskedKey,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Assigned Tasks Chips
            Text(text = "Atanan Görevler", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                slot.assignedTasks.forEach { task ->
                    val taskName = when (task) {
                        "customer_reply" -> "Müşteri Yanıtları"
                        "daily_report" -> "Günlük Rapor"
                        "image_analysis" -> "Kumaş/Görsel Analizi"
                        "stock_audit" -> "Stok Denetimi"
                        "pricing_recommendation" -> "Fiyatlandırma"
                        else -> task
                    }
                    Surface(
                        color = PrimaryOrangeLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = taskName,
                            fontSize = 11.sp,
                            color = PrimaryOrangeDark,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Connection Button & Live Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTestConnection,
                    enabled = !isTesting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White),
                    modifier = Modifier.testTag("test_connection_slot_${slot.slot}")
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Ediliyor...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bağlantıyı Test Et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (slot.timeoutSeconds > 0) {
                    Text(
                        text = "Zaman Aşımı: ${slot.timeoutSeconds}s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Test Result Feedback Banner
            AnimatedVisibility(visible = testResult != null) {
                testResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = if (res.success) SuccessContainer else ErrorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (res.success) "Bağlantı Başarılı!" else "Bağlantı Başarısız",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (res.success) SuccessGreen else ErrorRed
                                )
                                if (res.latencyMs > 0) {
                                    Text(
                                        text = "${res.latencyMs} ms",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (res.success) SuccessGreen else ErrorRed
                                    )
                                }
                            }

                            if (!res.reply.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Model Yanıtı: \"${res.reply}\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (!res.error.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = res.error,
                                    fontSize = 11.sp,
                                    color = ErrorRed
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
fun IntegrationStatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isConfigured: Boolean,
    details: List<Pair<String, String>>,
    onConfigure: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    color = if (isConfigured) SuccessContainer else WarningContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isConfigured) "HAZIR" else "YAPILANDIRILMAMIŞ",
                        color = if (isConfigured) SuccessGreen else WarningAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(10.dp))
            details.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfigure,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrangeLight, contentColor = PrimaryOrangeDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Yapılandır / Bilgileri Güncelle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


