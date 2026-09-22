package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AppLanguage
import com.example.ui.dialogs.AddProductDialog
import com.example.ui.dialogs.AdjustStockDialog
import com.example.ui.dialogs.LanguageCurrencyDialog
import com.example.ui.dialogs.NewTicketDialog
import com.example.ui.dialogs.OrderReceiptDialog
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.screens.BarcodeScannerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.SalesPosScreen
import com.example.ui.screens.StaffDashboardScreen
import com.example.ui.screens.SupportAndBackupScreen
import com.example.ui.screens.TrendyolScreen
import com.example.ui.theme.ErpDanger
import com.example.ui.theme.ErpNavyPrimary
import com.example.ui.theme.ErpStrings
import com.example.ui.theme.ErpSuccess
import com.example.ui.theme.ErpTealSecondary
import com.example.ui.theme.SmartErpTheme
import com.example.ui.theme.TrendyolOrange
import com.example.ui.viewmodel.ErpTab
import com.example.ui.viewmodel.ErpViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ErpViewModel = viewModel()
            SmartErpApp(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartErpApp(viewModel: ErpViewModel) {
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()

    val products by viewModel.products.collectAsStateWithLifecycle()
    val lowStockProducts by viewModel.lowStockProducts.collectAsStateWithLifecycle()
    val warehouses by viewModel.warehouses.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val trendyolLogs by viewModel.trendyolLogs.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val tickets by viewModel.tickets.collectAsStateWithLifecycle()
    val backups by viewModel.backups.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val trendyolConfig by viewModel.trendyolConfig.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastScannedProduct by viewModel.lastScannedProduct.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val showPaymentDialog by viewModel.showPaymentDialog.collectAsState()
    val showAddProductDialog by viewModel.showAddProductDialog.collectAsState()
    val showLangCurrencyDialog by viewModel.showLangCurrencyDialog.collectAsState()
    val showNewTicketDialog by viewModel.showNewTicketDialog.collectAsState()
    val editingStockProduct by viewModel.editingProductStock.collectAsState()
    val lastCompletedOrder by viewModel.lastCompletedOrder.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val layoutDirection = when (language) {
        AppLanguage.ARABIC -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

    SmartErpTheme {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = ErpTealSecondary,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = ErpStrings.get("app_title", language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "متصل بترنديول • ${currency.code}",
                                        fontSize = 10.sp,
                                        color = Color(0xFFD0E1FD)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Quick Trendyol Sync Status Pill
                            Surface(
                                color = TrendyolOrange.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setTab(ErpTab.TRENDYOL) }
                                    .padding(end = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = "Trendyol",
                                        tint = TrendyolOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Trendyol",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Language & Currency Selector Button
                            IconButton(
                                onClick = { viewModel.showLangCurrencyDialog.value = true },
                                modifier = Modifier.testTag("lang_currency_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = ErpNavyPrimary,
                            titleContentColor = Color.White,
                            actionIconContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    // Modern Multi-Tab Bottom Navigation Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = listOf(
                                TabItem(ErpTab.DASHBOARD, ErpStrings.get("tab_dashboard", language), Icons.Default.BarChart),
                                TabItem(ErpTab.INVENTORY, ErpStrings.get("tab_inventory", language), Icons.Default.Inventory2, badgeCount = lowStockProducts.size),
                                TabItem(ErpTab.POS, ErpStrings.get("tab_pos", language), Icons.Default.PointOfSale, badgeCount = cart.values.sum()),
                                TabItem(ErpTab.TRENDYOL, ErpStrings.get("tab_trendyol", language), Icons.Default.Storefront),
                                TabItem(ErpTab.SCANNER, ErpStrings.get("tab_scanner", language), Icons.Default.QrCodeScanner),
                                TabItem(ErpTab.STAFF, ErpStrings.get("tab_staff", language), Icons.Default.Badge),
                                TabItem(ErpTab.SUPPORT_BACKUP, ErpStrings.get("tab_support_backup", language), Icons.Default.SupportAgent)
                            )

                            tabs.forEach { tab ->
                                val isSelected = currentTab == tab.tab
                                Surface(
                                    color = if (isSelected) ErpNavyPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clickable { viewModel.setTab(tab.tab) }
                                        .testTag("tab_${tab.tab.name}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BadgedBox(
                                            badge = {
                                                if (tab.badgeCount > 0) {
                                                    Badge(
                                                        containerColor = if (tab.tab == ErpTab.INVENTORY) ErpDanger else ErpTealSecondary
                                                    ) {
                                                        Text("${tab.badgeCount}", color = Color.White, fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = tab.icon,
                                                contentDescription = tab.title,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = tab.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        ErpTab.DASHBOARD -> DashboardScreen(
                            products = products,
                            lowStockProducts = lowStockProducts,
                            warehouses = warehouses,
                            orders = orders,
                            currency = currency,
                            language = language,
                            onNavigateToInventory = { viewModel.setTab(ErpTab.INVENTORY) },
                            onNavigateToTrendyol = { viewModel.setTab(ErpTab.TRENDYOL) },
                            onNavigateToPos = { viewModel.setTab(ErpTab.POS) }
                        )

                        ErpTab.INVENTORY -> InventoryScreen(
                            products = products,
                            warehouses = warehouses,
                            currency = currency,
                            language = language,
                            onAddProductClick = { viewModel.showAddProductDialog.value = true },
                            onAdjustStockClick = { prod -> viewModel.editingProductStock.value = prod },
                            onScanClick = { viewModel.setTab(ErpTab.SCANNER) }
                        )

                        ErpTab.POS -> SalesPosScreen(
                            products = products,
                            cart = cart,
                            currency = currency,
                            language = language,
                            onAddToCart = { prod -> viewModel.addToCart(prod) },
                            onRemoveFromCart = { prod -> viewModel.removeFromCart(prod) },
                            onUpdateQty = { prod, qty -> viewModel.updateCartQty(prod, qty) },
                            onClearCart = { viewModel.clearCart() },
                            onOpenCheckout = { viewModel.showPaymentDialog.value = true },
                            onOpenScanner = { viewModel.setTab(ErpTab.SCANNER) }
                        )

                        ErpTab.TRENDYOL -> TrendyolScreen(
                            config = trendyolConfig,
                            logs = trendyolLogs,
                            isSyncing = isSyncing,
                            language = language,
                            onSyncNow = { viewModel.syncWithTrendyol() },
                            onImportOrders = { viewModel.importTrendyolOrders() },
                            onUpdateConfig = { sId, key, interval -> viewModel.updateTrendyolConfig(sId, key, interval) }
                        )

                        ErpTab.SCANNER -> BarcodeScannerScreen(
                            scannedProduct = lastScannedProduct,
                            productsList = products,
                            currency = currency,
                            language = language,
                            onBarcodeScanned = { code -> viewModel.scanBarcode(code) },
                            onAddToCart = { prod ->
                                viewModel.addToCart(prod)
                                viewModel.showMessage("تمت إضافة ${prod.name} لسلة البيع")
                            },
                            onRapidCountIncrement = { pId -> viewModel.rapidStockCountIncrement(pId) }
                        )

                        ErpTab.STAFF -> StaffDashboardScreen(
                            employees = employees,
                            currency = currency,
                            language = language,
                            onToggleShift = { emp -> viewModel.toggleEmployeeShift(emp) }
                        )

                        ErpTab.SUPPORT_BACKUP -> SupportAndBackupScreen(
                            tickets = tickets,
                            backups = backups,
                            language = language,
                            onCreateBackup = { viewModel.createEncryptedBackup() },
                            onOpenNewTicket = { viewModel.showNewTicketDialog.value = true },
                            onResolveTicket = { t -> viewModel.resolveTicket(t) }
                        )
                    }
                }
            }

            // Dialogs
            if (showPaymentDialog) {
                PaymentDialog(
                    cart = cart,
                    currency = currency,
                    language = language,
                    onDismiss = { viewModel.showPaymentDialog.value = false },
                    onConfirmPayment = { cName, cPhone, method, cashier ->
                        viewModel.processCheckout(cName, cPhone, method, cashier)
                    }
                )
            }

            lastCompletedOrder?.let { order ->
                OrderReceiptDialog(
                    order = order,
                    currency = currency,
                    language = language,
                    onDismiss = { viewModel.lastCompletedOrder.value = null }
                )
            }

            if (showAddProductDialog) {
                AddProductDialog(
                    warehouses = warehouses,
                    language = language,
                    onDismiss = { viewModel.showAddProductDialog.value = false },
                    onConfirm = { name, nameEn, barcode, sku, cat, wId, qty, minAlert, cost, sell, tyBarcode ->
                        viewModel.addNewProduct(name, nameEn, barcode, sku, cat, wId, qty, minAlert, cost, sell, tyBarcode)
                    }
                )
            }

            editingStockProduct?.let { prod ->
                AdjustStockDialog(
                    product = prod,
                    language = language,
                    onDismiss = { viewModel.editingProductStock.value = null },
                    onConfirm = { newStock -> viewModel.updateProductStock(prod.id, newStock) }
                )
            }

            if (showLangCurrencyDialog) {
                LanguageCurrencyDialog(
                    currentLanguage = language,
                    currentCurrency = currency,
                    onDismiss = { viewModel.showLangCurrencyDialog.value = false },
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onSelectCurrency = { curr -> viewModel.setCurrency(curr) }
                )
            }

            if (showNewTicketDialog) {
                NewTicketDialog(
                    language = language,
                    onDismiss = { viewModel.showNewTicketDialog.value = false },
                    onConfirm = { name, contact, subj, msg, priority ->
                        viewModel.createSupportTicket(name, contact, subj, msg, priority)
                    }
                )
            }
        }
    }
}

data class TabItem(
    val tab: ErpTab,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)
