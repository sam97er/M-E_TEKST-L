package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.orders.OrdersScreen
import com.example.ui.orders.OrdersViewModel
import com.example.ui.products.ProductsScreen
import com.example.ui.products.ProductsViewModel
import com.example.ui.questions.QuestionsScreen
import com.example.ui.questions.QuestionsViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.telegram.TelegramScreen
import com.example.ui.telegram.TelegramViewModel
import com.example.ui.trendyol.TrendyolScreen
import com.example.ui.trendyol.TrendyolViewModel
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val productsViewModel: ProductsViewModel by viewModels()
    private val trendyolViewModel: TrendyolViewModel by viewModels()
    private val questionsViewModel: QuestionsViewModel by viewModels()
    private val ordersViewModel: OrdersViewModel by viewModels()
    private val telegramViewModel: TelegramViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScaffold(
                    dashboardViewModel = dashboardViewModel,
                    settingsViewModel = settingsViewModel,
                    productsViewModel = productsViewModel,
                    trendyolViewModel = trendyolViewModel,
                    questionsViewModel = questionsViewModel,
                    ordersViewModel = ordersViewModel,
                    telegramViewModel = telegramViewModel,
                    reportsViewModel = reportsViewModel
                )
            }
        }
    }
}

enum class NavigationSection(val title: String, val icon: ImageVector) {
    DASHBOARD("Genel Bakış", Icons.Outlined.Dashboard),
    ORDERS("Sipariş & Kâr", Icons.Outlined.MonetizationOn),
    QUESTIONS("Sorular", Icons.Outlined.QuestionAnswer),
    REPORTS("AI Rapor", Icons.Outlined.AutoGraph),
    PRODUCTS("Ürünler", Icons.Outlined.Inventory2),
    TRENDYOL("Trendyol", Icons.Outlined.Storefront),
    TELEGRAM("Telegram", Icons.Outlined.Notifications),
    SETTINGS("Ayarlar", Icons.Outlined.Settings)
}

@Composable
fun MainAppScaffold(
    dashboardViewModel: DashboardViewModel,
    settingsViewModel: SettingsViewModel,
    productsViewModel: ProductsViewModel,
    trendyolViewModel: TrendyolViewModel,
    questionsViewModel: QuestionsViewModel,
    ordersViewModel: OrdersViewModel,
    telegramViewModel: TelegramViewModel,
    reportsViewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    var currentSection by remember { mutableStateOf(NavigationSection.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceCard,
                tonalElevation = 8.dp
            ) {
                NavigationSection.values().forEach { section ->
                    NavigationBarItem(
                        selected = currentSection == section,
                        onClick = { currentSection = section },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title
                            )
                        },
                        label = {
                            Text(
                                text = section.title,
                                fontWeight = if (currentSection == section) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                fontSize = 9.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryOrange,
                            selectedTextColor = PrimaryOrange,
                            indicatorColor = PrimaryOrange.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_${section.name.lowercase()}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentSection) {
            NavigationSection.DASHBOARD -> {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToSection = { target ->
                        when (target.uppercase()) {
                            "QUESTIONS" -> currentSection = NavigationSection.QUESTIONS
                            "ORDERS" -> currentSection = NavigationSection.ORDERS
                            "REPORTS" -> currentSection = NavigationSection.REPORTS
                            "PRODUCTS" -> currentSection = NavigationSection.PRODUCTS
                            "TRENDYOL" -> currentSection = NavigationSection.TRENDYOL
                            "TELEGRAM" -> currentSection = NavigationSection.TELEGRAM
                            "SETTINGS" -> currentSection = NavigationSection.SETTINGS
                            "AUDIT" -> currentSection = NavigationSection.TELEGRAM
                        }
                    }
                )
            }
            NavigationSection.ORDERS -> {
                OrdersScreen(
                    viewModel = ordersViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.REPORTS -> {
                ReportsScreen(
                    viewModel = reportsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.QUESTIONS -> {
                QuestionsScreen(
                    viewModel = questionsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.PRODUCTS -> {
                ProductsScreen(
                    viewModel = productsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.TRENDYOL -> {
                TrendyolScreen(
                    viewModel = trendyolViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.TELEGRAM -> {
                TelegramScreen(
                    viewModel = telegramViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationSection.SETTINGS -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/**
 * Kept for Robolectric screenshot and compatibility tests.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
