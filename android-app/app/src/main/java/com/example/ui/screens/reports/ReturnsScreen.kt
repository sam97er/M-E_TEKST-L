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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
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
fun ReturnsScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val returns by repository.returns.collectAsState(initial = emptyList())
    val orders by repository.orders.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var aiAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showAddReturnModal by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "İade & Kalite Analizi",
            subtitle = "${returns.size} İade Kaydı • Kalıp & Kumaş Optimizasyonu"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Action & AI Card
            item {
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = TrendyolOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("İade Sebepleri Örüntü Taraması", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            Button(
                                onClick = { showAddReturnModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("İade Al", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tekrarlanan beden şikayetlerini, kumaş/renk uyumsuzluklarını ve açıklamada eksik kalan noktaları tespit eder.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (aiAnalysisResult != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = aiAnalysisResult!!,
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isAnalyzing = true
                                    val result = repository.runReturnsAnalysis()
                                    isAnalyzing = false
                                    if (result.isSuccess) {
                                        aiAnalysisResult = result.content
                                        snackbarHostState.showSnackbar("İade örüntü analizi tamamlandı.")
                                    } else {
                                        snackbarHostState.showSnackbar(result.content)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("run_return_analysis_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isAnalyzing
                        ) {
                            if (isAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("İade Yorumları Analiz Ediliyor...")
                            } else {
                                Text("Tüm İadeleri Analiz Et (Slot 3 AI)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Returns List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Kayıtlı İade Bildirimleri", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${returns.size} Adet", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (returns.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Kayıtlı iade bulunmamaktadır.", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(returns) { ret ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "İade ID: ${ret.returnId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                StatusPill(
                                    text = ret.restockAction,
                                    colorType = if (ret.restockAction == "STOKA_EKLENDİ") StatusColorType.SUCCESS else StatusColorType.DANGER
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sipariş: ${ret.orderNumber} • ${ret.productTitle} [${ret.color} - ${ret.size}]",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Sebep: ${ret.returnReason}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DangerRed)
                                    if (ret.customerComment.isNotBlank()) {
                                        Text("Müşteri Yorumu: \"${ret.customerComment}\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tarih: ${sdf.format(Date(ret.returnDate))}", fontSize = 11.sp, color = Color.Gray)
                                Text("İşleyen: ${ret.processedBy}", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Process Return
    if (showAddReturnModal) {
        val firstOrder = orders.firstOrNull()
        var orderNumInput by remember { mutableStateOf(firstOrder?.orderNumber ?: "TY-ORD-101") }
        var barcodeInput by remember { mutableStateOf(firstOrder?.barcode ?: "868000100101") }
        var returnQtyInput by remember { mutableStateOf("1") }
        var reasonInput by remember { mutableStateOf("Beden Küçük Geldi / Kalıp Dar") }
        var commentInput by remember { mutableStateOf("Kollar ve omuz kısmı beklediğimden dar geldi.") }
        var restockActionChoice by remember { mutableStateOf("STOKA_EKLENDİ") }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddReturnModal = false },
            title = { Text("İade İşlemi ve Stok Hareketi") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "İade adedi, siparişteki satılan adedi aşamaz. Seçime göre ürün stoka tekrar eklenir veya defolu ayrılır.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = orderNumInput,
                            onValueChange = { orderNumInput = it },
                            label = { Text("Sipariş Numarası") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = barcodeInput,
                            onValueChange = { barcodeInput = it },
                            label = { Text("Ürün Barkodu") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = returnQtyInput,
                            onValueChange = { returnQtyInput = it },
                            label = { Text("İade Adedi") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = { reasonInput = it },
                            label = { Text("İade Sebebi") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            label = { Text("Müşteri Açıklaması") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Stok Eylemi:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = restockActionChoice == "STOKA_EKLENDİ",
                                onClick = { restockActionChoice = "STOKA_EKLENDİ" },
                                colors = RadioButtonDefaults.colors(selectedColor = SuccessGreen)
                            )
                            Text("Tekrar Stoka Ekle (Sağlam)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = restockActionChoice == "İADE_DEFOLU",
                                onClick = { restockActionChoice = "İADE_DEFOLU" },
                                colors = RadioButtonDefaults.colors(selectedColor = DangerRed)
                            )
                            Text("Defolu / Kusurlu Ayır (Stoka Ekleme)", fontSize = 12.sp)
                        }
                    }
                    if (errorText != null) {
                        item {
                            Text(errorText!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = returnQtyInput.toIntOrNull() ?: 1
                        val retId = "RET-${System.currentTimeMillis().toString().takeLast(6)}"
                        scope.launch {
                            val res = repository.executeReturnTransaction(
                                returnId = retId,
                                orderNumber = orderNumInput,
                                barcode = barcodeInput,
                                returnQty = qty,
                                reason = reasonInput,
                                customerComment = commentInput,
                                restockAction = restockActionChoice
                            )
                            if (res.isSuccess) {
                                showAddReturnModal = false
                                snackbarHostState.showSnackbar("İade işlemi tamamlandı ($retId).")
                            } else {
                                errorText = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                ) {
                    Text("İadeyi Onayla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddReturnModal = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
