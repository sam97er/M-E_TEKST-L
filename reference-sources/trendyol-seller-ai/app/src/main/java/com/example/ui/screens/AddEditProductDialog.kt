package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.ProductEntity
import com.example.data.remote.OptimizedProductDescription
import com.example.data.remote.ProductMetadata
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.TrendyolOrangeDark
import com.example.ui.theme.TrendyolOrangeLight
import com.example.ui.theme.TrendyolSuccessGreen
import kotlinx.coroutines.launch

@Composable
fun AddEditProductDialog(
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onGenerateAiDescription: ((ProductMetadata, (OptimizedProductDescription) -> Unit) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialProduct?.title ?: "") }
    var barcode by remember { mutableStateOf(initialProduct?.barcode ?: "TY-${System.currentTimeMillis() % 100000}") }
    var brand by remember { mutableStateOf(initialProduct?.brand ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Giyim / Moda") }
    var salePriceText by remember { mutableStateOf(initialProduct?.salePrice?.toString() ?: "499.00") }
    var listPriceText by remember { mutableStateOf(initialProduct?.listPrice?.toString() ?: "699.00") }
    var stockText by remember { mutableStateOf(initialProduct?.stockCount?.toString() ?: "20") }
    var imageUrl by remember { mutableStateOf(initialProduct?.imageUrl ?: "https://images.unsplash.com/photo-1556905055-8f358a7a47b2?w=500&auto=format&fit=crop&q=60") }
    var description by remember { mutableStateOf(initialProduct?.description ?: "") }
    var keywordsText by remember { mutableStateOf("") }
    var materialText by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var aiGeneratedSuccess by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initialProduct == null) "إضافة منتج جديد لترنديول" else "تعديل بيانات المنتج",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان المنتج (Ürün Başlığı)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("الماركة (Marka)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("الباركود (Barkod)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("الفئة (Kategori)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = salePriceText,
                        onValueChange = { salePriceText = it },
                        label = { Text("سعر البيع (₺)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = listPriceText,
                        onValueChange = { listPriceText = it },
                        label = { Text("السعر قبل الخصم (₺)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { stockText = it },
                        label = { Text("الكمية بالمخزون (Stok)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = materialText,
                        onValueChange = { materialText = it },
                        label = { Text("الخامة / المادة") },
                        placeholder = { Text("قطن 100%، جلد طبيعي...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = keywordsText,
                    onValueChange = { keywordsText = it },
                    label = { Text("ميزات وكلمات مفتاحية إضافية") },
                    placeholder = { Text("مقاوم للمطر، قالب تركي مريح...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // AI Description Generator Trigger Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(TrendyolOrangeLight.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TrendyolOrangeDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "توليد وصف سيو بالذكاء الاصطناعي (Gemini)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrendyolOrangeDark
                                )
                            }

                            if (aiGeneratedSuccess) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = TrendyolSuccessGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "تم التوليد بنجاح",
                                        fontSize = 10.sp,
                                        color = TrendyolSuccessGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = "يقوم Gemini بتحليل بيانات المنتج وإنشاء عنوان سيو جذاب، قائمة مميزات، ووصف مقنع متوافق مع خوارزميات ترنديول.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )

                        Button(
                            onClick = {
                                isGeneratingAi = true
                                aiGeneratedSuccess = false
                                val metadata = ProductMetadata(
                                    title = title.ifBlank { "منتج عصري" },
                                    brand = brand.ifBlank { "Trendyol Collection" },
                                    category = category,
                                    price = salePriceText.toDoubleOrNull(),
                                    originalPrice = listPriceText.toDoubleOrNull(),
                                    attributes = if (materialText.isNotBlank()) mapOf("الخامة" to materialText) else emptyMap(),
                                    keyFeatures = if (keywordsText.isNotBlank()) keywordsText.split(Regex("[,،\\s]+")).filter { it.isNotBlank() } else emptyList(),
                                    targetMarketplace = "Trendyol"
                                )

                                onGenerateAiDescription?.invoke(metadata) { result ->
                                    isGeneratingAi = false
                                    aiGeneratedSuccess = true
                                    if (title.isBlank() || title == "منتج ترنديول جديد") {
                                        title = result.seoTitle
                                    }
                                    description = result.rawFormattedOutput
                                }
                            },
                            enabled = !isGeneratingAi,
                            colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "جارٍ إنشاء الوصف بالذكاء الاصطناعي...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "توليد الوصف والمواصفات الآن", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف والمواصفات المحسنة (Açıklama)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "إلغاء")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val salePrice = salePriceText.toDoubleOrNull() ?: 100.0
                            val listPrice = listPriceText.toDoubleOrNull() ?: (salePrice * 1.3)
                            val stock = stockText.toIntOrNull() ?: 10

                            val productToSave = initialProduct?.copy(
                                title = title.ifBlank { "منتج ترنديول جديد" },
                                barcode = barcode,
                                brand = brand.ifBlank { "Trendyol Partner" },
                                category = category,
                                salePrice = salePrice,
                                listPrice = listPrice,
                                stockCount = stock,
                                imageUrl = imageUrl,
                                description = description
                            ) ?: ProductEntity(
                                barcode = barcode,
                                title = title.ifBlank { "منتج ترنديول جديد" },
                                brand = brand.ifBlank { "Trendyol Partner" },
                                category = category,
                                salePrice = salePrice,
                                listPrice = listPrice,
                                stockCount = stock,
                                imageUrl = imageUrl,
                                buyboxWinner = true,
                                competitorPrice = salePrice + 15.0,
                                isActive = true,
                                description = description
                            )

                            onSave(productToSave)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "حفظ المنتج", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

