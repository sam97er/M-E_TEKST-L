package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.MAndETekstilRepository
import com.example.ui.screens.approvals.ApprovalsScreen
import com.example.ui.screens.automations.AutomationsScreen
import com.example.ui.screens.automations.TelegramScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.growth.GrowthScreen
import com.example.ui.screens.hub.ToolsHubScreen
import com.example.ui.screens.orders.OrdersScreen
import com.example.ui.screens.profit.ProfitScreen
import com.example.ui.screens.questions.QuestionsScreen
import com.example.ui.screens.reports.DailyBriefScreen
import com.example.ui.screens.reports.ReturnsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.stock.StockScreen
import com.example.ui.screens.sync.SyncCenterScreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.TrendyolOrange

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Özet", Icons.Default.Dashboard)
    object Orders : Screen("orders", "Siparişler", Icons.Default.ShoppingBag)
    object Stock : Screen("stock", "Stok AI", Icons.Default.Inventory2)
    object Questions : Screen("questions", "Sorular AI", Icons.Default.QuestionAnswer)
    object Hub : Screen("hub", "Araçlar", Icons.Default.Apps)

    // Deep modules
    object Profit : Screen("profit", "Kâr Hesaplayıcı")
    object Growth : Screen("growth", "Büyüme & Kalite AI")
    object Approvals : Screen("approvals", "AI Onay Merkezi")
    object DailyBrief : Screen("daily_brief", "Günlük Özet")
    object Returns : Screen("returns", "İade Analizi")
    object Automations : Screen("automations", "Otomasyonlar")
    object Telegram : Screen("telegram", "Telegram")
    object Settings : Screen("settings", "Ayarlar")
    object SyncCenter : Screen("sync_center", "Senkronizasyon Merkezi")
}

@Composable
fun MainAppNavigation(repository: MAndETekstilRepository) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        repository.seedInitialData()
    }

    val bottomNavItems = listOf(
        Screen.Dashboard,
        Screen.Orders,
        Screen.Stock,
        Screen.Questions,
        Screen.Hub
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            screen.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = screen.title
                                )
                            }
                        },
                        label = { Text(screen.title, fontSize = 11.sp) },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TrendyolOrange,
                            selectedTextColor = TrendyolOrange,
                            indicatorColor = TrendyolOrange.copy(alpha = 0.12f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    repository = repository,
                    onNavigateToOrders = { navController.navigate(Screen.Orders.route) },
                    onNavigateToStock = { navController.navigate(Screen.Stock.route) },
                    onNavigateToQuestions = { navController.navigate(Screen.Questions.route) },
                    onNavigateToProfit = { navController.navigate(Screen.Profit.route) },
                    onNavigateToDailyBrief = { navController.navigate(Screen.DailyBrief.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) },
                    onNavigateToSyncCenter = { navController.navigate(Screen.SyncCenter.route) },
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Screen.Orders.route) {
                OrdersScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Stock.route) {
                StockScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Questions.route) {
                QuestionsScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Hub.route) {
                ToolsHubScreen(
                    repository = repository,
                    onNavigateToProfit = { navController.navigate(Screen.Profit.route) },
                    onNavigateToGrowth = { navController.navigate(Screen.Growth.route) },
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) },
                    onNavigateToDailyBrief = { navController.navigate(Screen.DailyBrief.route) },
                    onNavigateToReturns = { navController.navigate(Screen.Returns.route) },
                    onNavigateToAutomations = { navController.navigate(Screen.Automations.route) },
                    onNavigateToTelegram = { navController.navigate(Screen.Telegram.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToSyncCenter = { navController.navigate(Screen.SyncCenter.route) }
                )
            }
            composable(Screen.Profit.route) {
                ProfitScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Growth.route) {
                GrowthScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Approvals.route) {
                ApprovalsScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.DailyBrief.route) {
                DailyBriefScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Returns.route) {
                ReturnsScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Automations.route) {
                AutomationsScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Telegram.route) {
                TelegramScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
            composable(Screen.SyncCenter.route) {
                SyncCenterScreen(repository = repository, snackbarHostState = snackbarHostState)
            }
        }
    }
}
