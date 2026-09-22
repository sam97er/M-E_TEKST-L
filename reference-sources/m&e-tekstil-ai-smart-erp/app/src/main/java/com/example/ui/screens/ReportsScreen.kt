package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReportsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "التقارير الإدارية (Telegram Bot Feed)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "ملخصات يومية سريعة ومباشرة لإرسالها عبر Telegram bot",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                TelegramReportCard(
                    title = "تقرير اليوم - M&E Tekstil ERP",
                    date = "19 سبتمبر 2026",
                    content = """
                        📊 ملخص المبيعات: 24 طلباً اليوم (أداء ممتاز في قطاع الكروب توب والبناطيل العريضة).
                        
                        ⚠️ تنبيه المخزون: لون Pudra Pembesi (كروب توب مقاس M) نفد تماماً. أرجو تحديث ملف stock.xlsx وتجهيز طلبية جديدة من المصنع لتجنب خسارة ترتيب المنتج في ترنديول.
                        
                        💰 الأرباح وصندوق الشراء: هامش الربح الصافي مستقر عند 32%. جميع الأسعار تنافسية وتحافظ على الـ Buybox.
                        
                        💬 خدمة العملاء: تم الرد على جميع استفسارات العملاء باللغة التركية بخصوص جودة الأقمشة (%100 pamuklu) والراحة.
                        
                        🚀 الإجراء المطلوب: الموافقة على طلبية التصنيع الجديدة للألوان المميزة M&E Pudra Pembesi و Gül Kurusu.
                    """.trimIndent()
                )
            }
            item {
                TelegramReportCard(
                    title = "تقرير الأمس",
                    date = "18 سبتمبر 2026",
                    content = """
                        📊 ملخص المبيعات: 19 طلباً.
                        ⚠️ تنبيه المخزون: الرفوف آمنة، متبقي 3 قطع Seamless باللون Gül Kurusu. تم اقتراح رفع السعر 10% لإبطاء الوتيرة.
                        💰 الأرباح: 4,850 TRY صافي ربح اليوم.
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun TelegramReportCard(title: String, date: String, content: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {},
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("إرسال عبر Telegram", fontSize = 12.sp)
            }
        }
    }
}
