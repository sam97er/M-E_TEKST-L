package com.example.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandGoldLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoSky
import com.example.ui.theme.Midnight
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarmBackground
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
    val totalSales by repository.totalSales.collectAsState(initial = 0.0)
    val scope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }

    val sales = totalSales ?: orders.sumOf { it.totalPrice }
    val profit = orders.sumOf { it.netProfit }
    val activeOrders = orders.count { it.status.name != "DELIVERED" && it.status.name != "CANCELLED" }
    val criticalStock = lowStockProducts.count { it.stockQuantity <= 2 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        // New brand header — deliberately different from the old orange dashboard.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Midnight)
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "M&E TEKSTİL",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            "Aİ Smart Pro • İşletme Kontrol Merkezi",
                            color = BrandGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.NotificationsNone,
                            contentDescription = "Bildirimler",
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = .08f))
                                .padding(9.dp)
                        )
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = Color.White,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = .08f))
                                .clickable(onClick = onNavigateToSettings)
                                .padding(9.dp)
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sync_status_card")
                        .clickable(onClick = onNavigateToSyncCenter),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = .18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, null, tint = SuccessGreen)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Trendyol bağlantısı", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Canlı senkronizasyon merkezi hazır", color = Color.White.copy(alpha = .65f), fontSize = 11.sp)
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val result = repository.syncTrendyol()
                                    isSyncing = false
                                    snackbarHostState.showSnackbar(
                                        result.getOrDefault("Senkronizasyon tamamlandı.")
                                    )
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.testTag("sync_now_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandGold,
                                contentColor = Midnight
                            )
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Midnight,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Şimdi", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Bugünün kontrolü", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Midnight)
                Text("Önemli rakamlar ve bekleyen işler tek ekranda.", color = Color(0xFF6B7280), fontSize = 13.sp)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ExecutiveMetric("Ciro", "${String.format(Locale.US, "%.0f", sales)} ₺", "Kayıtlı satış", Icons.Default.TrendingUp, BrandGold, Modifier.weight(1f), onNavigateToProfit)
                    ExecutiveMetric("Net Kâr", "${String.format(Locale.US, "%.0f", profit)} ₺", "Hesaplanan", Icons.Default.AutoAwesome, SuccessGreen, Modifier.weight(1f), onNavigateToProfit)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ExecutiveMetric("Sipariş", "$activeOrders", "Aktif işlem", Icons.Default.ShoppingBag, InfoSky, Modifier.weight(1f), onNavigateToOrders)
                    ExecutiveMetric("Kritik Stok", "$criticalStock", "≤ 2 adet", Icons.Default.WarningAmber, DangerRed, Modifier.weight(1f), onNavigateToStock)
                }
            }

            item {
                Text("Aksiyon merkezi", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Midnight)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard("Siparişler", "$activeOrders aktif", Icons.Default.ShoppingBag, InfoSky, onNavigateToOrders, Modifier.weight(1f))
                    ActionCard("Stok", "${products.size} varyant", Icons.Default.Inventory2, BrandGoldDarkCompat(), onNavigateToStock, Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ActionCard("Sorular AI", "${questions.size} bekliyor", Icons.Default.QuestionAnswer, SuccessGreen, onNavigateToQuestions, Modifier.weight(1f))
                    ActionCard("İadeler", "${returns.size} kayıt", Icons.Default.Inventory2, DangerRed, onNavigateToOrders, Modifier.weight(1f))
                }
            }

            if (pendingAiTasksCount > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("pending_ai_banner").clickable(onClick = onNavigateToApprovals),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2E5BF))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).background(BrandGold.copy(alpha = .16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = BrandGold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("$pendingAiTasksCount AI görevi onay bekliyor", fontWeight = FontWeight.Bold, color = Midnight)
                                Text("Yanıt, fiyat ve içerik önerilerini tek merkezden incele.", fontSize = 12.sp, color = Color(0xFF6B5A2B))
                            }
                        }
                    }
                }
            }

            item {
                Text("Hızlı işlemler", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Midnight)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onNavigateToQuestions,
                        modifier = Modifier.weight(1f).testTag("quick_reply_button"),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("AI Yanıt") }
                    OutlinedButton(
                        onClick = onNavigateToDailyBrief,
                        modifier = Modifier.weight(1f).testTag("quick_daily_brief_button"),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("Günlük Rapor") }
                    OutlinedButton(
                        onClick = onNavigateToStock,
                        modifier = Modifier.weight(1f).testTag("quick_stock_button"),
                        shape = RoundedCornerShape(13.dp)
                    ) { Text("Stok") }
                }
            }

            item {
                Text(
                    "M&E Tekstil Aİ",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8F98),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ExecutiveMetric(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = Color(0xFF687386), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Midnight)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF8A8F98))
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Midnight)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF7B8494))
        }
    }
}

private fun BrandGoldDarkCompat(): Color = Color(0xFF8F6B22)
