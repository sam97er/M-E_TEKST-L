package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.DecimalFormat

private val CurrencyFmt = DecimalFormat("#,##0.00")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onNavigateToSection: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "M&E",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "M&E Tekstil ERP",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Genel Yönetim & Canlı Nabız",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadDashboardSummary() },
                        modifier = Modifier.testTag("btn_refresh_dashboard")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = PrimaryOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && uiState.summary.salesKpis.todayOrdersCount == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.testTag("dashboard_loading"),
                    color = PrimaryOrange
                )
            }
        } else {
            val summary = uiState.summary
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // 1. System Pulse Bar
                item {
                    SystemPulseHeaderCard(
                        pulse = summary.systemPulse,
                        isSyncing = uiState.isSyncing,
                        onQuickSync = { viewModel.triggerQuickTrendyolSync() },
                        onNavigateSettings = { onNavigateToSection("SETTINGS") }
                    )
                }

                // 2. Critical Action Required Alerts (if any)
                if (summary.criticalAlerts.isNotEmpty()) {
                    item {
                        Text(
                            "Eylem Gerektiren Durumlar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    items(summary.criticalAlerts) { alert ->
                        DashboardAlertCard(
                            alert = alert,
                            onClick = { onNavigateToSection(alert.actionTargetSection) }
                        )
                    }
                }

                // 3. Sales & Net Profit KPIs
                item {
                    SalesAndProfitOverviewSection(
                        sales = summary.salesKpis,
                        onViewOrders = { onNavigateToSection("ORDERS") }
                    )
                }

                // 4. Operational Twin-Cards: Questions & Stock Health
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer Questions Hub Card
                        CustomerQuestionsKpiCard(
                            modifier = Modifier.weight(1f),
                            questionKpis = summary.questionKpis,
                            onClick = { onNavigateToSection("QUESTIONS") }
                        )

                        // Inventory Health Card
                        InventoryHealthKpiCard(
                            modifier = Modifier.weight(1f),
                            inventoryKpis = summary.inventoryKpis,
                            onClick = { onNavigateToSection("PRODUCTS") }
                        )
                    }
                }

                // 5. Quick Actions Hub
                item {
                    QuickActionsGridSection(
                        isSyncing = uiState.isSyncing,
                        onSyncTrendyol = { viewModel.triggerQuickTrendyolSync() },
                        onReviewQuestions = { onNavigateToSection("QUESTIONS") },
                        onViewOrders = { onNavigateToSection("ORDERS") },
                        onAiReports = { onNavigateToSection("REPORTS") },
                        onProducts = { onNavigateToSection("PRODUCTS") },
                        onTelegram = { onNavigateToSection("TELEGRAM") }
                    )
                }

                // 6. Unified Recent Activity Feed
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Son İşlem & Sistem Akışı",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tüm Kayıtlar",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryOrange,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { onNavigateToSection("AUDIT") }
                                .padding(4.dp)
                        )
                    }
                }

                if (summary.recentActivities.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Henüz işlem aktivitesi kaydedilmedi.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(summary.recentActivities) { activity ->
                        ActivityFeedItemCard(activity = activity)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun SystemPulseHeaderCard(
    pulse: SystemPulseDto,
    isSyncing: Boolean,
    onQuickSync: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (pulse.overallHealth == "HEALTHY") ProfitGreen else WarningAmber
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sistem Durumu: ${if (pulse.overallHealth == "HEALTHY") "Kusursuz & Çevrimiçi" else "Dikkat Gerekiyor"}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryOrange
                    )
                } else {
                    FilledTonalButton(
                        onClick = onQuickSync,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = PrimaryOrange.copy(alpha = 0.15f),
                            contentColor = PrimaryOrange
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_dashboard_quick_sync")
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eşitle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pulse Chips Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PulseChip(
                    modifier = Modifier.weight(1f),
                    title = "AI Motoru",
                    value = "${pulse.activeAiProvider}",
                    subValue = pulse.aiModelName,
                    isOk = pulse.aiIsActive,
                    icon = Icons.Default.AutoAwesome
                )
                PulseChip(
                    modifier = Modifier.weight(1f),
                    title = "Trendyol",
                    value = if (pulse.trendyolConnected) "Bağlı" else "Bekliyor",
                    subValue = pulse.trendyolLastSync ?: "-",
                    isOk = pulse.trendyolConnected,
                    icon = Icons.Default.Storefront
                )
                PulseChip(
                    modifier = Modifier.weight(1f),
                    title = "Telegram",
                    value = if (pulse.telegramBotActive) "Devrede" else "Kapalı",
                    subValue = "${pulse.telegramPendingQueueCount} bekleyen",
                    isOk = pulse.telegramBotActive,
                    icon = Icons.Default.Send
                )
            }
        }
    }
}

@Composable
fun PulseChip(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subValue: String,
    isOk: Boolean,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (isOk) ProfitGreen else WarningAmber
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subValue,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashboardAlertCard(
    alert: DashboardAlertItemDto,
    onClick: () -> Unit
) {
    val (bgColor, accentColor, icon) = when (alert.alertType.uppercase()) {
        "CRITICAL" -> Triple(ErrorContainer, LossRed, Icons.Default.Error)
        "WARNING" -> Triple(WarningContainer, WarningAmber, Icons.Default.Warning)
        else -> Triple(InfoContainer, InfoBlue, Icons.Default.Info)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("card_alert_${alert.id}"),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accentColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onClick,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = accentColor, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(alert.actionLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SalesAndProfitOverviewSection(
    sales: SalesKpisDto,
    onViewOrders: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewOrders() }
            .testTag("card_sales_overview"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = PrimaryOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Bugünkü Satış & Kârlılık",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = if (sales.todayProfitMarginPercent >= 20.0) ProfitGreen.copy(alpha = 0.15f) else PrimaryOrange.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "%${sales.todayProfitMarginPercent} Marj",
                        color = if (sales.todayProfitMarginPercent >= 20.0) ProfitGreen else PrimaryOrange,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Bugün Net Kâr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₺${CurrencyFmt.format(sales.todayNetProfitTl)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = if (sales.todayNetProfitTl >= 0) ProfitGreen else LossRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Brüt Ciro (${sales.todayOrdersCount} Sipariş)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₺${CurrencyFmt.format(sales.todayGrossRevenueTl)}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = BorderColor.copy(alpha = 0.6f)
            )

            // 7 Days Rolling Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Son 7 Gün: ${sales.sevenDayOrdersCount} sipariş • ₺${CurrencyFmt.format(sales.sevenDayGrossRevenueTl)} ciro",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Net: ₺${CurrencyFmt.format(sales.sevenDayNetProfitTl)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = if (sales.sevenDayNetProfitTl >= 0) ProfitGreen else LossRed
                )
            }
        }
    }
}

@Composable
fun CustomerQuestionsKpiCard(
    modifier: Modifier = Modifier,
    questionKpis: QuestionKpisDto,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("card_kpi_questions"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                if (questionKpis.draftReadyForApprovalCount > 0) {
                    Surface(
                        color = PrimaryOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${questionKpis.draftReadyForApprovalCount} Onay",
                            color = PrimaryOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${questionKpis.draftReadyForApprovalCount + questionKpis.unansweredCount}",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Bekleyen Soru",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Bugün Gönderilen: ${questionKpis.sentTodayCount}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = ProfitGreen
            )
        }
    }
}

@Composable
fun InventoryHealthKpiCard(
    modifier: Modifier = Modifier,
    inventoryKpis: InventoryKpisDto,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("card_kpi_inventory"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = PrimaryOrange,
                    modifier = Modifier.size(20.dp)
                )
                if (inventoryKpis.outOfStockSkusCount > 0) {
                    Surface(
                        color = LossRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${inventoryKpis.outOfStockSkusCount} Bitti",
                            color = LossRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${inventoryKpis.totalPhysicalUnits} Adet",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "${inventoryKpis.totalVariantSkus} Varyant SKU",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Atıl Stok: ₺${CurrencyFmt.format(inventoryKpis.stagnantTiedCapitalTl)}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = if (inventoryKpis.stagnantTiedCapitalTl > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun QuickActionsGridSection(
    isSyncing: Boolean,
    onSyncTrendyol: () -> Unit,
    onReviewQuestions: () -> Unit,
    onViewOrders: () -> Unit,
    onAiReports: () -> Unit,
    onProducts: () -> Unit,
    onTelegram: () -> Unit
) {
    Column {
        Text(
            "Hızlı Aksiyonlar & Modüller",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Trendyol Eşitle",
                icon = Icons.Default.Sync,
                onClick = onSyncTrendyol,
                tag = "btn_act_sync"
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Soru Onayları",
                icon = Icons.Default.FactCheck,
                onClick = onReviewQuestions,
                tag = "btn_act_questions"
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Kâr/Zarar",
                icon = Icons.Default.MonetizationOn,
                onClick = onViewOrders,
                tag = "btn_act_orders"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "AI Raporları",
                icon = Icons.Default.Analytics,
                onClick = onAiReports,
                tag = "btn_act_reports"
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Ürün & Stok",
                icon = Icons.Default.Checkroom,
                onClick = onProducts,
                tag = "btn_act_products"
            )
            QuickActionButton(
                modifier = Modifier.weight(1f),
                title = "Telegram Bot",
                icon = Icons.Default.NotificationsActive,
                onClick = onTelegram,
                tag = "btn_act_telegram"
            )
        }
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .testTag(tag),
        shape = RoundedCornerShape(10.dp),
        border = CardDefaults.outlinedCardBorder(),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrimaryOrange)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ActivityFeedItemCard(activity: ActivityFeedItemDto) {
    val (icon, iconColor) = when (activity.activityType.uppercase()) {
        "ORDER" -> Pair(Icons.Default.ShoppingBag, ProfitGreen)
        "QUESTION" -> Pair(Icons.Default.QuestionAnswer, PrimaryOrange)
        "STOCK" -> Pair(Icons.Default.Inventory2, InfoBlue)
        "AUDIT" -> Pair(Icons.Default.History, WarningAmber)
        else -> Pair(Icons.Default.Notifications, PrimaryOrange)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("feed_item_${activity.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        activity.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        activity.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

