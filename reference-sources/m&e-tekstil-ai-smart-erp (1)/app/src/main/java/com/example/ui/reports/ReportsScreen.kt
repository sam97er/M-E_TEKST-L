package com.example.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.data.model.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "AI Raporlar & Analiz",
                            tint = PrimaryOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI Akıllı Raporlar & Analiz",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hareketsiz Stok, İlan Kalitesi & Fiyat Sihirbazı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadAllReports() },
                        modifier = Modifier.testTag("refresh_reports_button")
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

            // Top Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryOrange,
                edgePadding = 12.dp,
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
                            Icon(Icons.Default.HourglassDisabled, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hareketsiz Stok", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                            if (uiState.stagnantReport.critical_items_count_safe() > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Badge(containerColor = ErrorRed) {
                                    Text("${uiState.stagnantReport.criticalItemsCount}")
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlan Kalite & SEO", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fiyat & İçerik Sihirbazı", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Insights, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Yönetici Raporu", fontSize = 13.sp, fontWeight = if (uiState.selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            // Tab Content
            when (uiState.selectedTab) {
                0 -> StagnantStockTab(viewModel = viewModel, uiState = uiState)
                1 -> ListingAuditsTab(viewModel = viewModel, uiState = uiState)
                2 -> PricingAndContentWizardTab(viewModel = viewModel, uiState = uiState)
                3 -> ExecutiveReportTab(viewModel = viewModel, uiState = uiState)
            }
        }
    }
}

// Helper extension for safe count check
fun StagnantStockReportResponseDto.critical_items_count_safe(): Int = criticalItemsCount

// ============================================================================
// 1. Stagnant Stock Tab
// ============================================================================

@Composable
fun StagnantStockTab(
    viewModel: ReportsViewModel,
    uiState: ReportsUiState
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
            ReportMetricCard(
                title = "Bağlı Sermaye",
                value = "₺${String.format("%,.0f", uiState.stagnantReport.totalTiedCapitalTl)}",
                icon = Icons.Default.AccountBalanceWallet,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "Kurtarılabilir Nakit",
                value = "₺${String.format("%,.0f", uiState.stagnantReport.potentialCashRecoveryTl)}",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                color = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "Atıl Adet",
                value = "${uiState.stagnantReport.totalStagnantUnits}",
                icon = Icons.Default.Inventory,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            ReportMetricCard(
                title = "Kritik Varyant",
                value = "${uiState.stagnantReport.criticalItemsCount}",
                icon = Icons.Default.Warning,
                color = ErrorRed,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter by Days Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hareketsizlik Eşiği:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15, 20, 30, 60).forEach { days ->
                    FilterChip(
                        selected = uiState.minDaysFilter == days,
                        onClick = { viewModel.loadStagnantStock(days) },
                        label = { Text("${days}+ Gün", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryOrange,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (uiState.minDaysFilter == days) PrimaryOrange else BorderColor,
                            enabled = true,
                            selected = uiState.minDaysFilter == days
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.stagnantReport.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tebrikler! ${uiState.minDaysFilter}+ gündür satılmayan hareketsiz stok bulunmuyor.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.stagnantReport.items) { item ->
                    StagnantProductCard(item = item)
                }
            }
        }
    }
}

@Composable
fun StagnantProductCard(item: StagnantProductItemDto) {
    val (urgencyColor, urgencyBg, urgencyLabel) = when (item.urgencyLevel) {
        "CRITICAL" -> Triple(ErrorRed, ErrorContainer, "Acil Nakde Çevir (60+ Gün)")
        "MEDIUM" -> Triple(WarningAmber, WarningContainer, "Yavaş Talep (30+ Gün)")
        else -> Triple(PrimaryOrange, PrimaryOrange.copy(alpha = 0.12f), "İzleme Aşamasında")
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.productCode} • ${item.color ?: ""} / ${item.size ?: ""} • Barkod: ${item.barcode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    color = urgencyBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = urgencyLabel,
                        color = urgencyColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Stok Bakiyesi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${item.stockQuantity} Adet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(text = "Bağlı Sermaye", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₺${String.format("%,.2f", item.tiedCapitalTl)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ErrorRed)
                }
                Column {
                    Text(text = "Tavsiye Tasfiye Fiyatı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₺${String.format("%,.2f", item.suggestedClearancePrice)} (-%${item.suggestedDiscountPercent.toInt()})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.aiRecommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ============================================================================
// 2. Listing Quality & SEO Tab
// ============================================================================

@Composable
fun ListingAuditsTab(
    viewModel: ReportsViewModel,
    uiState: ReportsUiState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Katalog İlan Kalite Ortalaması", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${uiState.listingAudits.averageQualityScore} / 100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.listingAudits.averageQualityScore >= 70) SuccessGreen else WarningAmber
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "A/B Standart", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(text = "${uiState.listingAudits.highQualityCount}", fontWeight = FontWeight.Bold, color = SuccessGreen, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Geliştirilmeli", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(text = "${uiState.listingAudits.mediumQualityCount}", fontWeight = FontWeight.Bold, color = WarningAmber, style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Kritik Eksik", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        Text(text = "${uiState.listingAudits.needsImprovementCount}", fontWeight = FontWeight.Bold, color = ErrorRed, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(uiState.listingAudits.products) { product ->
                ProductAuditCard(
                    audit = product,
                    onOptimizeClick = {
                        viewModel.selectTab(2)
                        viewModel.selectProductForAnalysis(product.productId)
                    }
                )
            }
        }
    }
}

@Composable
fun ProductAuditCard(
    audit: ProductQualityAuditItemDto,
    onOptimizeClick: () -> Unit
) {
    val gradeColor = when (audit.grade) {
        "A" -> SuccessGreen
        "B" -> PrimaryOrange
        "C" -> WarningAmber
        else -> ErrorRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(gradeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = audit.grade, fontWeight = FontWeight.Bold, color = gradeColor, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = audit.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = "${audit.productCode} • ${audit.categoryName ?: "Kategori Yok"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    color = gradeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${audit.qualityScore} Puan",
                        color = gradeColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-scores Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AuditSubScoreChip("Başlık", "${audit.titleQualityScore}/30", Modifier.weight(1f))
                AuditSubScoreChip("Açıklama", "${audit.descriptionQualityScore}/40", Modifier.weight(1f))
                AuditSubScoreChip("Görseller", "${audit.imageQualityScore}/30", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attribute Checkpoints Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AuditCheckIcon("Ana Görsel", audit.hasPrimaryImage)
                AuditCheckIcon("Kumaş Oranı", audit.hasFabricComposition)
                AuditCheckIcon("Yıkama Bilgisi", audit.hasWashingInstructions)
                AuditCheckIcon("Manken Ölçüsü", audit.hasSizeChartInfo)
            }

            if (audit.deficiencies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Tespit Edilen Eksiklikler (${audit.deficiencies.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                audit.deficiencies.forEach { def ->
                    Text(
                        text = "• ${def.message} (${def.suggestion})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOptimizeClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryOrange),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI İçerik & Fiyat Sihirbazında Aç", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AuditSubScoreChip(label: String, score: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            Text(text = score, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun AuditCheckIcon(label: String, isOk: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (isOk) SuccessGreen else ErrorRed,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 10.sp, color = if (isOk) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ============================================================================
// 3. Pricing & Content Studio Tab
// ============================================================================

@Composable
fun PricingAndContentWizardTab(
    viewModel: ReportsViewModel,
    uiState: ReportsUiState
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Product Selection Chips
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Analiz Edilecek Ürünü Seçin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.listingAudits.products) { prod ->
                            FilterChip(
                                selected = uiState.selectedProductId == prod.productId,
                                onClick = { viewModel.selectProductForAnalysis(prod.productId) },
                                label = { Text(prod.productCode, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryOrange,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dynamic Pricing & Margin Simulator Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "🎯 Akıllı Fiyatlandırma & Kâr Marjı Simülatörü", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Trendyol komisyonu (%20), kargo (₺38.50), ambalaj ve KDV kesintileri dinamik hesaplanır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    // Target Margin Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Hedeflenen Net Kâr Marjı:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "%${uiState.targetMarginInput.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                    }

                    Slider(
                        value = uiState.targetMarginInput.toFloat(),
                        onValueChange = { viewModel.onTargetMarginChanged(it.toDouble()) },
                        valueRange = 10f..50f,
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryOrange,
                            activeTrackColor = PrimaryOrange,
                            inactiveTrackColor = BorderColor
                        )
                    )

                    uiState.pricingAdvice?.let { advice ->
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PricingMetricBox("Başa-Baş Taban Fiyat", "₺${String.format("%.2f", advice.breakEvenMinimumPrice)}", ErrorRed, Modifier.weight(1f))
                            PricingMetricBox("Önerilen Satış Fiyatı", "₺${String.format("%.2f", advice.suggestedOptimalPrice)}", SuccessGreen, Modifier.weight(1f))
                            PricingMetricBox("Flash Kampanya Fiyatı", "₺${String.format("%.2f", advice.suggestedFlashDealPrice)}", WarningAmber, Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "💡 AI Fiyatlandırma Stratejisi:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                                Text(text = advice.aiPricingStrategy, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // AI Content & SEO Studio Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "✨ Trendyol SEO Başlık & Açıklama Sihirbazı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "Arama hacmi yüksek tekstil anahtar kelimeleri ve dökümlü ürün maddeleri oluşturun.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = uiState.targetAudienceInput,
                        onValueChange = { viewModel.onTargetAudienceChanged(it) },
                        label = { Text("Hedef Kitle") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = BorderColor)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.toneOfVoiceInput,
                        onValueChange = { viewModel.onToneOfVoiceChanged(it) },
                        label = { Text("Yazım Dili & Tonu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryOrange, unfocusedBorderColor = BorderColor)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.generateOptimizedContent() },
                        enabled = !uiState.isOptimizingContent && uiState.selectedProductId != null,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_ai_content_button")
                    ) {
                        if (uiState.isOptimizingContent) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Yüksek Dönüşümlü SEO İçeriği Üret", fontWeight = FontWeight.Bold)
                    }

                    uiState.optimizedContent?.let { content ->
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "Önerilen Trendyol Başlığı:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(text = content.optimizedTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Öne Çıkan Maddeli Özellikler:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        content.optimizedBulletPoints.forEach { pt ->
                            Text(text = pt, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Etiketler (Hashtags):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryOrange)
                        Text(text = content.suggestedTags.joinToString(" "), style = MaterialTheme.typography.bodySmall, color = PrimaryOrangeDark, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
fun PricingMetricBox(title: String, price: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = price, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ============================================================================
// 4. Executive Summary Tab
// ============================================================================

@Composable
fun ExecutiveReportTab(
    viewModel: ReportsViewModel,
    uiState: ReportsUiState
) {
    val report = uiState.executiveSummary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Headline & Overall Health Score
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "M&E TEKSTİL YÖNETİCİ ÖZETİ", color = PrimaryOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(text = report.headline.ifEmpty { "İşletme Performans Raporu" }, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = "Rapor Tarihi: ${report.reportDate}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(PrimaryOrange.copy(alpha = 0.12f))
                                .border(2.dp, PrimaryOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${report.overallHealthScore}", color = PrimaryOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(text = "PUAN", color = PrimaryOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                    Text(
                        text = report.executiveSummaryText.ifEmpty { "Sistem verileri taranıyor..." },
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Key KPI Metrics Grid
        item {
            Text(text = "Temel Performans Göstergeleri (KPI)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                report.keyMetrics.chunked(2).forEach { rowMetrics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMetrics.forEach { metric ->
                            ExecutiveMetricCard(metric = metric, modifier = Modifier.weight(1f))
                        }
                        if (rowMetrics.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Strengths & Bottlenecks
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "💪 Güçlü Operasyonel Yönler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    report.topStrengths.forEach { s ->
                        Text(text = "• $s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = BorderColor)

                    Text(text = "⚠️ Kritik Riskler & Darboğazlar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                    Spacer(modifier = Modifier.height(6.dp))
                    report.criticalBottlenecks.forEach { b ->
                        Text(text = "• $b", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        // Strategic Actions List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "🚀 Tavsiye Edilen Öncelikli Eylemler", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    report.recommendedNextActions.forEachIndexed { idx, act ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Surface(
                                color = PrimaryOrange,
                                shape = CircleShape,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "${idx + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = act, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveMetricCard(metric: ExecutiveSummaryMetricDto, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = metric.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = metric.trend, style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontSize = 10.sp)
        }
    }
}

@Composable
fun ReportMetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
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
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, fontSize = 9.sp)
        }
    }
}
