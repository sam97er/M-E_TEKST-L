package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomerSupportScreen() {
    val context = LocalContext.current
    var selectedQuestionType by remember { mutableStateOf(0) }
    var generatedReply by remember {
        mutableStateOf(
            "Merhaba, Mankenimizin üzerindeki ürün S bedendir. Ürünlerimiz yüksek kaliteli ve %100 pamuklu içerikli olduğundan terletme yapmaz, gün boyu rahatlıkla kullanabilirsiniz. İlginiz için teşekkür eder, M&E Tekstil olarak keyifli alışverişler dileriz. 🌸"
        )
    }

    val sampleQuestions = listOf(
        "Mankenin üzerindeki beden nedir ve kumaşı terletir mi?",
        "Ürün kalıbı dar mı, kendi bedenimi mi almalıyım?",
        "Seamless leggings esnek mi ve iç gösterir mi?",
        "Yıkama talimatı nedir, çeker mi?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Chat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "خدمة عملاء ترنديول (Trendyol Support)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "صياغة ردود احترافية باللغة التركية تركز على %100 pamuklu والراحة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                Text("اختر سؤالاً شائعاً من عميل ترنديول:", fontWeight = FontWeight.SemiBold)
            }
            items(sampleQuestions.size) { index ->
                OutlinedCard(
                    onClick = {
                        selectedQuestionType = index
                        generatedReply = when (index) {
                            0 -> "Merhaba, Mankenimizin üzerindeki ürün S bedendir. Ürünlerimiz yüksek kaliteli ve %100 pamuklu içerikli olduğundan terletme yapmaz, gün boyu rahatlıkla kullanabilirsiniz. İlginiz için teşekkür eder, M&E Tekstil olarak keyifli alışverişler dileriz. 🌸"
                            1 -> "Merhaba, ürünlerimiz standart Trendyol kalıplarına uygundur. Kendi bedeninizi tercih edebilirsiniz. %100 pamuklu yapısıyla vücudu sarar ve tam konfor sağlar. 🌸"
                            2 -> "Merhaba, Seamless koleksiyonumuz yüksek esneklik kabiliyetine sahip özel örme teknolojisiyle üretilmiştir. Kesinlikle iç göstermez ve gün boyu hareket özgürlüğü sunar. 🌸"
                            else -> "Merhaba, ürünümüzü 30 derecede hassas programda yıkaymanızı tavsiye ederiz. Kaliteli pamuklu yapısıyla uzun yıllar ilk günkü formunu korur. 🌸"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedQuestionType == index) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = sampleQuestions[index],
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("الرد الجاهز للنسخ إلى Trendyol Chat (باللغة التركية):", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = generatedReply,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Trendyol Reply", generatedReply)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الرد إلى الحافظة بنجاح 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نسخ الرد التركي")
                        }
                    }
                }
            }
        }
    }
}
