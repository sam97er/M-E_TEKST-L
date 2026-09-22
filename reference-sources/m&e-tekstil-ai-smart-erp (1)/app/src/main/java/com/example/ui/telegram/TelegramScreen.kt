package com.example.ui.telegram

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLogItemDto
import com.example.data.model.TelegramEventItemDto
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramScreen(
    viewModel: TelegramViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Telegram Bildirimleri",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Telegram & Bildirimler",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.config.isConfigured && uiState.config.enabled) "Bot Aktif & Bağlı" else "Bot Yapılandırılmadı / Pasif",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.config.isConfigured && uiState.config.enabled) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            when (uiState.selectedTab) {
                                0 -> viewModel.loadConfig()
                                1 -> viewModel.loadEvents()
                                2 -> viewModel.loadAuditLogs()
                            }
                        },
                        modifier = Modifier.testTag("refresh_telegram_button")
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
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Success / Error Alerts
            uiState.successMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SuccessContainer),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, color = SuccessGreen, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            uiState.errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorContainer),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, color = ErrorRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearMessages() }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = ErrorRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Top Tab Navigation
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                        color = PrimaryOrange
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bot Ayarları", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Kuyruk & Olaylar", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                            if (uiState.eventsResponse.pendingCount > 0 || uiState.eventsResponse.failedCount > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = if (uiState.eventsResponse.failedCount > 0) ErrorRed else PrimaryOrange) {
                                    Text("${uiState.eventsResponse.pendingCount + uiState.eventsResponse.failedCount}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Denetim İzi", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Main Content Area
            when (uiState.selectedTab) {
                0 -> BotConfigTab(viewModel = viewModel, uiState = uiState)
                1 -> NotificationQueueTab(viewModel = viewModel, uiState = uiState)
                2 -> AuditTrailTab(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

@Composable
fun BotConfigTab(
    viewModel: TelegramViewModel,
    uiState: TelegramUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Summary Card
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
                        Column {
                            Text(
                                text = "Telegram Bildirim Durumu",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isEnabledToggle) "Otomatik bildirimler açık" else "Bildirim gönderimi duraklatıldı",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.isEnabledToggle) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isEnabledToggle,
                            onCheckedChange = { viewModel.toggleEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryOrange,
                                checkedTrackColor = PrimaryOrangeLight,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = BorderColor
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Aktif Bot Token", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = uiState.config.botTokenMasked, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Hedef Chat / Grup ID", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = uiState.config.chatId ?: "Tanımsız", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Credentials Input Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bot Bağlantı Bilgilerini Güncelle",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Telegram @BotFather üzerinden oluşturduğunuz bot tokeni ve bildirim alacak chat/kanal kimliğini giriniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = uiState.inputBotToken,
                        onValueChange = { viewModel.onBotTokenChanged(it) },
                        label = { Text("Yeni Bot Token (örn: 123456:ABC-DEF...)") },
                        placeholder = { Text("Değiştirmek istemiyorsanız boş bırakın") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_bot_token_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = PrimaryOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.inputChatId,
                        onValueChange = { viewModel.onChatIdChanged(it) },
                        label = { Text("Hedef Chat / Kanal ID (örn: -100123456789)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_chat_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = PrimaryOrange
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.saveConfig() },
                        enabled = !uiState.isSavingConfig,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_telegram_config_button")
                    ) {
                        if (uiState.isSavingConfig) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Ayarları Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Notification Categories Toggles
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bildirim Kategorileri",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hangi işletme olaylarında anlık Telegram mesajı gönderileceğini seçin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )

                    NotificationToggleRow(
                        title = "Yeni Sipariş Bildirimi",
                        desc = "Trendyol'dan sipariş düştüğünde kâr ve kalem bilgisiyle bildir",
                        icon = Icons.Default.ShoppingCart,
                        checked = uiState.notifyNewOrder,
                        onCheckedChange = { viewModel.toggleCategory("NEW_ORDER", it) }
                    )

                    NotificationToggleRow(
                        title = "Kritik Stok Uyarısı",
                        desc = "Bir ürünün stoğu 5 adedin altına indiğinde uyarı gönder",
                        icon = Icons.Default.Warning,
                        checked = uiState.notifyLowStock,
                        onCheckedChange = { viewModel.toggleCategory("LOW_STOCK", it) }
                    )

                    NotificationToggleRow(
                        title = "Yeni Müşteri Sorusu",
                        desc = "Trendyol müşterisi soru sorduğunda ve AI taslağı hazırlandığında bildir",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        checked = uiState.notifyNewQuestion,
                        onCheckedChange = { viewModel.toggleCategory("NEW_QUESTION", it) }
                    )

                    NotificationToggleRow(
                        title = "Günlük Satış & Net Kâr Özeti",
                        desc = "Her günün ciro, sipariş ve tahmini kâr tablosunu raporla",
                        icon = Icons.Default.Assessment,
                        checked = uiState.notifyDailyDigest,
                        onCheckedChange = { viewModel.toggleCategory("DAILY_DIGEST", it) }
                    )

                    NotificationToggleRow(
                        title = "Sistem & Entegrasyon Hataları",
                        desc = "Trendyol API ve kota aşımlarında yöneticiyi bilgilendir",
                        icon = Icons.Default.ErrorOutline,
                        checked = uiState.notifySystemError,
                        onCheckedChange = { viewModel.toggleCategory("SYSTEM_ERROR", it) }
                    )
                }
            }
        }

        // Test Message & Instant Digest Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hızlı Eylemler & Test",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.testMessageInput,
                        onValueChange = { viewModel.onTestMessageChanged(it) },
                        label = { Text("Özel Test Mesajı") },
                        placeholder = { Text("Varsayılan test mesajı için boş bırakabilirsiniz") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryOrange,
                            unfocusedBorderColor = BorderColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.sendTestMessage() },
                            enabled = !uiState.isSendingTest,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
                        ) {
                            if (uiState.isSendingTest) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryOrange, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Test Bildirimi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.sendDailyDigest() },
                            enabled = !uiState.isSendingDigest,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White)
                        ) {
                            if (uiState.isSendingDigest) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Günlük Özeti İlet", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryOrangeLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryOrangeDark, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryOrange,
                checkedTrackColor = PrimaryOrangeLight,
                uncheckedTrackColor = BorderColor
            )
        )
    }
}

@Composable
fun NotificationQueueTab(
    viewModel: TelegramViewModel,
    uiState: TelegramUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Metric Counters Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QueueMetricCard(
                title = "Toplam",
                value = "${uiState.eventsResponse.total}",
                icon = Icons.Default.ListAlt,
                color = PrimaryOrange,
                modifier = Modifier.weight(1f)
            )
            QueueMetricCard(
                title = "Gönderildi",
                value = "${uiState.eventsResponse.sentCount}",
                icon = Icons.Default.CheckCircle,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            QueueMetricCard(
                title = "Bekliyor",
                value = "${uiState.eventsResponse.pendingCount}",
                icon = Icons.Default.HourglassTop,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            QueueMetricCard(
                title = "Hata",
                value = "${uiState.eventsResponse.failedCount}",
                icon = Icons.Default.Error,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Retry and Clear Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { viewModel.retryFailedEvents() },
                enabled = !uiState.isRetrying && (uiState.eventsResponse.failedCount > 0 || uiState.eventsResponse.pendingCount > 0),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White),
                modifier = Modifier.testTag("retry_telegram_events_button")
            ) {
                if (uiState.isRetrying) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Hatalıları Yeniden Dene", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            TextButton(
                onClick = { viewModel.clearDeliveredEvents() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("İletilenleri Temizle", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "ALL" to "Tümü",
                "PENDING" to "Bekleyenler",
                "FAILED" to "Hatalılar",
                "SENT" to "Gönderilenler",
                "SKIPPED" to "Atlananlar"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = uiState.statusFilter == key,
                    onClick = { viewModel.setStatusFilter(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrangeLight,
                        selectedLabelColor = PrimaryOrangeDark,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.statusFilter == key) PrimaryOrange else BorderColor,
                        enabled = true,
                        selected = uiState.statusFilter == key
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Events List
        if (uiState.eventsResponse.events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.NotificationsNone, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Bu filtreye uygun bildirim olayı bulunmuyor.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.eventsResponse.events) { event ->
                    TelegramEventCard(event = event)
                }
            }
        }
    }
}

@Composable
fun QueueMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontSize = 10.sp)
        }
    }
}

@Composable
fun TelegramEventCard(event: TelegramEventItemDto) {
    val (statusColor, statusBg, statusIcon, statusLabel) = when (event.status.uppercase()) {
        "SENT" -> Quadruple(SuccessGreen, SuccessContainer, Icons.Default.CheckCircle, "İletildi")
        "FAILED" -> Quadruple(ErrorRed, ErrorContainer, Icons.Default.Error, "Hata")
        "PENDING" -> Quadruple(WarningAmber, WarningContainer, Icons.Default.HourglassTop, "Bekliyor")
        else -> Quadruple(MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, Icons.Default.Block, "Atlandı")
    }

    val eventTypeLabel = when (event.eventType) {
        "NEW_ORDER" -> "Yeni Sipariş"
        "LOW_STOCK" -> "Kritik Stok"
        "NEW_QUESTION" -> "Müşteri Sorusu"
        "DAILY_DIGEST" -> "Günlük Özet"
        "SYSTEM_ERROR" -> "Sistem Hatası"
        else -> "Test Bildirimi"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = eventTypeLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = " • #${event.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = event.message.replace(Regex("<[^>]*>"), ""), // strip basic HTML tags for clean display
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (!event.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Hata: ${event.errorMessage}", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Oluşturulma: ${event.createdAt.take(19).replace("T", " ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                if (event.retryCount > 0) {
                    Text(
                        text = "Deneme: ${event.retryCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AuditTrailTab(
    viewModel: TelegramViewModel,
    uiState: TelegramUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Değişmez Sistem Denetim İzi (Audit Log)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Stok hareketleri, AI onayları, Trendyol senkronizasyonları ve Telegram bildirimlerinin tam işlem günlüğü.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val auditFilters = listOf(
                "ALL" to "Tüm Kayıtlar",
                "TELEGRAM_SENT" to "Telegram Gönderim",
                "INVENTORY_CHANGE" to "Stok / Varyant",
                "AI_APPROVAL" to "AI Onayları",
                "ORDER_SYNC" to "Sipariş Senkron"
            )
            items(auditFilters) { (key, label) ->
                FilterChip(
                    selected = uiState.auditFilter == key,
                    onClick = { viewModel.setAuditFilter(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrangeLight,
                        selectedLabelColor = PrimaryOrangeDark,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.auditFilter == key) PrimaryOrange else BorderColor,
                        enabled = true,
                        selected = uiState.auditFilter == key
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.auditResponse.logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Seçilen filtre için denetim kaydı bulunamadı.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.auditResponse.logs) { log ->
                    AuditLogCard(log = log)
                }
            }
        }
    }
}

@Composable
fun AuditLogCard(log: AuditLogItemDto) {
    val actorColor = when (log.actor.uppercase()) {
        "SYSTEM", "TELEGRAM_SERVICE" -> PrimaryOrange
        "USER", "ADMIN" -> SuccessGreen
        "AI_ASSISTANT" -> InfoBlue
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.eventType,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "(${log.action})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    color = actorColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = log.actor,
                        color = actorColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Varlık: ${log.entityName} ${log.entityId?.let { "• ID: $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!log.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = log.details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = log.createdAt.take(19).replace("T", " "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
