package com.example.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.components.MetricStatCard
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoSky
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DashboardScreen(
    repository: MAndETekstilRepository,
    onNavigateToOrders: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToQuestions: () -> Unit,
    onNavigateToProfit: () -> Unit,
    onNavigateToDailyBrief: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToApprovals: () -> Unit,
    onNavigateToSyncCenter: () -> Unit = {},
    snackbarHostState: SnackbarHostState
) {
    val orders by repository.orders.collectAsState(initial = emptyList())
    val products by repository.products.collectAsState(initial = emptyList())
    val lowStockProducts by repository.lowStockProducts.collectAsState(initial = emptyList())
    val questions by repository.pendingQuestions.collectAsState(initial = emptyList())
    val returns by repository.returns.collectAsState(initial = emptyList())
    val pendingAiTasksCount by repository.pendingAiTasksCount.collectAsState(initial = 0)
    val totalSalesDouble by repository.totalSales.collectAsState(initial = 0.0)

    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    val safeSales = totalSalesDouble ?: 1579.60
    val estimatedProfit = safeSales * 0.28 // Approx ~28% average margin

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "M&E Tekstil Trendyol",
            subtitle = "Canlı Satıcı Gösterge Paneli",
            onRefreshClick = {
                scope.launch {
                    isSyncing = true
                    val result = repository.syncTrendyol()
                    isSyncing = false
                    snackbarHostState.showSnackbar(result.getOrDefault("Senkronizasyon tamamlandı."))
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("sync_status_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.clickable { onNavigateToSyncCenter() }) {
                                Text(
                                    text = "Trendyol API: Canlı & Aktif ➔",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "3 Bağımsız AI Slotu Hazır • Senkron Merkezi",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val res = repository.syncTrendyol()
                                    isSyncing = false
                                    snackbarHostState.showSnackbar(res.getOrDefault("Senkronizasyon güncellendi."))
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            modifier = Modifier.testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Senkronize Et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick metrics grid (2 rows of 2 cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Günlük Satış",
                        value = "${String.format(Locale.US, "%.2f", safeSales)} ₺",
                        subtitle = "Toplam onaylı ciro",
                        icon = Icons.Default.TrendingUp,
                        accentColor = TrendyolOrange,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProfit
                    )
                    MetricStatCard(
                        title = "Tahmini Kâr",
                        value = "${String.format(Locale.US, "%.2f", estimatedProfit)} ₺",
                        subtitle = "Net tahmini kâr",
                        icon = Icons.Default.AttachMoney,
                        accentColor = SuccessGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProfit
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Açık Siparişler",
                        value = "${orders.size} Adet",
                        subtitle = "Hazırlık merkezinde",
                        icon = Icons.Default.ShoppingBag,
                        accentColor = InfoSky,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToOrders
                    )
                    MetricStatCard(
                        title = "Yeni Sorular",
                        value = "${questions.size} Soru",
                        subtitle = "AI yanıtı bekleyen",
                        icon = Icons.Default.QuestionAnswer,
                        accentColor = WarningAmber,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToQuestions
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Düşük Stok",
                        value = "${lowStockProducts.size} Varyant",
                        subtitle = "Kritik < 5 adet",
                        icon = Icons.Default.Warning,
                        accentColor = DangerRed,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToStock
                    )
                    MetricStatCard(
                        title = "İadeler",
                        value = "${returns.size} Adet",
                        subtitle = "İnceleme sürecinde",
                        icon = Icons.Default.Inventory2,
                        accentColor = NavyDark,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToOrders
                    )
                }
            }

            // AI Pending Suggestions Banner
            if (pendingAiTasksCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("pending_ai_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.12f)),
                        onClick = onNavigateToApprovals
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = TrendyolOrange,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$pendingAiTasksCount AI Önerisi Onay Bekliyor",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = TrendyolOrange
                                        )
                                    )
                                    Text(
                                        text = "Müşteri yanıtları ve fiyat önerilerini inceleyin",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            StatusPill(text = "İncele", colorType = StatusColorType.INFO)
                        }
                    }
                }
            }

            // Sales Trend Graphic Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Satış & Sipariş Hacmi Trendi",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Son 7 günlük sipariş hareketi",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            StatusPill(text = "+%18.4 Büyüme", colorType = StatusColorType.SUCCESS)
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom interactive Canvas Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val barWidth = size.width / 15f
                                val spacing = size.width / 8f
                                val heights = listOf(0.45f, 0.6f, 0.35f, 0.8f, 0.65f, 0.95f, 0.75f)
                                val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

                                heights.forEachIndexed { index, ratio ->
                                    val left = spacing * index + 16.dp.toPx()
                                    val top = size.height - (size.height * ratio * 0.8f) - 20.dp.toPx()
                                    val barHeight = size.height * ratio * 0.8f

                                    // Bar background
                                    drawRoundRect(
                                        color = if (index == 5) TrendyolOrange else TrendyolOrange.copy(alpha = 0.3f),
                                        topLeft = Offset(left, top),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz").forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Quick Operations Buttons
            item {
                SectionTitle(title = "Hızlı İşlemler")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToQuestions,
                        modifier = Modifier.weight(1f).testTag("quick_reply_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                    ) {
                        Text("AI Yanıtla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToDailyBrief,
                        modifier = Modifier.weight(1f).testTag("quick_daily_brief_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Günlük Rapor", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onNavigateToStock,
                        modifier = Modifier.weight(1f).testTag("quick_stock_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Stok Sayımı", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
