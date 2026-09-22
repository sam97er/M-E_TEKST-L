package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProductEntity
import com.example.data.remote.OptimizedProductDescription
import com.example.data.remote.ProductMetadata
import com.example.ui.AiChatMessage
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.TrendyolNavy
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark
import com.example.ui.theme.TrendyolOrangeLight
import com.example.ui.theme.TrendyolSuccessGreen

@Composable
fun AiStudioScreen(
    chatMessages: List<AiChatMessage>,
    isAiLoading: Boolean,
    aiOperationResult: String?,
    optimizedDescriptionResult: OptimizedProductDescription? = null,
    products: List<ProductEntity>,
    onSendMessage: (String) -> Unit,
    onGenerateListing: (String, String, String, String) -> Unit,
    onGenerateOptimizedDescription: ((ProductMetadata) -> Unit)? = null,
    onAnalyzeBuybox: (ProductEntity) -> Unit,
    onClearResult: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val tabs = listOf("وكيل ترنديول (Agent)", "توليد وصف متطور للمنتجات", "تحليل الباي بوكس")

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_studio_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TrendyolOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                    color = TrendyolOrange
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = {
                        selectedSubTab = index
                        onClearResult()
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }

        when (selectedSubTab) {
            0 -> AiChatView(
                chatMessages = chatMessages,
                isAiLoading = isAiLoading,
                onSendMessage = onSendMessage
            )
            1 -> AiListingGeneratorView(
                isAiLoading = isAiLoading,
                aiResult = aiOperationResult,
                optimizedResult = optimizedDescriptionResult,
                onGenerateListing = onGenerateListing,
                onGenerateOptimizedDescription = onGenerateOptimizedDescription
            )
            2 -> AiBuyboxStrategistView(
                products = products,
                isAiLoading = isAiLoading,
                aiResult = aiOperationResult,
                onAnalyze = onAnalyzeBuybox
            )
        }
    }
}

@Composable
fun AiChatView(
    chatMessages: List<AiChatMessage>,
    isAiLoading: Boolean,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val promptSuggestions = listOf(
        "أظهر المنتجات التي مخزونها أقل من 5",
        "كم ربحي اليوم؟",
        "اعمل وصف للمنتج سترة قطنية تركية",
        "ما هي الطلبات بانتظار الشحن والتجهيز؟",
        "تحليل منافسي الباي بوكس"
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Quick suggestion chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(promptSuggestions) { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(TrendyolOrangeLight)
                        .clickable { onSendMessage(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = suggestion, fontSize = 11.sp, color = TrendyolOrangeDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!isUser) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(TrendyolOrange),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Column(
                            modifier = Modifier.weight(1f, fill = false),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            // Function / Tool Call Execution Pill
                            if (msg.toolCallInfo != null) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5E9))
                                        .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = TrendyolSuccessGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "⚡ تم استدعاء الوظيفة: ${msg.toolCallInfo.title}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            }

                            // Message Bubble
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 14.dp
                                        )
                                    )
                                    .background(if (isUser) TrendyolOrange else MaterialTheme.colorScheme.surface)
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = msg.text,
                                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp
                                    )

                                    if (!isUser && msg.text.length > 50) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Trendyol AI Copilot", msg.text))
                                                    Toast.makeText(context, "تم نسخ الرد", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = Slate400,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Attached Products Cards from Tool execution
                            if (msg.attachedProducts.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "المنتجات المرتبطة بالاستعلام:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate500
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    msg.attachedProducts.forEach { prod ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(text = prod.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                                    Text(
                                                        text = "المخزون: ${prod.stockCount} | السعر: ${prod.salePrice} ₺ | باركود: ${prod.barcode}",
                                                        fontSize = 10.sp,
                                                        color = if (prod.stockCount <= 5) Color.Red else Slate500
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(TrendyolNavy),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (isAiLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = TrendyolOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "جاري تحليل الأمر واستدعاء دوال المتجر وقاعدة البيانات...", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                placeholder = { Text("اكتب أمراً مثل: كم ربحي اليوم؟ أو أظهر المنتجات...") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrendyolOrange,
                    unfocusedBorderColor = Slate200
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TrendyolOrange)
                    .testTag("send_ai_chat_btn")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = "إرسال", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AiListingGeneratorView(
    isAiLoading: Boolean,
    aiResult: String?,
    optimizedResult: OptimizedProductDescription? = null,
    onGenerateListing: (String, String, String, String) -> Unit,
    onGenerateOptimizedDescription: ((ProductMetadata) -> Unit)? = null
) {
    val context = LocalContext.current
    var productName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Giyim / Moda") }
    var subCategory by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var originalPriceText by remember { mutableStateOf("") }
    var materialText by remember { mutableStateOf("") }
    var targetAudienceText by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var selectedTone by remember { mutableStateOf("Professional") }
    var selectedLanguage by remember { mutableStateOf("Arabic") }

    val tones = listOf(
        "Professional" to "احترافي رسمي",
        "Marketing" to "تسويقي جذاب",
        "Luxury" to "فاخر وأنيق",
        "Trendy" to "عصري وشبابي"
    )

    val languages = listOf(
        "Arabic" to "العربية 🇸🇦",
        "Turkish" to "التركية 🇹🇷",
        "English" to "الإنجليزية 🇬🇧"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TrendyolOrangeLight)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TrendyolOrangeDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "مولد أوصاف المنتجات المحسن عبر Gemini API (Metadata Optimizer)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrendyolOrangeDark
                        )
                        Text(
                            text = "أدخل بيانات ومواصفات المنتج وسيقوم الذكاء الاصطناعي بتوليد عنوان سيو، نقاط بيع، ووصف متوافق مع خوارزميات ترنديول.",
                            fontSize = 10.sp,
                            color = Slate700
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                label = { Text("اسم المنتج أو العنوان الأولي (مثال: جاكيت رجالي ضد الماء)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("الماركة (Marka)") },
                    placeholder = { Text("مثال: TrendyolMilla") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("الفئة الرئيسية") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = { subCategory = it },
                    label = { Text("الفئة الفرعية (اختياري)") },
                    placeholder = { Text("مثال: ملابس شتوية") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = materialText,
                    onValueChange = { materialText = it },
                    label = { Text("الخامة والمادة") },
                    placeholder = { Text("مثال: 100% قطن عضوي") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر البيع (₺)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = originalPriceText,
                    onValueChange = { originalPriceText = it },
                    label = { Text("السعر قبل الخصم (₺)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        item {
            OutlinedTextField(
                value = targetAudienceText,
                onValueChange = { targetAudienceText = it },
                label = { Text("الجمهور المستهدف (اختياري)") },
                placeholder = { Text("مثال: رجال، رياضيون، مناسب للإهداء") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            OutlinedTextField(
                value = keywords,
                onValueChange = { keywords = it },
                label = { Text("أبرز المزايا والمواصفات والكلمات المفتاحية") },
                placeholder = { Text("مثال: بطانة صوف حرارية، سحاب مقاوم للماء، جيوب داخلية، غسيل يدوي") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(10.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "نبرة الوصف (Tone of Voice):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tones) { (key, label) ->
                        val isSelected = selectedTone == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) TrendyolOrange else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedTone = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "لغة المخرجات:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(languages) { (key, label) ->
                        val isSelected = selectedLanguage == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) TrendyolOrange else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedLanguage = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val attributes = mutableMapOf<String, String>()
                    if (materialText.isNotBlank()) attributes["الخامة / Material"] = materialText
                    if (targetAudienceText.isNotBlank()) attributes["الجمهور / Target"] = targetAudienceText
                    if (subCategory.isNotBlank()) attributes["التصنيف الفرعي"] = subCategory

                    val keyFeaturesList = keywords
                        .split(Regex("[,،\\n]+"))
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val metadata = ProductMetadata(
                        title = productName.trim(),
                        brand = brandName.trim(),
                        category = category.trim(),
                        subCategory = subCategory.trim().ifBlank { null },
                        price = priceText.toDoubleOrNull(),
                        originalPrice = originalPriceText.toDoubleOrNull(),
                        attributes = attributes,
                        keyFeatures = keyFeaturesList,
                        targetMarketplace = "Trendyol",
                        tone = selectedTone,
                        language = selectedLanguage
                    )

                    if (onGenerateOptimizedDescription != null) {
                        onGenerateOptimizedDescription(metadata)
                    } else {
                        onGenerateListing(productName, brandName, category, keywords)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
                enabled = productName.isNotBlank() && !isAiLoading
            ) {
                if (isAiLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "جارٍ المعالجة عبر Gemini API...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "✨ توليد وصف سيو متكامل بالذكاء الاصطناعي", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Structured Result Card if available
        if (optimizedResult != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TrendyolOrange)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "بطاقة المنتج المحسنة (Trendyol SEO)",
                                    fontWeight = FontWeight.Bold,
                                    color = TrendyolOrangeDark,
                                    fontSize = 14.sp
                                )
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Trendyol SEO Description", optimizedResult.rawFormattedOutput))
                                    Toast.makeText(context, "تم نسخ بطاقة المنتج كاملة للحافظة", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ الكل", tint = TrendyolOrange)
                            }
                        }

                        // SEO Title Box
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "العنوان المحسن (SEO Title):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrendyolOrangeDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = optimizedResult.seoTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }

                        // Bullet Points
                        if (optimizedResult.highlightBullets.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "أبرز مميزات المنتج (Öne Çıkan Özellikler):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrendyolOrangeDark)
                                    for (point in optimizedResult.highlightBullets) {
                                        Text(text = "• $point", fontSize = 12.sp, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }

                        // Detailed Description
                        if (optimizedResult.fullDescription.isNotBlank()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(text = "الوصف التسويقي المفصل (Ürün Açıklaması):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TrendyolOrangeDark)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = optimizedResult.fullDescription, fontSize = 12.sp, lineHeight = 17.sp)
                                }
                            }
                        }

                        // Search Tags
                        if (optimizedResult.searchTags.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "الكلمات المفتاحية والوسوم (Arama Etiketleri):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate700)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(optimizedResult.searchTags) { tag ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TrendyolOrangeLight)
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Tag", tag))
                                                    Toast.makeText(context, "تم نسخ: $tag", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = "#$tag", fontSize = 10.sp, color = TrendyolOrangeDark, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (aiResult != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "النتيجة المولدة من Gemini:", fontWeight = FontWeight.Bold, color = TrendyolOrangeDark)
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Trendyol Listing", aiResult))
                                    Toast.makeText(context, "تم نسخ النص كاملاً للحافظة", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "نسخ", tint = TrendyolOrange)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = aiResult, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@Composable
fun AiBuyboxStrategistView(
    products: List<ProductEntity>,
    isAiLoading: Boolean,
    aiResult: String?,
    onAnalyze: (ProductEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = TrendyolOrangeLight)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Paid, contentDescription = null, tint = TrendyolOrangeDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اختر أي منتج لتحليل استراتيجية الباي بوكس، تكلفة الشحن والعمولة وحساب السعر الرابح",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrendyolOrangeDark
                    )
                }
            }
        }

        items(products) { product ->
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
                        Text(text = product.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text(text = "سعرك: ${product.salePrice} ₺ | المنافس: ${product.competitorPrice} ₺", fontSize = 11.sp, color = Slate500)
                    }

                    Button(
                        onClick = { onAnalyze(product) },
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isAiLoading
                    ) {
                        Text(text = "تحليل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (aiResult != null) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = TrendyolOrange)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "توصية خوارزمية الباي بوكس:", fontWeight = FontWeight.Bold, color = TrendyolOrangeDark)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = aiResult, fontSize = 13.sp, lineHeight = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

