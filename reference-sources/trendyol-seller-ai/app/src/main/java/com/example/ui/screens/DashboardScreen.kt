package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.QuestionEntity
import com.example.data.repository.DashboardMetrics
import com.example.ui.ScreenTab
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TrendyolBlue
import com.example.ui.theme.TrendyolErrorRed
import com.example.ui.theme.TrendyolNavy
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark
import com.example.ui.theme.TrendyolOrangeGradientEnd
import com.example.ui.theme.TrendyolOrangeGradientStart
import com.example.ui.theme.TrendyolOrangeLight
import com.example.ui.theme.TrendyolSuccessGreen
import com.example.ui.theme.TrendyolWarningYellow

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    products: List<ProductEntity>,
    orders: List<OrderEntity>,
    questions: List<QuestionEntity>,
    onNavigate: (ScreenTab) -> Unit,
    onRestockClick: (ProductEntity) -> Unit,
    onAnswerQuestionClick: (QuestionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val lowStockProducts = products.filter { it.stockCount < 5 && it.isActive }
    val unansweredQuestions = questions.filter { !it.isAnswered }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Banner with AI Copilot Greeting
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_banner_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Header background illustration
                    Image(
                        painter = painterResource(id = R.drawable.ic_trendyol_banner),
                        contentDescription = "Trendyol Dashboard Banner",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient overlay for smooth readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )

                    // Text overlay on banner
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TrendyolOrangeGradientStart,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مساعد ترنديول الذكي متصل ومفعل",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "تحكم متكامل في مبيعاتك، مخزونك، والباي بوكس",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Sub-card quick action button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onNavigate(ScreenTab.AI_STUDIO) },
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dashboard_ai_advisor_btn")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "مستشار الذكاء الاصطناعي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { onNavigate(ScreenTab.SETTINGS) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dashboard_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = TrendyolOrange, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "الإعدادات والمزامنة", fontSize = 12.sp, color = TrendyolOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Financial & Orders Summary Grid
        item {
            Text(
                text = "ملخص الأداء والمبيعات (Satış Özeti)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "إجمالي المبيعات",
                    value = "${String.format("%.2f", metrics.totalRevenue)} ₺",
                    subtext = if (metrics.totalRevenue > 0) "إجمالي المبيعات المحققة" else "0.00 ₺",
                    icon = Icons.Default.Paid,
                    iconColor = TrendyolSuccessGreen,
                    backgroundColor = TrendyolSuccessGreen.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "طلبات اليوم",
                    value = "${metrics.todayOrders} طلب",
                    subtext = if (metrics.totalOrders > 0) "إجمالي: ${metrics.totalOrders} طلب" else "لا توجد طلبات جديدة",
                    icon = Icons.Default.ShoppingBag,
                    iconColor = TrendyolOrange,
                    backgroundColor = TrendyolOrangeLight,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "بانتظار الشحن",
                    value = "${metrics.pendingShipment} طرد",
                    subtext = if (metrics.pendingShipment > 0) "Trendyol Express" else "لا توجد طرود معلقة",
                    icon = Icons.Default.LocalShipping,
                    iconColor = TrendyolBlue,
                    backgroundColor = TrendyolBlue.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "المرتجعات",
                    value = "${metrics.returnedCount} طرد",
                    subtext = if (metrics.returnedCount > 0) "${metrics.returnedCount} مرتجع مسجل" else "لا توجد مرتجعات",
                    icon = Icons.Default.Warning,
                    iconColor = TrendyolWarningYellow,
                    backgroundColor = TrendyolWarningYellow.copy(alpha = 0.08f),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Trendyol Satıcı Puanı (Seller Quality Card)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تقييم المتجر في ترنديول (Satıcı Puanı)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "9.8 / 10",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = TrendyolOrangeDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { 0.98f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = TrendyolOrange,
                        trackColor = Slate200,
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScoreDetailItem(title = "سرعة الشحن", score = "9.9", isGood = true)
                        ScoreDetailItem(title = "رضا العملاء", score = "9.7", isGood = true)
                        ScoreDetailItem(title = "معدل الإلغاء", score = "%0.2", isGood = true)
                        ScoreDetailItem(title = "الباي بوكس", score = "%84", isGood = true)
                    }
                }
            }
        }

        // 4. Urgent Attention: Low Stock Products
        if (lowStockProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = TrendyolErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تنبيه المخزون المنخفض (Kritik Stok)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TrendyolErrorRed
                        )
                    }
                    Text(
                        text = "عرض الكل",
                        fontSize = 12.sp,
                        color = TrendyolOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate(ScreenTab.PRODUCTS) }
                    )
                }
            }

            items(lowStockProducts) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                                text = product.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الباركود: ${product.barcode} | المتبقي: ${product.stockCount} فقط",
                                color = if (product.stockCount == 0) TrendyolErrorRed else TrendyolWarningYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onRestockClick(product) },
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolNavy),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "+10 تزويد", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 5. Unanswered Questions with 1-Click AI Reply
        if (unansweredQuestions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ChatBubbleOutline, contentDescription = null, tint = TrendyolOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "أسئلة بانتظار الرد (Cevaplanmamış Sorular)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "عرض الأسئلة",
                        fontSize = 12.sp,
                        color = TrendyolOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate(ScreenTab.QUESTIONS) }
                    )
                }
            }

            items(unansweredQuestions.take(2)) { question ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${question.customerName} - بخصوص: ${question.productTitle}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${question.questionText}\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onAnswerQuestionClick(question) },
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrangeLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TrendyolOrangeDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "توليد رد ذكي وإرسال", color = TrendyolOrangeDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 6. Recent Orders Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أحدث الطلبات (Son Siparişler)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "جميع الطلبات (${orders.size})",
                    fontSize = 12.sp,
                    color = TrendyolOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigate(ScreenTab.ORDERS) }
                )
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TrendyolOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = TrendyolOrange, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "لا توجد طلبات واردة حالياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = "ستظهر الطلبات الجديدة هنا تلقائياً عند استلامها من متجر ترنديول.", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }
            }
        } else {
            items(orders.take(3)) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                OrderStatusBadge(status = order.status)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "${order.customerName} • ${order.customerCity}", fontSize = 11.sp, color = Slate500)
                            Text(text = order.itemsSummary, fontSize = 11.sp, maxLines = 1, color = Slate700)
                        }
                        Text(
                            text = "${String.format("%.2f", order.totalAmount)} ₺",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TrendyolOrangeDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(backgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, fontSize = 11.sp, color = Slate500)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtext, fontSize = 10.sp, color = iconColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ScoreDetailItem(title: String, score: String, isGood: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = Slate500)
        Text(
            text = score,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isGood) TrendyolSuccessGreen else TrendyolErrorRed
        )
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val (label, bg, fg) = when (status) {
        "Created" -> Triple("جديد", TrendyolOrangeLight, TrendyolOrangeDark)
        "Picking" -> Triple("قيد التجهيز", TrendyolBlue.copy(alpha = 0.15f), TrendyolBlue)
        "Shipped" -> Triple("بالشحن", TrendyolNavy.copy(alpha = 0.15f), TrendyolNavy)
        "Delivered" -> Triple("تم التسليم", TrendyolSuccessGreen.copy(alpha = 0.15f), TrendyolSuccessGreen)
        "Returned" -> Triple("مرتجع", TrendyolErrorRed.copy(alpha = 0.15f), TrendyolErrorRed)
        else -> Triple(status, Slate200, Slate700)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
