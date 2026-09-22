package com.example.ui.screens.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.WarningAmber

@Composable
fun ToolsHubScreen(
    repository: MAndETekstilRepository,
    onNavigateToProfit: () -> Unit,
    onNavigateToGrowth: () -> Unit,
    onNavigateToApprovals: () -> Unit,
    onNavigateToDailyBrief: () -> Unit,
    onNavigateToReturns: () -> Unit,
    onNavigateToAutomations: () -> Unit,
    onNavigateToTelegram: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSyncCenter: () -> Unit = {}
) {
    val pendingAiTasksCount by repository.pendingAiTasksCount.collectAsState(initial = 0)

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Tüm Modüller & Araçlar",
            subtitle = "M&E Tekstil Özel Yönetim Süiti"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HubToolItem(
                    title = "Trendyol Canlı Senkronizasyon Merkezi",
                    subtitle = "Canlı API bağlantısı, pagination, veri sağlığı ve senkronizasyon günlükleri",
                    icon = Icons.Default.CloudSync,
                    accentColor = TrendyolOrange,
                    tag = "hub_sync_center",
                    onClick = onNavigateToSyncCenter
                )
            }
            item {
                HubToolItem(
                    title = "Akıllı Kâr & Maliyet Hesaplayıcı",
                    subtitle = "Komisyon, kargo, stopaj ve net kâr marjı simülatörü",
                    icon = Icons.Default.Calculate,
                    accentColor = SuccessGreen,
                    tag = "hub_profit_calculator",
                    onClick = onNavigateToProfit
                )
            }
            item {
                HubToolItem(
                    title = "AI Ürün Büyüme & Kalite (Slot 3)",
                    subtitle = "SEO başlık, açıklama üretimi, kalite ve fiyat senaryoları",
                    icon = Icons.Default.AutoAwesome,
                    accentColor = TrendyolOrange,
                    tag = "hub_growth",
                    onClick = onNavigateToGrowth
                )
            }
            item {
                HubToolItem(
                    title = "AI Onay & İnceleme Merkezi",
                    subtitle = "İnsan denetiminden geçmeyi bekleyen AI aksiyonları",
                    icon = Icons.Default.DoneAll,
                    accentColor = WarningAmber,
                    badgeText = if (pendingAiTasksCount > 0) "$pendingAiTasksCount Bekliyor" else null,
                    tag = "hub_approvals",
                    onClick = onNavigateToApprovals
                )
            }
            item {
                HubToolItem(
                    title = "Günlük İş & Kâr Özeti (Slot 2)",
                    subtitle = "Yöneticiye özel günlük ciro, kâr ve öncelikli aksiyonlar",
                    icon = Icons.Default.Insights,
                    accentColor = InfoSky,
                    tag = "hub_daily_brief",
                    onClick = onNavigateToDailyBrief
                )
            }
            item {
                HubToolItem(
                    title = "İade & Yorum Analizi AI",
                    subtitle = "Tekrarlanan iade sebepleri ve beden kalıp iyileştirmeleri",
                    icon = Icons.Default.Replay,
                    accentColor = Color(0xFF8B5CF6),
                    tag = "hub_returns",
                    onClick = onNavigateToReturns
                )
            }
            item {
                HubToolItem(
                    title = "Otomasyon Yöneticisi",
                    subtitle = "Tetikleyici, koşul ve eylem bazlı akıllı kurallar",
                    icon = Icons.Default.SettingsSuggest,
                    accentColor = TrendyolOrange,
                    tag = "hub_automations",
                    onClick = onNavigateToAutomations
                )
            }
            item {
                HubToolItem(
                    title = "Telegram Bildirim Merkezi",
                    subtitle = "Sipariş, soru, kritik stok ve hata bot uyarıları",
                    icon = Icons.Default.Send,
                    accentColor = Color(0xFF0284C7),
                    tag = "hub_telegram",
                    onClick = onNavigateToTelegram
                )
            }
            item {
                HubToolItem(
                    title = "Trendyol API & 3 AI Sağlayıcı Ayarları",
                    subtitle = "Satıcı anahtarları, 3 AI slotu yapılandırması ve denetim logları",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF475569),
                    tag = "hub_settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

@Composable
fun HubToolItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String? = null,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (badgeText != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusPill(text = badgeText, colorType = StatusColorType.WARNING)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Git", tint = Color.Gray)
        }
    }
}
