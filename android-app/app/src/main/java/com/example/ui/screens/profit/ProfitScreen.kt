package com.example.ui.screens.profit

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.components.SectionTitle
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NavyDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ProfitScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val products by repository.products.collectAsState(initial = emptyList())
    val rules = remember { repository.getProfitRules() }
    val scope = rememberCoroutineScope()

    var salePriceText by remember { mutableStateOf("299.90") }
    var buyingPriceText by remember { mutableStateOf("110.00") }
    var commissionRateText by remember { mutableStateOf("18.5") }
    var shippingCostText by remember { mutableStateOf("35.0") }

    var aiAnalysisResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzingAi by remember { mutableStateOf(false) }

    val salePrice = salePriceText.toDoubleOrNull() ?: 0.0
    val buyingPrice = buyingPriceText.toDoubleOrNull() ?: 0.0
    val commissionRate = commissionRateText.toDoubleOrNull() ?: 18.5
    val shippingCost = shippingCostText.toDoubleOrNull() ?: 35.0

    val calcResult = repository.calculateProfit(salePrice, buyingPrice, commissionRate, shippingCost)

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Akıllı Kâr Hesaplayıcı",
            subtitle = "Kâr Marjı & Maliyet Analiz Motoru"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Quick Inventory Product Picker
            item {
                Text("Kayıtlı Ürünlerden Seçin:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { prod ->
                        Card(
                            modifier = Modifier.clickable {
                                salePriceText = prod.salePrice.toString()
                                buyingPriceText = prod.buyingPrice.toString()
                                commissionRateText = prod.commissionRate.toString()
                                shippingCostText = prod.shippingCost.toString()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(prod.productCode, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${prod.color} (${prod.size})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Calculation Inputs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = "Parametreler", tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Maliyet & Satış Parametreleri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = salePriceText,
                                onValueChange = { salePriceText = it },
                                label = { Text("Satış Tutarı (₺)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("calc_sale_price_input")
                            )
                            OutlinedTextField(
                                value = buyingPriceText,
                                onValueChange = { buyingPriceText = it },
                                label = { Text("Ürün Maliyeti (₺)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("calc_buying_price_input")
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = commissionRateText,
                                onValueChange = { commissionRateText = it },
                                label = { Text("Komisyon (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("calc_commission_input")
                            )
                            OutlinedTextField(
                                value = shippingCostText,
                                onValueChange = { shippingCostText = it },
                                label = { Text("Kargo Gideri (₺)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("calc_shipping_input")
                            )
                        }
                    }
                }
            }

            // Output Display Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (calcResult.isProfitable) SuccessGreen.copy(alpha = 0.08f) else DangerRed.copy(alpha = 0.08f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (calcResult.isProfitable) "Tahmini Net Kâr" else "Zarar Riski!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            StatusPill(
                                text = "Kâr Marjı: %${String.format(Locale.US, "%.1f", calcResult.profitMarginPercent)}",
                                colorType = if (calcResult.isProfitable) StatusColorType.SUCCESS else StatusColorType.DANGER
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${String.format(Locale.US, "%.2f", calcResult.netProfit)} ₺",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (calcResult.isProfitable) SuccessGreen else DangerRed
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Turkish breakdown lines
                        CalculationRow(label = "Satış Tutarı", value = "${String.format(Locale.US, "%.2f", calcResult.salePrice)} ₺")
                        CalculationRow(label = "Ürün Maliyeti", value = "-${String.format(Locale.US, "%.2f", calcResult.buyingPrice)} ₺")
                        CalculationRow(label = "Komisyon Tutarı (%${calcResult.commissionRate})", value = "-${String.format(Locale.US, "%.2f", calcResult.commissionAmount)} ₺")
                        CalculationRow(label = "Kargo Gideri", value = "-${String.format(Locale.US, "%.2f", calcResult.shippingCost)} ₺")
                        CalculationRow(label = "KDV/Stopaj (%${rules.defaultTaxRate})", value = "-${String.format(Locale.US, "%.2f", calcResult.taxAmount)} ₺")
                        CalculationRow(label = "İade Maliyeti Tamponu", value = "-${String.format(Locale.US, "%.2f", calcResult.returnBufferAmount)} ₺")
                        CalculationRow(label = "Paketleme & Diğer Giderler", value = "-${String.format(Locale.US, "%.2f", calcResult.otherExpenses)} ₺")
                        Spacer(modifier = Modifier.height(6.dp))
                        CalculationRow(label = "Toplam Giderler", value = "${String.format(Locale.US, "%.2f", calcResult.totalExpenses)} ₺", isBold = true)
                    }
                }
            }

            // AI Profit & Strategy Advisor (Slot 2)
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
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "AI", tint = TrendyolOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Slot 2: İş ve Kâr AI Analizi", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fiyat esnekliği, kargo barem avantajı ve 2'li set tavsiyeleri üretir.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (aiAnalysisResult != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.08f))
                            ) {
                                Text(
                                    text = aiAnalysisResult!!,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isAnalyzingAi = true
                                    val dummyProduct = products.firstOrNull() ?: return@launch
                                    val res = repository.runPricingAdvisor(dummyProduct.copy(salePrice = salePrice, buyingPrice = buyingPrice))
                                    isAnalyzingAi = false
                                    if (res.isSuccess) {
                                        aiAnalysisResult = res.content
                                    } else {
                                        snackbarHostState.showSnackbar("AI analizi: ${res.errorMessage}")
                                    }
                                }
                            },
                            enabled = !isAnalyzingAi,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isAnalyzingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analiz Ediliyor...")
                            } else {
                                Text("Fiyatlandırma Stratejisi Oluştur", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculationRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
