package com.example.ui.screens.sync

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SyncLogEntity
import com.example.data.repository.MAndETekstilRepository
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncCenterScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val syncLogs by repository.syncLogs.collectAsState(initial = emptyList())
    val trendyolConfig = remember { repository.getTrendyolConfig() }
    val scope = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }
    var selectedInterval by remember { mutableStateOf(30) } // minutes

    val latestLog = syncLogs.firstOrNull()
    val sdf = remember { SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("tr")) }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Senkronizasyon Merkezi",
            subtitle = "Trendyol ➔ ERP Canlı Çift Yönlü Veri Hattı",
            onRefreshClick = {
                scope.launch {
                    isSyncing = true
                    val res = repository.runComprehensiveSync()
                    isSyncing = false
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Senkronizasyon başarıyla tamamlandı.")
                    } else {
                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Senkronizasyon hatası.")
                    }
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
            // Hero Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("sync_hero_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (latestLog?.status == "SUCCESS") SuccessGreen.copy(alpha = 0.2f)
                                            else if (latestLog?.status == "FAILED") DangerRed.copy(alpha = 0.2f)
                                            else InfoSky.copy(alpha = 0.2f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                                        contentDescription = "Sync",
                                        tint = if (latestLog?.status == "FAILED") DangerRed else SuccessGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isSyncing) "Senkronizasyon Yapılıyor..." else "Sistem Senkronize",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = if (latestLog != null) "Son Güncelleme: ${sdf.format(Date(latestLog.timestamp))}" else "Henüz kayıt yok",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            StatusPill(
                                text = if (trendyolConfig.sellerId.isNotBlank()) "Canlı API" else "Çevrimdışı",
                                colorType = if (trendyolConfig.sellerId.isNotBlank()) StatusColorType.SUCCESS else StatusColorType.INFO
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats in small grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("İşlenen Öğe", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${latestLog?.itemsProcessed ?: 0}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Hata / Atlanan", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${latestLog?.itemsFailed ?: 0}", color = if ((latestLog?.itemsFailed ?: 0) > 0) DangerRed else Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("İşlem Süresi", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${latestLog?.durationMs ?: 0} ms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sync action button
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    val res = repository.runComprehensiveSync()
                                    isSyncing = false
                                    if (res.isSuccess) {
                                        snackbarHostState.showSnackbar("Senkronizasyon tamamlandı.")
                                    } else {
                                        snackbarHostState.showSnackbar(res.exceptionOrNull()?.message ?: "Senkronizasyon hatası.")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sync_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Trendyol Verileri Çekiliyor...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hemen Senkronize Et", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Sync Settings & Rate Limiting Guard Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Otomatik Senkronizasyon & Hız Sınırı Koruması", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Trendyol API hız kotalarına (Rate Limit HTTP 429) karşı otomatik üstel geri çekilme (exponential backoff) devrededir.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Otomatik Eşitleme Aralığı:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 60, 0).forEach { mins ->
                                val label = if (mins == 0) "Manuel" else "$mins Dk"
                                FilterChip(
                                    selected = selectedInterval == mins,
                                    onClick = { selectedInterval = mins },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TrendyolOrange.copy(alpha = 0.15f),
                                        selectedLabelColor = TrendyolOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Historical Sync Logs Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NavyDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Senkronizasyon Geçmişi ve Kayıtlar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Text("${syncLogs.size} Kayıt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (syncLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Henüz senkronizasyon kaydı bulunmuyor.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(syncLogs) { log ->
                    SyncLogItemCard(log = log, sdf = sdf)
                }
            }
        }
    }
}

@Composable
fun SyncLogItemCard(log: SyncLogEntity, sdf: SimpleDateFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (log.status == "SUCCESS") Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (log.status == "SUCCESS") SuccessGreen else DangerRed,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tür: ${log.syncType}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.details,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("İşlenen: ${log.itemsProcessed} adet", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                    if (log.itemsFailed > 0) {
                        Text("Hatalı: ${log.itemsFailed}", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Medium)
                    }
                    Text("Süre: ${log.durationMs} ms", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
