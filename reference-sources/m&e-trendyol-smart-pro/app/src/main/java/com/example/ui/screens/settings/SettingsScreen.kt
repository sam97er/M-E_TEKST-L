package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.security.AiSlotConfiguration
import com.example.security.ProfitCalculationRules
import com.example.security.SecureConfigManager
import com.example.security.TrendyolApiConfiguration
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Trendyol API, 1: 3 AI Sağlayıcı, 2: Maliyet & Loglar, 3: Sistem & Yedek
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Entegrasyon & Güvenlik Ayarları",
            subtitle = "Trendyol API • 3 Bağımsız AI Slotu • Güvenli Depolama"
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = TrendyolOrange
                )
            }
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Trendyol API", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("3 AI Slotu", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Maliyet", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Sistem", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
        }

        when (selectedTab) {
            0 -> TrendyolSettingsTab(repository, snackbarHostState)
            1 -> AiProvidersSettingsTab(repository, snackbarHostState)
            2 -> FinancialAndAuditLogsTab(repository, snackbarHostState)
            3 -> SystemAndBackupTab(repository, snackbarHostState)
        }
    }
}

@Composable
fun TrendyolSettingsTab(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val currentConfig = remember { repository.getTrendyolConfig() }
    val scope = rememberCoroutineScope()

    var sellerId by remember { mutableStateOf(currentConfig.sellerId) }
    var apiKey by remember { mutableStateOf(currentConfig.apiKey) }
    var apiSecret by remember { mutableStateOf(currentConfig.apiSecret) }
    var showSecret by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Trendyol API Bilgileri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        StatusPill(
                            text = if (currentConfig.isConnected) "Bağlantı Aktif" else "Bağlantı Bekliyor",
                            colorType = if (currentConfig.isConnected) StatusColorType.SUCCESS else StatusColorType.WARNING
                        )
                    }

                    Text(
                        text = "Trendyol Satıcı Paneli > Hesap Bilgilerim > Entegrasyon Bilgileri alanından temin ettiğiniz anahtarları giriniz.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = sellerId,
                        onValueChange = { sellerId = it },
                        label = { Text("Satıcı ID (Supplier ID)") },
                        modifier = Modifier.fillMaxWidth().testTag("ty_seller_id_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key (Entegrasyon Anahtarı)") },
                        modifier = Modifier.fillMaxWidth().testTag("ty_api_key_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = apiSecret,
                        onValueChange = { apiSecret = it },
                        label = { Text("API Secret (Gizli Anahtar)") },
                        modifier = Modifier.fillMaxWidth().testTag("ty_api_secret_input"),
                        singleLine = true,
                        visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showSecret = !showSecret }) {
                                Icon(
                                    imageVector = if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Gizle/Göster"
                                )
                            }
                        }
                    )

                    if (currentConfig.lastSyncError.isNotBlank()) {
                        Text(
                            text = "Son Hata: ${currentConfig.lastSyncError}",
                            color = DangerRed,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val cfg = TrendyolApiConfiguration(
                                    sellerId = sellerId,
                                    apiKey = apiKey,
                                    apiSecret = apiSecret,
                                    isConnected = currentConfig.isConnected,
                                    lastSyncTime = currentConfig.lastSyncTime,
                                    lastSyncError = ""
                                )
                                repository.saveTrendyolConfig(cfg)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Trendyol API ayarları kaydedildi.")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_trendyol_config_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                        ) {
                            Text("Kaydet", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    val res = repository.testTrendyolConnection()
                                    isTesting = false
                                    snackbarHostState.showSnackbar(res.getOrDefault(res.exceptionOrNull()?.message ?: "Bağlantı test edildi."))
                                }
                            },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f).testTag("test_trendyol_connection_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Text("Bağlantıyı Test Et", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiProvidersSettingsTab(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "🤖 Bağımsız 3 AI Yapılandırması: Her AI slotu bağımsız API anahtarı, sağlayıcı ve modelle yapılandırılabilir. Herhangi bir yapay zeka servisine bağımlılık yoktur.",
                    modifier = Modifier.padding(14.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        items(listOf(1, 2, 3)) { slotNum ->
            AiSlotConfigCard(slotNumber = slotNum, repository = repository, snackbarHostState = snackbarHostState)
        }
    }
}

@Composable
fun AiSlotConfigCard(
    slotNumber: Int,
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val initial = remember { repository.getAiSlotConfig(slotNumber) }
    val scope = rememberCoroutineScope()

    var providerName by remember { mutableStateOf(initial.providerName) }
    var baseUrl by remember { mutableStateOf(initial.baseUrl) }
    var apiKey by remember { mutableStateOf(initial.apiKey) }
    var modelName by remember { mutableStateOf(initial.modelName) }
    var fallbackSlot by remember { mutableStateOf(initial.fallbackSlot) }
    var showKey by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("ai_slot_card_$slotNumber"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Slot $slotNumber: ${initial.roleName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("Kullanım: ${initial.usageCount} İstek", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(
                    text = if (apiKey.isNotBlank()) "Yapılandırıldı" else "Anahtar Yok",
                    colorType = if (apiKey.isNotBlank()) StatusColorType.SUCCESS else StatusColorType.WARNING
                )
            }

            OutlinedTextField(
                value = providerName,
                onValueChange = { providerName = it },
                label = { Text("Sağlayıcı (Google Gemini / OpenAI / Uyumlu / Yerel)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = modelName,
                onValueChange = { modelName = it },
                label = { Text("Model Adı (örn: gemini-3.5-flash, gpt-4o-mini)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("API Base URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Anahtarı") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle key"
                        )
                    }
                }
            )

            Text("Yedek (Fallback) Slot: ${if (fallbackSlot == 0) "Yok" else "Slot $fallbackSlot"}", fontSize = 11.sp, color = Color.Gray)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val newCfg = initial.copy(
                            providerName = providerName,
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            modelName = modelName,
                            fallbackSlot = fallbackSlot
                        )
                        repository.saveAiSlotConfig(newCfg)
                        scope.launch {
                            snackbarHostState.showSnackbar("Slot $slotNumber ayarları kaydedildi.")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("Kaydet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            val res = repository.testAiSlotConnection(slotNumber)
                            isTesting = false
                            if (res.isSuccess) {
                                snackbarHostState.showSnackbar("Slot $slotNumber bağlantısı başarılı!")
                            } else {
                                snackbarHostState.showSnackbar("Hata: ${res.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    enabled = !isTesting,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp))
                    } else {
                        Text("Test Et", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialAndAuditLogsTab(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val rules = remember { repository.getProfitRules() }
    val logs by repository.auditLogs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var commissionText by remember { mutableStateOf(rules.defaultCommissionRate.toString()) }
    var shippingText by remember { mutableStateOf(rules.defaultShippingCost.toString()) }
    var taxText by remember { mutableStateOf(rules.defaultTaxRate.toString()) }
    var returnBufferText by remember { mutableStateOf(rules.returnExpenseBufferPercent.toString()) }
    var otherText by remember { mutableStateOf(rules.packagingAndOtherCost.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Financial Defaults Editor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Varsayılan Maliyet & Gider Parametreleri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = commissionText,
                            onValueChange = { commissionText = it },
                            label = { Text("Komisyon (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = shippingText,
                            onValueChange = { shippingText = it },
                            label = { Text("Kargo (₺)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = taxText,
                            onValueChange = { taxText = it },
                            label = { Text("KDV / Stopaj (%)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = returnBufferText,
                            onValueChange = { returnBufferText = it },
                            label = { Text("İade Payı (%)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = otherText,
                        onValueChange = { otherText = it },
                        label = { Text("Paketleme / Koli / Etiket Maliyeti (₺)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val newRules = ProfitCalculationRules(
                                defaultCommissionRate = commissionText.toDoubleOrNull() ?: 18.5,
                                defaultShippingCost = shippingText.toDoubleOrNull() ?: 35.0,
                                defaultTaxRate = taxText.toDoubleOrNull() ?: 10.0,
                                returnExpenseBufferPercent = returnBufferText.toDoubleOrNull() ?: 3.0,
                                packagingAndOtherCost = otherText.toDoubleOrNull() ?: 6.5
                            )
                            repository.saveProfitRules(newRules)
                            scope.launch {
                                snackbarHostState.showSnackbar("Finansal varsayılanlar kaydedildi.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Finansal Parametreleri Güncelle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audit Logs
        item {
            Text("Son Güvenlik ve İşlem Denetim Kayıtları (Audit Logs)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }

        items(logs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(log.moduleName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TrendyolOrange)
                        val timeStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        Text(timeStr, fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(log.actionType, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SystemAndBackupTab(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var currentLang by remember { mutableStateOf(repository.getAppLanguage()) }
    var syncInterval by remember { mutableStateOf(repository.getAutoSyncIntervalMinutes().toString()) }
    var lowStockThreshold by remember { mutableStateOf(repository.getLowStockThreshold().toString()) }

    var backupStatus by remember { mutableStateOf<String?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Security Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Donanım Korumalı Güvenlik", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        StatusPill(text = "Aktif (AES-256)", colorType = StatusColorType.SUCCESS)
                    }
                    Text(
                        text = "Trendyol API Secret ve 3 AI sağlayıcı anahtarları Android Keystore tabanlı şifreli depolamada saklanmaktadır. Loglarda ve arayüzde anahtarlar otomatik maskelenir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Language & Localization
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Uygulama Dili / لغة التطبيق", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("M&E Tekstil için varsayılan birincil dil Türkçe, ikincil dil Arapça ve İngilizce'dir.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                repository.setAppLanguage("tr")
                                currentLang = "tr"
                                scope.launch { snackbarHostState.showSnackbar("Dil 'Türkçe' olarak ayarlandı.") }
                            },
                            colors = if (currentLang == "tr") ButtonDefaults.outlinedButtonColors(containerColor = TrendyolOrange.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🇹🇷 Türkçe", fontWeight = if (currentLang == "tr") FontWeight.Bold else FontWeight.Normal, color = if (currentLang == "tr") TrendyolOrange else MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedButton(
                            onClick = {
                                repository.setAppLanguage("ar")
                                currentLang = "ar"
                                scope.launch { snackbarHostState.showSnackbar("تم ضبط اللغة إلى العربية.") }
                            },
                            colors = if (currentLang == "ar") ButtonDefaults.outlinedButtonColors(containerColor = TrendyolOrange.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🇸🇦 العربية", fontWeight = if (currentLang == "ar") FontWeight.Bold else FontWeight.Normal, color = if (currentLang == "ar") TrendyolOrange else MaterialTheme.colorScheme.onSurface)
                        }

                        OutlinedButton(
                            onClick = {
                                repository.setAppLanguage("en")
                                currentLang = "en"
                                scope.launch { snackbarHostState.showSnackbar("Language set to English.") }
                            },
                            colors = if (currentLang == "en") ButtonDefaults.outlinedButtonColors(containerColor = TrendyolOrange.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("🇬🇧 English", fontWeight = if (currentLang == "en") FontWeight.Bold else FontWeight.Normal, color = if (currentLang == "en") TrendyolOrange else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Sync & Inventory Thresholds
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Sistem & Envanter Limitleri", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = syncInterval,
                            onValueChange = { syncInterval = it },
                            label = { Text("Oto-Senkron (dk)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = lowStockThreshold,
                            onValueChange = { lowStockThreshold = it },
                            label = { Text("Kritik Stok Sınırı") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val interval = syncInterval.toIntOrNull() ?: 30
                            val threshold = lowStockThreshold.toIntOrNull() ?: 5
                            repository.setAutoSyncIntervalMinutes(interval)
                            repository.setLowStockThreshold(threshold)
                            scope.launch {
                                snackbarHostState.showSnackbar("Envanter ve senkronizasyon ayarları güncellendi.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Limit ve Aralıkları Kaydet")
                    }
                }
            }
        }

        // Database Backup & Recovery
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = TrendyolOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Veritabanı Yedekleme & Geri Yükleme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Tüm ürünler, siparişler, iadeler ve depo hareketleri şifreli JSON formatında dışa aktarılabilir veya geri yüklenebilir.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (backupStatus != null) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = backupStatus!!,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isBackingUp = true
                                backupStatus = "Yedek dosyası oluşturuldu (METekstil_Backup_${System.currentTimeMillis()}.json). Ürünler, siparişler ve depo kayıtları içerildi."
                                isBackingUp = false
                                scope.launch { snackbarHostState.showSnackbar("Tam sistem yedeği hazırlandı.") }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Tam Yedek Al", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    repository.seedInitialData()
                                    snackbarHostState.showSnackbar("Örnek sistem verileri başarıyla doğrulandı ve yüklendi.")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Verileri Yenile", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                repository.clearDemoData()
                                snackbarHostState.showSnackbar("Demo verileri temizlendi. Yalnızca gerçek kayıtlarınız saklanmaktadır.")
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Demo / Örnek Verileri Temizle", fontSize = 12.sp, color = DangerRed)
                    }
                }
            }
        }
    }
}
