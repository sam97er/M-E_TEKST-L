package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ScreenTab
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeLight

@Composable
fun TrendyolBottomNav(
    currentTab: ScreenTab,
    onTabSelected: (ScreenTab) -> Unit,
    pendingQuestionsCount: Int,
    pendingOrdersCount: Int,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("trendyol_bottom_nav"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // 1. Dashboard
        NavigationBarItem(
            selected = currentTab == ScreenTab.DASHBOARD,
            onClick = { onTabSelected(ScreenTab.DASHBOARD) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                    contentDescription = "الرئيسية"
                )
            },
            label = {
                Text(
                    text = "الرئيسية",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == ScreenTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TrendyolOrange,
                selectedTextColor = TrendyolOrange,
                indicatorColor = TrendyolOrangeLight
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
        )

        // 2. Orders
        NavigationBarItem(
            selected = currentTab == ScreenTab.ORDERS,
            onClick = { onTabSelected(ScreenTab.ORDERS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (pendingOrdersCount > 0) {
                            Badge(
                                containerColor = TrendyolOrange,
                                contentColor = Color.White
                            ) {
                                Text(text = "$pendingOrdersCount", fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == ScreenTab.ORDERS) Icons.Filled.LocalShipping else Icons.Outlined.LocalShipping,
                        contentDescription = "الطلبات"
                    )
                }
            },
            label = {
                Text(
                    text = "الطلبات",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == ScreenTab.ORDERS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TrendyolOrange,
                selectedTextColor = TrendyolOrange,
                indicatorColor = TrendyolOrangeLight
            ),
            modifier = Modifier.testTag("nav_tab_orders")
        )

        // 3. Products
        NavigationBarItem(
            selected = currentTab == ScreenTab.PRODUCTS,
            onClick = { onTabSelected(ScreenTab.PRODUCTS) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.PRODUCTS) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                    contentDescription = "المنتجات"
                )
            },
            label = {
                Text(
                    text = "المنتجات",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == ScreenTab.PRODUCTS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TrendyolOrange,
                selectedTextColor = TrendyolOrange,
                indicatorColor = TrendyolOrangeLight
            ),
            modifier = Modifier.testTag("nav_tab_products")
        )

        // 4. Questions
        NavigationBarItem(
            selected = currentTab == ScreenTab.QUESTIONS,
            onClick = { onTabSelected(ScreenTab.QUESTIONS) },
            icon = {
                BadgedBox(
                    badge = {
                        if (pendingQuestionsCount > 0) {
                            Badge(
                                containerColor = TrendyolOrange,
                                contentColor = Color.White
                            ) {
                                Text(text = "$pendingQuestionsCount", fontSize = 10.sp)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (currentTab == ScreenTab.QUESTIONS) Icons.Filled.ChatBubbleOutline else Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "الأسئلة"
                    )
                }
            },
            label = {
                Text(
                    text = "الأسئلة",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == ScreenTab.QUESTIONS) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TrendyolOrange,
                selectedTextColor = TrendyolOrange,
                indicatorColor = TrendyolOrangeLight
            ),
            modifier = Modifier.testTag("nav_tab_questions")
        )

        // 5. AI Studio
        NavigationBarItem(
            selected = currentTab == ScreenTab.AI_STUDIO,
            onClick = { onTabSelected(ScreenTab.AI_STUDIO) },
            icon = {
                Icon(
                    imageVector = if (currentTab == ScreenTab.AI_STUDIO) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                    contentDescription = "الذكاء الاصطناعي"
                )
            },
            label = {
                Text(
                    text = "ذكاء المتجر",
                    fontSize = 11.sp,
                    fontWeight = if (currentTab == ScreenTab.AI_STUDIO) FontWeight.Bold else FontWeight.Normal
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = TrendyolOrange,
                selectedTextColor = TrendyolOrange,
                indicatorColor = TrendyolOrangeLight
            ),
            modifier = Modifier.testTag("nav_tab_ai_studio")
        )
    }
}
