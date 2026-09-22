package com.example.ui.screens.growth

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TrendyolProductEntity
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import kotlinx.coroutines.launch

@Composable
fun GrowthScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val products by repository.products.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: SEO & Başlık/Açıklama, 1: Kalite Denetçisi, 2: Fiyat Stratejisi
    var selectedProduct by remember { mutableStateOf<TrendyolProductEntity?>(null) }

    var optimizationResult by remember { mutableStateOf<String?>(null) }
    var qualityCheckResult by remember { mutableStateOf<String?>(null) }
    var pricingResult by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    val activeProduct = selectedProduct ?: products.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Büyüme & Kalite AI",
            subtitle = "Slot 3: Ürün ve Büyüme AI • SEO, Kalite & Fiyat"
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
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("SEO & İçerik", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Kalite Kontrol", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Fiyat Önerisi", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Product Selector Strip
            item {
                Text("İncelenecek Ürünü Seçin:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products) { p ->
                        val isSelected = activeProduct?.barcode == p.barcode
                        Card(
                            modifier = Modifier.clickable {
                                selectedProduct = p
                                optimizationResult = null
                                qualityCheckResult = null
                                pricingResult = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TrendyolOrange else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = p.productCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${p.color} (${p.size})",
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (activeProduct != null) {
                when (selectedTab) {
                    0 -> {
                        // SEO & Content Generator
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
                                        Text("Trendyol Başlık, Açıklama & Anahtar Kelime", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Mevcut Başlık: ${activeProduct.title}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text("Kategori: ${activeProduct.categoryName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (optimizationResult != null) {
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.08f))
                                        ) {
                                            Text(
                                                text = optimizationResult!!,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val res = repository.generateProductOptimization(
                                                    activeProduct.productCode,
                                                    activeProduct.title,
                                                    activeProduct.categoryName,
                                                    "M&E Tekstil %100 Pamuklu ürün"
                                                )
                                                isLoading = false
                                                if (res.isSuccess) {
                                                    optimizationResult = res.content
                                                } else {
                                                    snackbarHostState.showSnackbar(res.errorMessage)
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                        } else {
                                            Text("SEO & Açıklama Üret (Slot 3)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Quality Checker
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = "Kalite", tint = SuccessGreen)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Trendyol Kalite & Politika Denetçisi", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Başlık formatı, yanıltıcı iddialar, iade riski ve kumaş içeriği kontrolü.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (qualityCheckResult != null) {
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f))
                                        ) {
                                            Text(
                                                text = qualityCheckResult!!,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val res = repository.runProductQualityCheck(activeProduct)
                                                isLoading = false
                                                if (res.isSuccess) {
                                                    qualityCheckResult = res.content
                                                } else {
                                                    snackbarHostState.showSnackbar(res.errorMessage)
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                        } else {
                                            Text("Kalite Denetimi Başlat", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Pricing Advisor
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.PriceChange, contentDescription = "Fiyat", tint = TrendyolOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Fiyatlandırma & Kâr Strateji Önerisi", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Yüksek kâr, hızlı satış ve kampanya senaryoları simülasyonu.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (pricingResult != null) {
                                        Card(
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.08f))
                                        ) {
                                            Text(
                                                text = pricingResult!!,
                                                modifier = Modifier.padding(12.dp),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                isLoading = true
                                                val res = repository.runPricingAdvisor(activeProduct)
                                                isLoading = false
                                                if (res.isSuccess) {
                                                    pricingResult = res.content
                                                } else {
                                                    snackbarHostState.showSnackbar(res.errorMessage)
                                                }
                                            }
                                        },
                                        enabled = !isLoading,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                        } else {
                                            Text("3 Fiyat Senaryosu Oluştur", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
