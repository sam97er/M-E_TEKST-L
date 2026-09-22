package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoreSettingsEntity
import com.example.ui.SyncUiState
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TrendyolBlue
import com.example.ui.theme.TrendyolErrorRed
import com.example.ui.theme.TrendyolNavy
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark
import com.example.ui.theme.TrendyolOrangeLight
import com.example.ui.theme.TrendyolSuccessGreen
import com.example.ui.theme.TrendyolWarningYellow

@Composable
fun SettingsScreen(
    settings: StoreSettingsEntity?,
    syncState: SyncUiState = SyncUiState.Idle,
    onSaveSettings: (
        storeName: String,
        supplierId: String,
        apiKey: String,
        apiSecret: String,
        isLiveMode: Boolean,
        geminiApiKey: String,
        telegramBotToken: String,
        telegramChatId: String,
        isTelegramEnabled: Boolean,
        notifyNewOrders: Boolean,
        notifyCustomerQuestions: Boolean,
        notifyLowStock: Boolean,
        notifyDailyReport: Boolean,
        notifyAiSlowMoving: Boolean
    ) -> Unit,
    onSyncNow: (String, String, String) -> Unit = { _, _, _ -> },
    onTestConnection: (String, String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _, _ -> },
    onTestTelegram: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onSendTelegramDailyReport: ((Boolean, String) -> Unit) -> Unit = {},
    onSendTelegramSlowMovingAdvice: ((Boolean, String) -> Unit) -> Unit = {},
    onSendTelegramLowStockAlert: ((Boolean, String) -> Unit) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var storeName by remember(settings) { mutableStateOf(settings?.storeName ?: "Trendyol Partner Store") }
    var supplierId by remember(settings) { mutableStateOf(settings?.supplierId ?: "") }
    var apiKey by remember(settings) { mutableStateOf(settings?.apiKey ?: "") }
    var apiSecret by remember(settings) { mutableStateOf(settings?.apiSecret ?: "") }
    var geminiApiKey by remember(settings) { mutableStateOf(settings?.geminiApiKey ?: "") }
    var isLiveMode by remember(settings) { mutableStateOf(settings?.isLiveMode ?: true) }

    // Telegram Bot State
    var telegramBotToken by remember(settings) { mutableStateOf(settings?.telegramBotToken ?: "") }
    var telegramChatId by remember(settings) { mutableStateOf(settings?.telegramChatId ?: "") }
    var isTelegramEnabled by remember(settings) { mutableStateOf(settings?.isTelegramEnabled ?: true) }
    var notifyNewOrders by remember(settings) { mutableStateOf(settings?.notifyNewOrders ?: true) }
    var notifyCustomerQuestions by remember(settings) { mutableStateOf(settings?.notifyCustomerQuestions ?: true) }
    var notifyLowStock by remember(settings) { mutableStateOf(settings?.notifyLowStock ?: true) }
    var notifyDailyReport by remember(settings) { mutableStateOf(settings?.notifyDailyReport ?: true) }
    var notifyAiSlowMoving by remember(settings) { mutableStateOf(settings?.notifyAiSlowMoving ?: true) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var isTestingTelegram by remember { mutableStateOf(false) }
    var isSendingTelegramReport by remember { mutableStateOf(false) }
    var isSendingTelegramAiAdvice by remember { mutableStateOf(false) }
    var isSendingTelegramLowStock by remember { mutableStateOf(false) }

    fun doSave() {
        onSaveSettings(
            storeName,
            supplierId,
            apiKey,
            apiSecret,
            isLiveMode,
            geminiApiKey,
            telegramBotToken,
            telegramChatId,
            isTelegramEnabled,
            notifyNewOrders,
            notifyCustomerQuestions,
            notifyLowStock,
            notifyDailyReport,
            notifyAiSlowMoving
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "إعدادات الربط والمزامنة والبوتات الذكية",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "إدارة ربط متجر ترنديول، بوت تيليجرام للإشعارات الفورية، ومحرك الذكاء الاصطناعي.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )
        }

        // 1. TELEGRAM BOT INTEGRATION CARD (FULL SUITE)
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("telegram_bot_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Telegram Header with Active Status Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(TrendyolBlue.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = TrendyolBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "بوت تيليجرام المتكامل (Telegram Bot)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "إشعارات لحظية للطلبات، الأسئلة، المخزون، والتقارير",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Switch(
                            checked = isTelegramEnabled,
                            onCheckedChange = { isTelegramEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TrendyolBlue
                            )
                        )
                    }

                    HorizontalDivider(color = Slate200, thickness = 1.dp)

                    // Telegram Guide Box
                    Surface(
                        color = TrendyolBlue.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "💡 كيفية إنشاء وربط البوت خلال دقيقة:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TrendyolBlue
                            )
                            Text(
                                text = "1. افتح تيليجرام وابحث عن <b>@BotFather</b> وأرسل <code>/newbot</code> لإنشاء بوتك ونسخ الـ <b>API Token</b>.\n2. ابحث عن <b>@userinfobot</b> أو <b>@RawDataBot</b> لمعرفة رقم <b>Chat ID</b> الخاص بك.\n3. أرسل رسالة <code>/start</code> لبوتك الجديد، ثم أدخل البيانات أدناه واضغط اختبار الاتصال.",
                                fontSize = 10.sp,
                                color = Slate700,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    // Bot Token Field
                    OutlinedTextField(
                        value = telegramBotToken,
                        onValueChange = { telegramBotToken = it },
                        label = { Text("رمز التوكن للبوت (Bot API Token)") },
                        placeholder = { Text("مثال: 7123456789:AAHKq...") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TrendyolBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Chat ID Field
                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = { Text("معرّف المحادثة (Telegram Chat ID)") },
                        placeholder = { Text("مثال: 987654321 أو -10012345678") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = TrendyolBlue)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    // Test Telegram Button
                    OutlinedButton(
                        onClick = {
                            isTestingTelegram = true
                            doSave()
                            onTestTelegram(telegramBotToken, telegramChatId) { success, msg ->
                                isTestingTelegram = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isTestingTelegram && telegramBotToken.isNotBlank() && telegramChatId.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TrendyolBlue)
                    ) {
                        if (isTestingTelegram) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TrendyolBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "جاري إرسال رسالة الاختبار...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "🧪 اختبار ربط البوت (إرسال رسالة تجريبية)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Notification Types Toggles
                    Text(
                        text = "🔔 أنواع الإشعارات التلقائية للبوت:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 1. New Order Toggle
                    NotificationToggleItem(
                        title = "طلب Trendyol جديد (Yeni Sipariş)",
                        description = "إشعار فوري برقم الطلب، اسم المشتري، والمدينة والمبلغ وتفاصيل الأصناف",
                        icon = Icons.Default.ShoppingBag,
                        iconColor = TrendyolOrange,
                        isChecked = notifyNewOrders,
                        onCheckedChange = { notifyNewOrders = it }
                    )

                    // 2. Customer Question Toggle
                    NotificationToggleItem(
                        title = "سؤال عميل جديد (Müşteri Sorusu)",
                        description = "تنبيه لحظي عند طرح المشتري سؤالاً مع نص السؤال واسم المنتج",
                        icon = Icons.Default.ChatBubbleOutline,
                        iconColor = TrendyolNavy,
                        isChecked = notifyCustomerQuestions,
                        onCheckedChange = { notifyCustomerQuestions = it }
                    )

                    // 3. Low Stock Toggle
                    NotificationToggleItem(
                        title = "تنبيه انخفاض المخزون (Kritik Stok)",
                        description = "تنبيه بالمنتجات التي قارب مخزونها على النفاد لمنع إيقاف مبيعاتها",
                        icon = Icons.Default.Warning,
                        iconColor = TrendyolErrorRed,
                        isChecked = notifyLowStock,
                        onCheckedChange = { notifyLowStock = it }
                    )

                    // 4. Daily Sales Report Toggle
                    NotificationToggleItem(
                        title = "تقرير مبيعات يومي (Günlük Rapor)",
                        description = "ملخص يومي شامل للإيرادات والطلبات والشحنات وتقييم المتجر",
                        icon = Icons.Default.Assessment,
                        iconColor = TrendyolSuccessGreen,
                        isChecked = notifyDailyReport,
                        onCheckedChange = { notifyDailyReport = it }
                    )

                    // 5. AI Slow-Moving Products Advice Toggle
                    NotificationToggleItem(
                        title = "اقتراحات AI للمنتجات قليلة المبيعات",
                        description = "توصيات ذكية بالأسعار والترويج لتحريك مبيعات المنتجات الراكدة",
                        icon = Icons.Default.AutoAwesome,
                        iconColor = TrendyolOrangeDark,
                        isChecked = notifyAiSlowMoving,
                        onCheckedChange = { notifyAiSlowMoving = it }
                    )

                    HorizontalDivider(color = Slate200, thickness = 1.dp)

                    // Quick Send Actions on Demand
                    Text(
                        text = "⚡ إرسال فوري إلى تيليجرام الآن بنقرة واحدة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Slate700
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isSendingTelegramReport = true
                                doSave()
                                onSendTelegramDailyReport { success, msg ->
                                    isSendingTelegramReport = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSendingTelegramReport && telegramBotToken.isNotBlank() && telegramChatId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolSuccessGreen),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (isSendingTelegramReport) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "تقرير المبيعات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                isSendingTelegramAiAdvice = true
                                doSave()
                                onSendTelegramSlowMovingAdvice { success, msg ->
                                    isSendingTelegramAiAdvice = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSendingTelegramAiAdvice && telegramBotToken.isNotBlank() && telegramChatId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolNavy),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (isSendingTelegramAiAdvice) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "اقتراحات AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                isSendingTelegramLowStock = true
                                doSave()
                                onSendTelegramLowStockAlert { success, msg ->
                                    isSendingTelegramLowStock = false
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            enabled = !isSendingTelegramLowStock && telegramBotToken.isNotBlank() && telegramChatId.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolWarningYellow),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            if (isSendingTelegramLowStock) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "تنبيه المخزون", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. TRENDYOL API SYNC STATUS CARD
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_status_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (syncState is SyncUiState.Success) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = if (syncState is SyncUiState.Success) TrendyolSuccessGreen else TrendyolOrange
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حالة الاتصال والمزامنة مع Trendyol",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (syncState is SyncUiState.Syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        }
                    }

                    when (syncState) {
                        is SyncUiState.Syncing -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = TrendyolOrange
                                )
                                Text(text = syncState.message, fontSize = 12.sp, color = Slate500)
                            }
                        }

                        is SyncUiState.Success -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "✅ ${syncState.message}",
                                    fontSize = 12.sp,
                                    color = TrendyolSuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "• ${syncState.productsCount} منتج تم تحديثها | ${syncState.ordersCount} طلب جديد | ${syncState.questionsCount} سؤال مشتري",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                                if (syncState.productsCount == 0 || syncState.questionsCount == 0) {
                                    Surface(
                                        color = TrendyolOrange.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "💡 ميزة المحاكاة الذكية مفعلة تلقائياً:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrendyolOrangeDark
                                            )
                                            Text(
                                                text = "تم الحفاظ على المنتجات والأسئلة التفاعلية (مع صورها) لكي تستمر في تجربة لوحة التحكم والذكاء الاصطناعي ومميزات بوت تيليجرام بالكامل، ريثما تقوم بتفعيل صلاحيات المنتجات والأسئلة (Ürün ve Soru Entegrasyonu) في حسابك على Trendyol Partner.",
                                                fontSize = 10.sp,
                                                color = Slate700,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is SyncUiState.Error -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "⚠️ ${syncState.errorMessage}",
                                    fontSize = 12.sp,
                                    color = TrendyolErrorRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (syncState.errorMessage.contains("403") || syncState.errorMessage.contains("صلاحيات")) {
                                    Surface(
                                        color = TrendyolErrorRed.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "💡 كيفية حل خطأ 403 في ترنديول:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrendyolErrorRed
                                            )
                                            Text(
                                                text = "1. افتح partner.trendyol.com ⬅ Hesabım ⬅ Entegrasyon Bilgileri.\n2. تأكد من أن الصلاحيات مفعلة بالكامل (Tüm Yetkiler).\n3. تأكد من عدم تفعيل تقييد الآي بي (IP Kısıtlaması).\n4. تأكد من عدم وجود مسافات فارغة في بداية أو نهاية المفاتيح.",
                                                fontSize = 10.sp,
                                                color = Slate700,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SyncUiState.Idle -> {
                            Text(
                                text = "المزامنة جاهزة. اضغط زر المزامنة لجلب أحدث البيانات من متجرك.",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    Button(
                        onClick = { onSyncNow(supplierId, apiKey, apiSecret) },
                        enabled = syncState !is SyncUiState.Syncing && supplierId.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "مزامنة البيانات الآن (Sync Store Now)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. TRENDYOL CREDENTIALS FORM CARD
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "بيانات الربط مع متجر Trendyol", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("اسم المتجر (Mağaza Adı)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = TrendyolOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = supplierId,
                        onValueChange = { supplierId = it },
                        label = { Text("رقم المورد / البائع (Satıcı / Supplier ID)") },
                        placeholder = { Text("مثال: 842910") },
                        leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TrendyolOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("مفتاح واجهة برمجة التطبيقات (API Key)") },
                        leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = TrendyolOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = apiSecret,
                        onValueChange = { apiSecret = it },
                        label = { Text("المفتاح السري (API Secret)") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = TrendyolOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Test API Button
                    OutlinedButton(
                        onClick = {
                            isTestingConnection = true
                            onTestConnection(supplierId, apiKey, apiSecret) { success, msg ->
                                isTestingConnection = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isTestingConnection && supplierId.isNotBlank() && apiKey.isNotBlank() && apiSecret.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "جاري فحص الاتصال بـ Trendyol...", fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = TrendyolSuccessGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "اختبار الاتصال بـ API ترنديول", color = TrendyolOrangeDark, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 4. AI ENGINE CONFIGURATION CARD
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TrendyolOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "إعدادات الذكاء الاصطناعي (Gemini AI Key)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "لتفعيل الرد الآلي على استفسارات المشترين، وتحسين الباي بوكس، وتوليد بطاقات المنتجات وتوصيات تيليجرام:",
                        fontSize = 12.sp,
                        color = Slate700
                    )

                    OutlinedTextField(
                        value = geminiApiKey,
                        onValueChange = { geminiApiKey = it },
                        label = { Text("مفتاح Gemini API Key (اختياري)") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TrendyolOrange) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Text(
                        text = "• الموديل: Google Gemini 3.5 Flash مع Function Calling\n• الحالة: ${if (geminiApiKey.isNotBlank()) "مفتاح مخصص مفعل ✓" else "المحرك الذكي الداخلي مفعل"}",
                        fontSize = 11.sp,
                        color = if (geminiApiKey.isNotBlank()) TrendyolSuccessGreen else Slate500
                    )
                }
            }
        }

        // 5. MASTER SAVE BUTTON
        item {
            Button(
                onClick = {
                    doSave()
                    Toast.makeText(context, "تم حفظ كافة الإعدادات وربط البوت ومزامنة متجر ترنديول...", Toast.LENGTH_SHORT).show()
                },
                enabled = syncState !is SyncUiState.Syncing,
                colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (syncState is SyncUiState.Syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "جاري المزامنة مع ترنديول...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "حفظ كافة الإعدادات والبدء", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = description, fontSize = 10.sp, color = Slate500, lineHeight = 13.sp)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = iconColor
                )
            )
        }
    }
}
