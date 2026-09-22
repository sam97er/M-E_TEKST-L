package com.example.ui.screens.questions

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomerQuestionsEntity
import com.example.data.repository.MAndETekstilRepository
import com.example.domain.model.ResponseStyle
import com.example.ui.components.StatusColorType
import com.example.ui.components.StatusPill
import com.example.ui.components.TrendyolAppTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.InfoSky
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TrendyolOrange
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuestionsScreen(
    repository: MAndETekstilRepository,
    snackbarHostState: SnackbarHostState
) {
    val questions by repository.allQuestions.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var activeQuestionForReply by remember { mutableStateOf<CustomerQuestionsEntity?>(null) }
    var selectedStyle by remember { mutableStateOf(ResponseStyle.FORMAL) }
    var editedReplyText by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    // Dispatch confirmation
    var questionToConfirmSend by remember { mutableStateOf<Pair<CustomerQuestionsEntity, String>?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TrendyolAppTopBar(
            title = "Müşteri İletişim AI",
            subtitle = "Slot 1: Müşteri İletişimi AI • ${questions.count { !it.isSent }} Bekleyen Soru"
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = TrendyolOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "M&E Tekstil Güvenli Yanıt İlkeleri",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "AI yanıtları asla onayınız olmadan Trendyol'a gönderilmez. Doğrulanamayan kumaş veya kalıp detaylarında manuel kontrol önerilir.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }

            items(questions, key = { it.questionId }) { question ->
                QuestionItemCard(
                    question = question,
                    onOpenReplyWorkflow = {
                        activeQuestionForReply = question
                        selectedStyle = question.selectedStyle
                        editedReplyText = question.aiSuggestedReply.ifBlank { question.finalReply }
                    }
                )
            }
        }
    }

    // Interactive Reply & Edit Modal
    if (activeQuestionForReply != null) {
        val q = activeQuestionForReply!!

        AlertDialog(
            onDismissRequest = { if (!isGeneratingAi) activeQuestionForReply = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("AI Yanıt Taslağı", fontWeight = FontWeight.Bold)
                    StatusPill(text = q.questionId, colorType = StatusColorType.INFO)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Müşteri: ${q.customerName} • ${q.productTitle}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "\"${q.questionText}\"",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Text("Yanıt Stili Seçin:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ResponseStyle.values()) { style ->
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = {
                                    selectedStyle = style
                                    // Trigger AI regeneration with new style
                                    scope.launch {
                                        isGeneratingAi = true
                                        val result = repository.generateCustomerReply(q, style)
                                        isGeneratingAi = false
                                        if (result.isSuccess) {
                                            editedReplyText = result.content
                                        }
                                    }
                                },
                                label = { Text(style.title, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TrendyolOrange,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (isGeneratingAi) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TrendyolOrange)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Slot 1 AI Yanıtı Üretiyor...", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = editedReplyText,
                        onValueChange = { editedReplyText = it },
                        modifier = Modifier.fillMaxWidth().height(140.dp).testTag("ai_reply_editor"),
                        label = { Text("Müşteriye İletilecek Yanıt (Düzenlenebilir)") }
                    )

                    // Uncertainty Check
                    if (q.questionText.contains("ölçü", ignoreCase = true) || q.questionText.contains("kilo", ignoreCase = true)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Warning, contentDescription = "Uyarı", tint = WarningAmber, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Beden & boy/kilo sorusu: Lütfen verilen tavsiyenin M&E kalıplarıyla uyumunu kontrol edin.",
                                fontSize = 10.sp,
                                color = WarningAmber
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMsg = editedReplyText.trim()
                        activeQuestionForReply = null
                        questionToConfirmSend = Pair(q, finalMsg)
                    },
                    enabled = editedReplyText.isNotBlank() && !isGeneratingAi,
                    colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange),
                    modifier = Modifier.testTag("submit_reply_button")
                ) {
                    Text("İncele & Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { activeQuestionForReply = null }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Final Approval Confirmation Dialog
    if (questionToConfirmSend != null) {
        val (q, finalTxt) = questionToConfirmSend!!
        AlertDialog(
            onDismissRequest = { questionToConfirmSend = null },
            title = { Text("Trendyol'a Yanıt Gönderim Onayı") },
            text = {
                Column {
                    Text("Aşağıdaki yanıt Trendyol Müşteri Paneline iletilecektir:\n")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(finalTxt, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gönderimi onaylıyor musunuz?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.approveAndSendReply(q.questionId, finalTxt)
                            questionToConfirmSend = null
                            snackbarHostState.showSnackbar("Yanıt Trendyol'a başarıyla iletildi.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("Evet, Trendyol'a Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { questionToConfirmSend = null }) {
                    Text("Geri")
                }
            }
        )
    }
}

@Composable
fun QuestionItemCard(
    question: CustomerQuestionsEntity,
    onOpenReplyWorkflow: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(question.questionDate))

    Card(
        modifier = Modifier.fillMaxWidth().testTag("question_item_${question.questionId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = question.customerName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = if (question.isSent) "Yanıtlandı" else "Yanıt Bekliyor",
                        colorType = if (question.isSent) StatusColorType.SUCCESS else StatusColorType.WARNING
                    )
                }
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = question.productTitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TrendyolOrange, fontWeight = FontWeight.SemiBold)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"${question.questionText}\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
            )

            if (question.aiSuggestedReply.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = TrendyolOrange.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = TrendyolOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Slot 1 AI Önerisi (${question.selectedStyle.title}):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = TrendyolOrange)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = question.aiSuggestedReply,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!question.isSent) {
                    Button(
                        onClick = onOpenReplyWorkflow,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrendyolOrange)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("İncele & Yanıtla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenReplyWorkflow,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Yanıt Detayı", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
