package com.example.ui.screens.reports

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DailyBriefScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val briefs by repository.dailyBriefs.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Günlük İş ve Kâr Özeti",
            subtitle = "Slot 2: İş ve Kâr AI • Stratejik Yönetici Raporu"
        )

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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bugünün Yönetici Özetini Hazırla", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Güncel satış, kâr, kritik stoklar ve müşteri geri bildirimlerini harmanlayarak somut aksiyon adımları sunar.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    isGenerating = true
                                    val res = repository.generateDailyBrief()
                                    isGenerating = false
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Günlük rapor üretildi ve kaydedildi.")
                                    } else {
                                        snackbarHostState.showSnackbar(res.errorMessage)
                                    }
                                }
                            },
                            enabled = !isGenerating,
                            modifier = Modifier.fillMaxWidth().testTag("generate_daily_brief_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rapor Üretiliyor...")
                            } else {
                                Text("Yeni Günlük Özet Üret (Slot 2)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (briefs.isEmpty()) {
                item {
                    Text(
                        text = "Henüz üretilmiş günlük rapor bulunmuyor. Yukarıdaki butona basarak bugünün raporunu oluşturabilirsiniz.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(briefs, key = { it.reportDate }) { brief ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Rapor Tarihi: ${brief.reportDate}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                StatusPill(
                                    text = "Kâr: ${String.format(Locale.US, "%.2f", brief.estimatedProfit)} ₺",
                                    colorType = StatusColorType.SUCCESS
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Satış: ${String.format(Locale.US, "%.2f", brief.totalSales)} ₺", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Sipariş: ${brief.orderCount} Adet", fontSize = 12.sp)
                                Text("Kritik Stok: ${brief.lowStockCount}", fontSize = 12.sp, color = TrendyolOrange)
                                Text("İade: ${brief.returnCount}", fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Yapay Zeka Analiz & Değerlendirmesi:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = brief.aiInterpretation,
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Tavsiye Edilen Aksiyonlar:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(text = brief.suggestedActions, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        repository.sendTelegramTest("📊 ${brief.reportDate} Raporu:\n\n${brief.aiInterpretation}")
                                        snackbarHostState.showSnackbar("Rapor Telegram'a iletildi.")
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Send, contentDescription = "Paylaş", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Telegram'a Gönder", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
