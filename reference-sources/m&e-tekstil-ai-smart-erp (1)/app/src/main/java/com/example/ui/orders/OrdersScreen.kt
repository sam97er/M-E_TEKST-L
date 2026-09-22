package com.example.ui.orders

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.theme.BorderColor
import com.example.ui.theme.PrimaryOrange
import java.text.DecimalFormat

val ProfitGreen = Color(0xFF1B873F)
val LossRed = Color(0xFFD32F2F)
val WarningAmber = Color(0xFFE65100)
val DarkSurface = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val df = remember { DecimalFormat("#,##0.00") }

    LaunchedEffect(state.errorMessage, state.successMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Siparişler & Kâr/Zarar Analizi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Trendyol Satışları, Komisyon & Net Kâr Hesaplama",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadSummary()
                            viewModel.loadOrders()
                            viewModel.loadAnalytics()
                        },
                        modifier = Modifier.testTag("refresh_orders_btn")
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
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High-Level Financial Summary Header
            FinancialSummaryHeader(summary = state.salesSummary, df = df)

            // Modern Primary Tab Selector
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Siparişler (${state.orders.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null) },
                    modifier = Modifier.testTag("tab_orders")
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Kârlılık & Trend", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Analytics, contentDescription = null) },
                    modifier = Modifier.testTag("tab_analytics")
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Kâr Simülatörü", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                    modifier = Modifier.testTag("tab_simulator")
                )
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

            // Active Tab Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (state.selectedTab) {
                    0 -> OrdersListTab(
                        state = state,
                        df = df,
                        onSelectOrder = { viewModel.selectOrder(it) },
                        onFilterChange = { viewModel.setStatusFilter(it) },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onSortChange = { viewModel.setSortBy(it) }
                    )
                    1 -> ProfitAnalyticsTab(
                        summary = state.salesSummary,
                        dailyList = state.dailyAnalytics,
                        rankings = state.productRanking,
                        df = df
                    )
                    2 -> ProfitSimulatorTab(
                        state = state,
                        df = df,
                        onValueChange = { s, c, comm, ship, pack, vat ->
                            viewModel.updateSimInput(s, c, comm, ship, pack, vat)
                        },
                        onCalculate = { viewModel.calculateSimulation() }
                    )
                }

                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .testTag("orders_loading_indicator"),
                        color = PrimaryOrange
                    )
                }
            }
        }

        // Order Detail Bottom Sheet
        if (state.selectedOrder != null) {
            OrderDetailModalBottomSheet(
                order = state.selectedOrder!!,
                df = df,
                isUpdating = state.isUpdatingStatus,
                onDismiss = { viewModel.selectOrder(null) },
                onUpdateStatus = { orderId, newStatus ->
                    viewModel.updateOrderStatus(orderId, newStatus)
                }
            )
        }
    }
}

@Composable
fun FinancialSummaryHeader(summary: SalesSummaryDto, df: DecimalFormat) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FinancialKpiItem(
                    title = "Toplam Ciro",
                    value = "₺${df.format(summary.totalNetSales)}",
                    subtitle = "${summary.totalOrdersCount} Sipariş",
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(modifier = Modifier.height(36.dp), color = BorderColor)

                FinancialKpiItem(
                    title = "Tahmini Net Kâr",
                    value = "₺${df.format(summary.totalEstimatedNetProfit)}",
                    subtitle = "%${summary.overallProfitMarginPercent} Marj",
                    color = if (summary.totalEstimatedNetProfit >= 0) ProfitGreen else LossRed,
                    modifier = Modifier.weight(1f)
                )

                VerticalDivider(modifier = Modifier.height(36.dp), color = BorderColor)

                FinancialKpiItem(
                    title = "Komisyon & Kargo",
                    value = "₺${df.format(summary.totalCommissionPaid + summary.totalShippingPaid)}",
                    subtitle = "Trendyol Kesintisi",
                    color = WarningAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun FinancialKpiItem(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListTab(
    state: OrdersUiState,
    df: DecimalFormat,
    onSelectOrder: (OrderSummaryItemDto) -> Unit,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSortChange: (String) -> Unit
) {
    val filterOptions = listOf(
        "ALL" to "Tümü",
        "Created" to "Yeni",
        "Shipped" to "Kargoda",
        "Delivered" to "Teslim Edildi",
        "Returned" to "İadeler",
        "Cancelled" to "İptaller"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("orders_search_input"),
                placeholder = { Text("Sipariş No, Müşteri veya Şehir Ara...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryOrange) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )
        }

        // Status Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { (key, label) ->
                val isSelected = state.statusFilter.equals(key, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryOrange.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryOrange
                    ),
                    modifier = Modifier.testTag("filter_chip_$key")
                )
            }
        }

        // Orders List
        if (state.orders.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Filtreye uygun sipariş bulunamadı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderSummaryCard(
                        order = order,
                        df = df,
                        onClick = { onSelectOrder(order) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    order: OrderSummaryItemDto,
    df: DecimalFormat,
    onClick: () -> Unit
) {
    val isProfit = order.estimatedProfit >= 0
    val statusColor = when (order.status.uppercase()) {
        "DELIVERED", "TESLİM EDİLDİ" -> ProfitGreen
        "SHIPPED", "KARGODA" -> Color(0xFF0284C7)
        "CREATED", "YENİ" -> PrimaryOrange
        "RETURNED", "İADE" -> WarningAmber
        "CANCELLED", "İPTAL" -> LossRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Order Number & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalShipping,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = order.trendyolOrderNumber,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.status,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle Row: Customer and City
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${order.customerName} • ${order.city ?: "Türkiye"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${order.itemCount} Kalem Ürün",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = BorderColor.copy(alpha = 0.4f)
            )

            // Bottom Row: Net Sales & Estimated Net Profit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Satış Tutarı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₺${df.format(order.netAmount)}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (order.realizedProfit != null) "Kesinleşmiş Kâr" else "Tahmini Net Kâr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₺${df.format(order.realizedProfit ?: order.estimatedProfit)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isProfit) ProfitGreen else LossRed
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            color = (if (isProfit) ProfitGreen else LossRed).copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = "%${order.profitMarginPercent}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = if (isProfit) ProfitGreen else LossRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitAnalyticsTab(
    summary: SalesSummaryDto,
    dailyList: List<DailySalesPointDto>,
    rankings: List<ProductProfitRankingDto>,
    df: DecimalFormat
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cost Breakdown Structure Card (COGS vs Commission vs Shipping vs Net Profit)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Genel Gider & Kâr Dağılımı (Waterfall)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfitWaterfallRow(label = "(+) Toplam Ciro", amount = summary.totalNetSales, color = MaterialTheme.colorScheme.onSurface, df = df)
                    ProfitWaterfallRow(label = "(-) Ürün Alış Maliyetleri (COGS)", amount = summary.totalProductCogs, color = LossRed, df = df)
                    ProfitWaterfallRow(label = "(-) Trendyol Komisyonu", amount = summary.totalCommissionPaid, color = WarningAmber, df = df)
                    ProfitWaterfallRow(label = "(-) Kargo Giderleri", amount = summary.totalShippingPaid, color = WarningAmber, df = df)
                    ProfitWaterfallRow(label = "(-) KDV & Stopaj", amount = summary.totalTaxPaid, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)
                    ProfitWaterfallRow(label = "(-) Paketleme & Platform Hizmeti", amount = summary.totalOtherExpenses, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "(=) Toplam Net Kâr",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ProfitGreen
                        )
                        Text(
                            text = "₺${df.format(summary.totalEstimatedNetProfit)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ProfitGreen
                        )
                    }
                }
            }
        }

        // Daily Trend Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Son 7 Günlük Kâr & Satış Trendi",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = ProfitGreen)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    dailyList.forEach { point ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = point.date.takeLast(5),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${point.orderCount} Sipariş",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "₺${df.format(point.grossRevenue)} Ciro",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "₺${df.format(point.netProfit)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ProfitGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Product Ranking Section
        item {
            Text(
                text = "En Çok Kâr Getiren Ürünler & Kârlılık Sıralaması",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(rankings) { rank ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rank.productName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Barkod: ${rank.barcode} • ${rank.totalUnitsSold} Adet Satıldı",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₺${df.format(rank.totalProfit)}",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (rank.totalProfit >= 0) ProfitGreen else LossRed
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (rank.isLowMargin) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Düşük Marj",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                            Text(
                                text = "%${rank.profitMarginPercent} Marj",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (rank.isLowMargin) WarningAmber else ProfitGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitWaterfallRow(label: String, amount: Double, color: Color, df: DecimalFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = "₺${df.format(amount)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

@Composable
fun ProfitSimulatorTab(
    state: OrdersUiState,
    df: DecimalFormat,
    onValueChange: (String?, String?, String?, String?, String?, String?) -> Unit,
    onCalculate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PrimaryOrange.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = PrimaryOrange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trendyol Fiyat & Kâr Simülatörü: Ürününüzü satışa koymadan önce komisyon, kargo, KDV ve ambalaj giderlerini düşerek net kârınızı hesaplayın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Inputs Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Maliyet ve Fiyat Parametreleri",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.simSalePrice,
                            onValueChange = { onValueChange(it, null, null, null, null, null) },
                            label = { Text("Satış Fiyatı (₺)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("sim_sale_price_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.simPurchaseCost,
                            onValueChange = { onValueChange(null, it, null, null, null, null) },
                            label = { Text("Alış/İmalat Maliyeti (₺)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("sim_purchase_cost_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.simCommissionRate,
                            onValueChange = { onValueChange(null, null, it, null, null, null) },
                            label = { Text("Komisyon Oranı (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("sim_commission_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.simShippingCost,
                            onValueChange = { onValueChange(null, null, null, it, null, null) },
                            label = { Text("Kargo Bedeli (₺)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("sim_shipping_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.simPackagingCost,
                            onValueChange = { onValueChange(null, null, null, null, it, null) },
                            label = { Text("Ambalaj & Etiket (₺)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = state.simVatRate,
                            onValueChange = { onValueChange(null, null, null, null, null, it) },
                            label = { Text("KDV Oranı (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onCalculate,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sim_calculate_btn")
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kâr & Marjı Hesapla", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Simulation Result Display
        state.simResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Simülasyon Sonucu",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Surface(
                                color = (if (res.isProfitable) ProfitGreen else LossRed).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (res.isProfitable) "%${res.profitMarginPercent} KÂR MARJI" else "ZARAR",
                                    color = if (res.isProfitable) ProfitGreen else LossRed,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Net Profit Box
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = (if (res.isProfitable) ProfitGreen else LossRed).copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Net Kalan Kâr (Adet Başı)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "₺${df.format(res.netProfit)}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = if (res.isProfitable) ProfitGreen else LossRed
                                )
                                Text(
                                    text = "Yatırım Getirisi (ROI): %${res.roiPercent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ProfitWaterfallRow(label = "Satış Fiyatı", amount = res.salePrice, color = MaterialTheme.colorScheme.onSurface, df = df)
                        ProfitWaterfallRow(label = "Ürün Alış Maliyeti", amount = res.purchaseCost, color = LossRed, df = df)
                        ProfitWaterfallRow(label = "Trendyol Komisyonu", amount = res.commissionAmount, color = WarningAmber, df = df)
                        ProfitWaterfallRow(label = "Kargo Bedeli", amount = res.shippingCost, color = WarningAmber, df = df)
                        ProfitWaterfallRow(label = "Platform Hizmet Bedeli", amount = res.serviceFee, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)
                        ProfitWaterfallRow(label = "KDV Tutarı", amount = res.taxAmount, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)
                        ProfitWaterfallRow(label = "Ambalaj Gideri", amount = res.packagingCost, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Key Target Benchmarks
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Başa Baş Fiyatı (0 Kâr)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₺${df.format(res.breakevenPrice)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("%20 Kâr Hedef Fiyatı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₺${df.format(res.targetPriceFor20PercentMargin)}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = PrimaryOrange)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Recommendation message
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (res.isProfitable) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (res.isProfitable) ProfitGreen else LossRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = res.recommendation,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailModalBottomSheet(
    order: OrderDetailDto,
    df: DecimalFormat,
    isUpdating: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sipariş Detayı & Kâr Dökümü",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = order.trendyolOrderNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryOrange
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Kapat")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer & Delivery Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Müşteri: ${order.customerName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Text("Şehir: ${order.city ?: "Türkiye"}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Durum: ${order.status}", style = MaterialTheme.typography.bodySmall, color = PrimaryOrange)
                        }
                    }
                }

                // Ordered Items
                item {
                    Text(
                        text = "Sipariş Edilen Ürünler (${order.items.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                items(order.items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Barkod: ${item.barcode} • ${item.quantity} Adet", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₺${df.format(item.totalPrice)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text("Maliyet: ₺${df.format(item.totalCost)}", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Profit Waterfall Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Finansal Ayrıştırma (Waterfall)", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(8.dp))

                            val bd = order.breakdown
                            ProfitWaterfallRow(label = "(+) Net Satış Tutarı", amount = bd.netSalesAmount, color = MaterialTheme.colorScheme.onSurface, df = df)
                            ProfitWaterfallRow(label = "(-) Ürün Alış Maliyeti", amount = bd.productPurchaseCost, color = LossRed, df = df)
                            ProfitWaterfallRow(label = "(-) Trendyol Komisyonu (%${bd.trendyolCommissionRateAvg})", amount = bd.trendyolCommissionAmount, color = WarningAmber, df = df)
                            ProfitWaterfallRow(label = "(-) Kargo Ücreti", amount = bd.shippingCost, color = WarningAmber, df = df)
                            ProfitWaterfallRow(label = "(-) KDV & Vergi", amount = bd.taxAmount, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)
                            ProfitWaterfallRow(label = "(-) Ambalaj & Hizmet Bedeli", amount = bd.packagingAndHandling + bd.serviceFee, color = MaterialTheme.colorScheme.onSurfaceVariant, df = df)

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Tahmini Net Kâr", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ProfitGreen)
                                    Text("Kâr Marjı: %${bd.profitMarginPercent}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "₺${df.format(bd.estimatedNetProfit)}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (bd.estimatedNetProfit >= 0) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }

                // Order Status Quick Action Buttons
                item {
                    Text("Sipariş Durumunu Güncelle", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onUpdateStatus(order.id, "Shipped") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Kargoda", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { onUpdateStatus(order.id, "Delivered") },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Teslim Edildi", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onUpdateStatus(order.id, "Returned") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("İade Alındı", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
